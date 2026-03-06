package com.atelbay.money_manager.data.transactions.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.data.transactions.mapper.toDomain
import com.atelbay.money_manager.data.transactions.mapper.toEntity
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        combine(
            transactionDao.observeAll(),
            categoryDao.observeAll(),
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override fun observeById(id: Long): Flow<Transaction?> =
        combine(
            transactionDao.observeById(id),
            categoryDao.observeAll(),
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
            val id = transactionDao.insert(newEntity)
            val delta = if (newEntity.type == "income") newEntity.amount else -newEntity.amount
            accountDao.updateBalance(newEntity.accountId, delta, now)
            syncManager.syncTransaction(id)
            id
        } else {
            val existing = transactionDao.getById(baseEntity.id)
            val updatedEntity = baseEntity.copy(
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
            syncManager.syncTransaction(baseEntity.id)
            baseEntity.id
        }
    }

    override suspend fun delete(id: Long) {
        val entity = transactionDao.getById(id) ?: return
        // Revert balance
        val delta = if (entity.type == "income") -entity.amount else entity.amount
        val now = System.currentTimeMillis()
        accountDao.updateBalance(entity.accountId, delta, now)
        transactionDao.softDeleteById(id, now)
        syncManager.syncTransaction(id)
    }

    override suspend fun getTopCurrenciesByUsage(): List<String> =
        transactionDao.getCurrencyTransactionCounts().map { it.currency }
}
