package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.mapper.toEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls all remote data for [userId] and merges into local Room DB.
 * Uses last-write-wins by comparing [updatedAt] timestamps.
 * Writes directly via DAOs to bypass repository balance-accounting logic.
 */
@Singleton
class PullSyncUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {
    suspend operator fun invoke(userId: String) {
        Timber.d("PullSync: starting for userId=$userId")
        pullAccounts(userId)
        pullCategories(userId)
        pullTransactions(userId)
        Timber.d("PullSync: done")
    }

    private suspend fun pullAccounts(userId: String) {
        val remoteAccounts = firestoreDataSource.pullAccounts(userId)
        for (dto in remoteAccounts) {
            val local = accountDao.getByRemoteId(dto.remoteId)
            if (dto.isDeleted) {
                if (local != null) accountDao.delete(local)
                continue
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue
            val entity = dto.toEntity(localId = local?.id ?: 0)
            if (local == null) {
                accountDao.insert(entity)
            } else {
                accountDao.upsertSync(listOf(entity.copy(id = local.id)))
            }
        }
    }

    private suspend fun pullCategories(userId: String) {
        val remoteCategories = firestoreDataSource.pullCategories(userId)
        for (dto in remoteCategories) {
            val local = categoryDao.getByRemoteId(dto.remoteId)
            if (dto.isDeleted) {
                if (local != null) categoryDao.delete(local)
                continue
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue
            val entity = dto.toEntity(localId = local?.id ?: 0)
            if (local == null) {
                categoryDao.insert(entity)
            } else {
                categoryDao.upsertSync(listOf(entity.copy(id = local.id)))
            }
        }
    }

    private suspend fun resolveCategory(remoteId: String): Long? {
        if (remoteId.startsWith("default:")) {
            val parts = remoteId.split(":")
            if (parts.size < 3) return null
            val name = parts[1]
            val type = parts[2]
            return categoryDao.getByType(type).find { it.isDefault && it.name == name }?.id
        }
        return categoryDao.getByRemoteId(remoteId)?.id
    }

    private suspend fun pullTransactions(userId: String) {
        val remoteTransactions = firestoreDataSource.pullTransactions(userId)
        for (dto in remoteTransactions) {
            val local = transactionDao.getByRemoteId(dto.remoteId)
            if (dto.isDeleted) {
                if (local != null) transactionDao.deleteById(local.id)
                continue
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue

            val categoryLocalId = resolveCategory(dto.categoryRemoteId)
            val accountLocalId = accountDao.getByRemoteId(dto.accountRemoteId)?.id
            if (categoryLocalId == null || accountLocalId == null) {
                Timber.w("PullSync: skipping transaction ${dto.remoteId} — missing account or category")
                continue
            }

            val entity = dto.toEntity(
                localId = local?.id ?: 0,
                categoryLocalId = categoryLocalId,
                accountLocalId = accountLocalId,
            )
            transactionDao.upsertSync(listOf(entity))
        }
    }
}
