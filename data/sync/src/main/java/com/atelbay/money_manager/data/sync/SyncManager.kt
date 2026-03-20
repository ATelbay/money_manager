package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.auth.AuthManager
import com.atelbay.money_manager.core.model.SyncStatus
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.mapper.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val fieldCipherHolder: FieldCipherHolder,
    private val firestoreDataSource: FirestoreDataSource,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {
    @Volatile
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val accountMutexes = ConcurrentHashMap<Long, Mutex>()
    private val categoryMutexes = ConcurrentHashMap<Long, Mutex>()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** Timestamp of the last successful bulk sync, preserved across Failed transitions. */
    @Volatile
    private var _lastSuccessfulSyncAt: Long? = null
    val lastSuccessfulSyncAt: Long? get() = _lastSuccessfulSyncAt

    /** Only [LoginSyncOrchestrator] should drive bulk-sync status transitions. */
    internal fun updateStatus(status: SyncStatus) {
        if (status is SyncStatus.Synced) _lastSuccessfulSyncAt = status.lastSyncedAt
        _syncStatus.value = status
    }

    fun syncTransaction(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        try {
            val entity = transactionDao.getById(id) ?: return@launch
            val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@launch
            val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@launch
            val finalEntity = if (entity.remoteId == null) {
                val remoteId = UUID.randomUUID().toString()
                transactionDao.update(entity.copy(remoteId = remoteId))
                entity.copy(remoteId = remoteId)
            } else entity
            firestoreDataSource.pushTransaction(userId, finalEntity.toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
        } catch (e: Exception) {
            Timber.e(e, "syncTransaction($id) failed")
        }
    }

    fun syncAccount(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        try {
            val remoteId = ensureAccountRemoteId(id) ?: return@launch
            val entity = accountDao.getById(id) ?: return@launch
            firestoreDataSource.pushAccount(userId, entity.copy(remoteId = remoteId).toDto(fieldCipherHolder))
        } catch (e: Exception) {
            Timber.e(e, "syncAccount($id) failed")
        }
    }

    fun syncCategory(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        try {
            val entity = categoryDao.getById(id) ?: return@launch
            if (entity.isDefault) return@launch
            val remoteId = ensureCategoryRemoteId(id) ?: return@launch
            firestoreDataSource.pushCategory(userId, entity.copy(remoteId = remoteId).toDto(fieldCipherHolder))
        } catch (e: Exception) {
            Timber.e(e, "syncCategory($id) failed")
        }
    }

    /**
     * Pushes all accounts that already have a remoteId (i.e. ensures latest balance is remote).
     * Accounts without a remoteId are handled by [pushAllPending].
     */
    suspend fun pushAllAccounts() {
        val userId = authManager.currentUser.value?.userId ?: return
        accountDao.getAllForSync()
            .filter { it.remoteId != null }
            .forEach { entity ->
                val updated = accountDao.getById(entity.id) ?: return@forEach
                firestoreDataSource.pushAccount(userId, updated.toDto(fieldCipherHolder))
            }
    }

    suspend fun pushAllPending() {
        val userId = authManager.currentUser.value?.userId ?: return
        Timber.d("pushAllPending: starting for userId=$userId")
        accountDao.getPendingSync().forEach { entity ->
            val remoteId = ensureAccountRemoteId(entity.id) ?: return@forEach
            val updated = accountDao.getById(entity.id) ?: return@forEach
            firestoreDataSource.pushAccount(userId, updated.copy(remoteId = remoteId).toDto(fieldCipherHolder))
        }
        categoryDao.getPendingSync().forEach { entity ->
            val remoteId = UUID.randomUUID().toString()
            categoryDao.update(entity.copy(remoteId = remoteId))
            firestoreDataSource.pushCategory(userId, entity.copy(remoteId = remoteId).toDto(fieldCipherHolder))
        }
        transactionDao.getPendingSync().forEach { entity ->
            val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@forEach
            val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@forEach
            val remoteId = UUID.randomUUID().toString()
            transactionDao.update(entity.copy(remoteId = remoteId, updatedAt = System.currentTimeMillis()))
            firestoreDataSource.pushTransaction(userId, entity.copy(remoteId = remoteId).toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
        }
        Timber.d("pushAllPending: done")
    }

    private suspend fun ensureAccountRemoteId(id: Long): String? {
        val mutex = accountMutexes.getOrPut(id) { Mutex() }
        return mutex.withLock {
            val entity = accountDao.getById(id) ?: return@withLock null
            if (entity.remoteId != null) return@withLock entity.remoteId
            val remoteId = UUID.randomUUID().toString()
            accountDao.update(entity.copy(remoteId = remoteId))
            remoteId
        }
    }

    private suspend fun ensureCategoryRemoteId(id: Long): String? {
        val mutex = categoryMutexes.getOrPut(id) { Mutex() }
        return mutex.withLock {
            val entity = categoryDao.getById(id) ?: return@withLock null
            if (entity.isDefault) return@withLock defaultCategoryRemoteId(entity.name, entity.type)
            if (entity.remoteId != null) return@withLock entity.remoteId
            val remoteId = UUID.randomUUID().toString()
            categoryDao.update(entity.copy(remoteId = remoteId))
            remoteId
        }
    }

    /**
     * Clears all remote IDs from local entities on sign-out so that the next user's
     * sync session cannot accidentally push the previous user's data to their Firestore path.
     */
    suspend fun clearSyncMetadata() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        accountDao.clearRemoteIds()
        categoryDao.clearRemoteIds()
        transactionDao.clearRemoteIds()
        Timber.d("SyncManager: sync metadata cleared on sign-out")
    }

    companion object {
        fun defaultCategoryRemoteId(name: String, type: String) = "default:$name:$type"
    }
}
