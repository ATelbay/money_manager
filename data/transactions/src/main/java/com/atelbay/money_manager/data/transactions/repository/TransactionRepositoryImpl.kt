package com.atelbay.money_manager.data.transactions.repository

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.data.transactions.mapper.toDomain
import com.atelbay.money_manager.data.transactions.mapper.toEntity
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        combine(
            transactionDao.observeAll(),
            categoryDao.observeAll().distinctUntilChanged(),
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override fun observeByCategoryTypeAndDateRange(
        categoryId: Long,
        transactionType: TransactionType,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<Transaction>> =
        combine(
            transactionDao.observeByCategoryTypeAndDateRange(
                categoryId = categoryId,
                type = transactionType.value,
                startDate = startMillis,
                endDate = endMillis,
            ),
            categoryDao.observeAll().distinctUntilChanged(),
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override fun observeById(id: Long): Flow<Transaction?> =
        combine(
            transactionDao.observeById(id),
            categoryDao.observeAll().distinctUntilChanged(),
        ) { transaction, categories ->
            transaction?.let { entity ->
                val categoryMap = categories.associateBy { it.id }
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override suspend fun save(transaction: Transaction): Long {
        val now = System.currentTimeMillis()
        val baseEntity = transaction.toEntity()

        return if (baseEntity.id == 0L) {
            val newEntity = baseEntity.copy(createdAt = now, updatedAt = now)
            val id = database.withTransaction {
                val newId = transactionDao.insert(newEntity)
                val delta = if (newEntity.type == "income") newEntity.amount else -newEntity.amount
                accountDao.updateBalance(newEntity.accountId, delta, now)
                newId
            }
            syncManager.syncTransaction(id)
            syncManager.syncAccount(newEntity.accountId)
            id
        } else {
            val oldAccountId = transactionDao.getById(baseEntity.id)?.accountId
            database.withTransaction {
                val existing = transactionDao.getById(baseEntity.id)
                val updatedEntity = baseEntity.copy(
                    createdAt = existing?.createdAt ?: now,
                    remoteId = existing?.remoteId,
                    isDeleted = existing?.isDeleted ?: false,
                    updatedAt = now,
                )
                transactionDao.update(updatedEntity)
                // Revert old transaction's effect on balance
                if (existing != null) {
                    val oldDelta = if (existing.type == "income") -existing.amount else existing.amount
                    accountDao.updateBalance(existing.accountId, oldDelta, now)
                }
                // Apply new transaction's effect on balance
                val delta = if (updatedEntity.type == "income") updatedEntity.amount else -updatedEntity.amount
                accountDao.updateBalance(updatedEntity.accountId, delta, now)
            }
            syncManager.syncTransaction(baseEntity.id)
            // Sync both accounts in case account changed
            if (oldAccountId != null && oldAccountId != baseEntity.accountId) {
                syncManager.syncAccount(oldAccountId)
            }
            syncManager.syncAccount(baseEntity.accountId)
            baseEntity.id
        }
    }

    override suspend fun delete(id: Long) {
        val entity = transactionDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        database.withTransaction {
            // Revert balance
            val delta = if (entity.type == "income") -entity.amount else entity.amount
            accountDao.updateBalance(entity.accountId, delta, now)
            transactionDao.softDeleteById(id, now)
        }
        syncManager.syncTransaction(id)
        syncManager.syncAccount(entity.accountId)
    }

    override suspend fun getTopCurrenciesByUsage(): List<String> =
        transactionDao.getCurrencyTransactionCounts().map { it.currency }
}
