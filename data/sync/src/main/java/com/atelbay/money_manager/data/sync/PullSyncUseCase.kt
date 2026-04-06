package com.atelbay.money_manager.data.sync

import com.atelbay.money_manager.core.crypto.FieldCipher.Companion.CURRENT_ENCRYPTION_VERSION
import com.atelbay.money_manager.core.crypto.FieldCipherHolder
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.BudgetDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.RecurringTransactionDao
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
    private val fieldCipherHolder: FieldCipherHolder,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringTransactionDao,
) {
    suspend operator fun invoke(userId: String) {
        Timber.d("PullSync: starting for userId=$userId")
        pullAccounts(userId)
        pullCategories(userId)
        pullBudgets(userId)
        pullRecurringTransactions(userId)
        pullTransactions(userId)
        Timber.d("PullSync: done")
    }

    private suspend fun pullAccounts(userId: String) {
        val remoteAccounts = firestoreDataSource.pullAccounts(userId)
        for (dto in remoteAccounts) {
            var local = accountDao.getByRemoteId(dto.remoteId)
            if (dto.isDeleted) {
                if (local != null) accountDao.softDeleteById(local.id, dto.updatedAt)
                continue
            }
            if (local == null) {
                val cipher = fieldCipherHolder.cipher
                val decryptedName = if (dto.encryptionVersion == CURRENT_ENCRYPTION_VERSION && cipher != null) {
                    try { cipher.decrypt(dto.name) } catch (e: Exception) { null }
                } else {
                    dto.name
                }
                if (decryptedName != null) {
                    val fallback = accountDao.getByNameAndCurrency(decryptedName, dto.currency)
                    if (fallback != null) {
                        Timber.d("PullSync: fallback match for account '${decryptedName}' — linking remoteId ${dto.remoteId}")
                        accountDao.update(fallback.copy(remoteId = dto.remoteId))
                        local = accountDao.getByRemoteId(dto.remoteId)
                    }
                }
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue
            val entity = dto.toEntity(localId = local?.id ?: 0, fieldCipherHolder = fieldCipherHolder)
                ?: continue
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
                if (local != null) categoryDao.softDeleteById(local.id, dto.updatedAt)
                continue
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue
            val entity = dto.toEntity(localId = local?.id ?: 0, fieldCipherHolder = fieldCipherHolder)
                ?: continue
            if (local == null) {
                categoryDao.insert(entity)
            } else {
                categoryDao.upsertSync(listOf(entity.copy(id = local.id)))
            }
        }
    }

    private suspend fun pullBudgets(userId: String) {
        val remoteBudgets = firestoreDataSource.pullBudgets(userId)
        for (dto in remoteBudgets) {
            val local = budgetDao.getByRemoteId(dto.remoteId)
            if (dto.isDeleted) {
                if (local != null) budgetDao.softDeleteById(local.id, dto.updatedAt)
                continue
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue

            val categoryLocalId = resolveCategory(dto.categoryRemoteId)
            if (categoryLocalId == null) {
                Timber.w("PullSync: skipping budget ${dto.remoteId} — missing category ${dto.categoryRemoteId}")
                continue
            }

            val entity = dto.toEntity(
                localId = local?.id ?: 0,
                fieldCipherHolder = fieldCipherHolder,
                localCategoryId = categoryLocalId,
            ) ?: continue
            if (local == null) {
                budgetDao.insert(entity)
            } else {
                budgetDao.upsertSync(listOf(entity.copy(id = local.id)))
            }
        }
    }

    private suspend fun pullRecurringTransactions(userId: String) {
        val remoteRecurrings = firestoreDataSource.pullRecurringTransactions(userId)
        for (dto in remoteRecurrings) {
            val local = recurringDao.getByRemoteId(dto.remoteId)
            if (dto.isDeleted) {
                if (local != null) recurringDao.softDeleteById(local.id, dto.updatedAt)
                continue
            }
            if (local != null && local.updatedAt >= dto.updatedAt) continue

            val categoryLocalId = resolveCategory(dto.categoryRemoteId)
            val accountLocalId = accountDao.getByRemoteId(dto.accountRemoteId)?.id
            if (categoryLocalId == null || accountLocalId == null) {
                Timber.w("PullSync: skipping recurring ${dto.remoteId} — missing account or category")
                continue
            }

            val entity = dto.toEntity(
                localId = local?.id ?: 0,
                categoryLocalId = categoryLocalId,
                accountLocalId = accountLocalId,
                fieldCipherHolder = fieldCipherHolder,
            ) ?: continue
            if (local == null) {
                recurringDao.insert(entity)
            } else {
                recurringDao.upsertSync(listOf(entity.copy(id = local.id)))
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
                if (local != null) {
                    val delta = if (local.type == "income") -local.amount else local.amount
                    accountDao.updateBalance(local.accountId, delta, dto.updatedAt)
                    transactionDao.softDeleteById(local.id, dto.updatedAt)
                }
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
                fieldCipherHolder = fieldCipherHolder,
            ) ?: continue
            transactionDao.upsertSync(listOf(entity))
        }
    }
}
