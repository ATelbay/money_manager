package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.auth.AuthManager
import com.atelbay.money_manager.core.model.SyncStatus
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.DefaultCategories
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.BudgetDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.DebtDao
import com.atelbay.money_manager.core.database.dao.DebtPaymentDao
import com.atelbay.money_manager.core.database.dao.RecurringTransactionDao
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
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringTransactionDao,
    private val debtDao: DebtDao,
    private val debtPaymentDao: DebtPaymentDao,
) {
    @Volatile
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val transactionMutexes = ConcurrentHashMap<Long, Mutex>()
    private val accountMutexes = ConcurrentHashMap<Long, Mutex>()
    private val categoryMutexes = ConcurrentHashMap<Long, Mutex>()
    private val budgetMutexes = ConcurrentHashMap<Long, Mutex>()
    private val recurringMutexes = ConcurrentHashMap<Long, Mutex>()
    private val debtMutexes = ConcurrentHashMap<Long, Mutex>()
    private val debtPaymentMutexes = ConcurrentHashMap<Long, Mutex>()

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
        val mutex = transactionMutexes.getOrPut(id) { Mutex() }
        mutex.withLock {
            try {
                val entity = transactionDao.getById(id) ?: return@withLock
                val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@withLock
                val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@withLock
                val finalEntity = if (entity.remoteId == null) {
                    entity.copy(remoteId = UUID.randomUUID().toString())
                } else entity
                // Push first, persist the new remoteId only on success — otherwise a failed push
                // would leave a remoteId locally with no cloud doc, and getPendingSync (remoteId
                // IS NULL) would never retry it.
                firestoreDataSource.pushTransaction(userId, finalEntity.toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
                if (entity.remoteId == null) transactionDao.update(finalEntity)
            } catch (e: Exception) {
                Timber.e(e, "syncTransaction($id) failed")
            }
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

    fun syncBudget(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        val mutex = budgetMutexes.getOrPut(id) { Mutex() }
        mutex.withLock {
            try {
                val entity = budgetDao.getById(id)
                    ?: budgetDao.getDeletedWithRemoteId().find { it.id == id }
                    ?: return@withLock
                val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@withLock
                val finalEntity = if (entity.remoteId == null) {
                    entity.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                } else entity
                firestoreDataSource.pushBudget(userId, finalEntity.toDto(fieldCipherHolder, categoryRemoteId))
                if (entity.remoteId == null) budgetDao.update(finalEntity)
            } catch (e: Exception) {
                Timber.e(e, "syncBudget($id) failed")
            }
        }
    }

    fun syncRecurring(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        val mutex = recurringMutexes.getOrPut(id) { Mutex() }
        mutex.withLock {
            try {
                val entity = recurringDao.getById(id)
                    ?: recurringDao.getDeletedWithRemoteId().find { it.id == id }
                    ?: return@withLock
                val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@withLock
                val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@withLock
                val finalEntity = if (entity.remoteId == null) {
                    entity.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                } else entity
                firestoreDataSource.pushRecurringTransaction(userId, finalEntity.toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
                if (entity.remoteId == null) recurringDao.update(finalEntity)
            } catch (e: Exception) {
                Timber.e(e, "syncRecurring($id) failed")
            }
        }
    }

    fun syncDebt(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        val mutex = debtMutexes.getOrPut(id) { Mutex() }
        mutex.withLock {
            try {
                val entity = debtDao.getById(id)
                    ?: debtDao.getDeletedWithRemoteId().find { it.id == id }
                    ?: return@withLock
                val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@withLock
                val finalEntity = if (entity.remoteId == null) {
                    entity.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                } else entity
                firestoreDataSource.pushDebt(userId, finalEntity.toDto(fieldCipherHolder, accountRemoteId))
                if (entity.remoteId == null) debtDao.update(finalEntity)
            } catch (e: Exception) {
                Timber.e(e, "syncDebt($id) failed")
            }
        }
    }

    fun syncDebtPayment(id: Long) = scope.launch {
        val userId = authManager.currentUser.value?.userId ?: return@launch
        val mutex = debtPaymentMutexes.getOrPut(id) { Mutex() }
        mutex.withLock {
            try {
                val entity = debtPaymentDao.getById(id)
                    ?: debtPaymentDao.getDeletedWithRemoteId().find { it.id == id }
                    ?: return@withLock
                val debtEntity = debtDao.getById(entity.debtId)
                    ?: debtDao.getDeletedWithRemoteId().find { it.id == entity.debtId }
                val debtRemoteId = debtEntity?.remoteId ?: return@withLock
                val txId = entity.transactionId
                val transactionRemoteId = if (txId != null) {
                    transactionDao.getById(txId)?.remoteId.orEmpty()
                } else ""
                val finalEntity = if (entity.remoteId == null) {
                    entity.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                } else entity
                firestoreDataSource.pushDebtPayment(userId, finalEntity.toDto(fieldCipherHolder, debtRemoteId, transactionRemoteId))
                if (entity.remoteId == null) debtPaymentDao.update(finalEntity)
            } catch (e: Exception) {
                Timber.e(e, "syncDebtPayment($id) failed")
            }
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
        // Push soft-deleted accounts so Firestore receives the isDeleted=true tombstone
        accountDao.getDeletedWithRemoteId().forEach { entity ->
            firestoreDataSource.pushAccount(userId, entity.toDto(fieldCipherHolder))
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
            // Push first, persist remoteId only on success (see syncTransaction for the rationale).
            val updated = entity.copy(remoteId = UUID.randomUUID().toString())
            firestoreDataSource.pushCategory(userId, updated.toDto(fieldCipherHolder))
            categoryDao.update(updated)
        }
        transactionDao.getPendingSync().forEach { entity ->
            val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@forEach
            val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@forEach
            val updated = entity.copy(remoteId = UUID.randomUUID().toString())
            firestoreDataSource.pushTransaction(userId, updated.toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
            transactionDao.update(updated)
        }
        budgetDao.getPendingSync().forEach { entity ->
            budgetMutexes.getOrPut(entity.id) { Mutex() }.withLock {
                val current = budgetDao.getById(entity.id) ?: return@withLock
                if (current.remoteId != null) return@withLock // already synced
                val categoryRemoteId = ensureCategoryRemoteId(current.categoryId) ?: return@withLock
                val updated = current.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                firestoreDataSource.pushBudget(userId, updated.toDto(fieldCipherHolder, categoryRemoteId))
                budgetDao.update(updated)
            }
        }
        budgetDao.getDeletedWithRemoteId().forEach { entity ->
            val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@forEach
            firestoreDataSource.pushBudget(userId, entity.toDto(fieldCipherHolder, categoryRemoteId))
        }
        recurringDao.getPendingSync().forEach { entity ->
            recurringMutexes.getOrPut(entity.id) { Mutex() }.withLock {
                val current = recurringDao.getById(entity.id) ?: return@withLock
                if (current.remoteId != null) return@withLock // already synced
                val categoryRemoteId = ensureCategoryRemoteId(current.categoryId) ?: return@withLock
                val accountRemoteId = ensureAccountRemoteId(current.accountId) ?: return@withLock
                val updated = current.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                firestoreDataSource.pushRecurringTransaction(userId, updated.toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
                recurringDao.update(updated)
            }
        }
        recurringDao.getDeletedWithRemoteId().forEach { entity ->
            val categoryRemoteId = ensureCategoryRemoteId(entity.categoryId) ?: return@forEach
            val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@forEach
            firestoreDataSource.pushRecurringTransaction(userId, entity.toDto(categoryRemoteId, accountRemoteId, fieldCipherHolder))
        }
        debtDao.getPendingSync().forEach { entity ->
            debtMutexes.getOrPut(entity.id) { Mutex() }.withLock {
                val current = debtDao.getById(entity.id) ?: return@withLock
                if (current.remoteId != null) return@withLock
                val accountRemoteId = ensureAccountRemoteId(current.accountId) ?: return@withLock
                val updated = current.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                firestoreDataSource.pushDebt(userId, updated.toDto(fieldCipherHolder, accountRemoteId))
                debtDao.update(updated)
            }
        }
        debtDao.getDeletedWithRemoteId().forEach { entity ->
            val accountRemoteId = ensureAccountRemoteId(entity.accountId) ?: return@forEach
            firestoreDataSource.pushDebt(userId, entity.toDto(fieldCipherHolder, accountRemoteId))
        }
        debtPaymentDao.getPendingSync().forEach { entity ->
            debtPaymentMutexes.getOrPut(entity.id) { Mutex() }.withLock {
                val current = debtPaymentDao.getById(entity.id) ?: return@withLock
                if (current.remoteId != null) return@withLock
                val debtEntity = debtDao.getById(current.debtId) ?: return@withLock
                val debtRemoteId = debtEntity.remoteId ?: return@withLock
                val curTxId = current.transactionId
                val transactionRemoteId = if (curTxId != null) {
                    transactionDao.getById(curTxId)?.remoteId.orEmpty()
                } else ""
                val updated = current.copy(remoteId = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                firestoreDataSource.pushDebtPayment(userId, updated.toDto(fieldCipherHolder, debtRemoteId, transactionRemoteId))
                debtPaymentDao.update(updated)
            }
        }
        debtPaymentDao.getDeletedWithRemoteId().forEach { entity ->
            val debtEntity = debtDao.getById(entity.debtId) ?: debtDao.getDeletedWithRemoteId().find { it.id == entity.debtId }
            val debtRemoteId = debtEntity?.remoteId ?: return@forEach
            val delTxId = entity.transactionId
            val transactionRemoteId = if (delTxId != null) {
                transactionDao.getById(delTxId)?.remoteId.orEmpty()
            } else ""
            firestoreDataSource.pushDebtPayment(userId, entity.toDto(fieldCipherHolder, debtRemoteId, transactionRemoteId))
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
     * Cancels any in-flight per-entity sync jobs and arms a fresh scope. Used on plain sign-out:
     * local data and remoteIds are deliberately KEPT so that the same user signing back in keeps
     * their existing remote identities (no duplicate Firestore docs). Cross-user isolation is
     * handled separately by [clearLocalUserData], invoked only when a *different* uid signs in.
     */
    suspend fun cancelInFlight() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * Wipes all local financial data and re-seeds the default categories. Invoked by
     * [LoginSyncOrchestrator] when a different user signs in on this device, so the previous
     * user's data can never be pushed into the new user's Firestore path (and is not visible
     * to them locally). Deletes children before parents to satisfy foreign keys.
     */
    suspend fun clearLocalUserData() {
        cancelInFlight()
        debtPaymentDao.deleteAll()
        debtDao.deleteAll()
        transactionDao.deleteAll()
        budgetDao.deleteAll()
        recurringDao.deleteAll()
        accountDao.deleteAll()
        categoryDao.deleteAll()
        categoryDao.insertAll(DefaultCategories.all())
        Timber.d("SyncManager: local user data wiped + default categories reseeded on user switch")
    }

    companion object {
        fun defaultCategoryRemoteId(name: String, type: String) = "default:$name:$type"
    }
}
