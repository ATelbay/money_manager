package com.atelbay.money_manager.feature.transactions.data.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.feature.transactions.data.mapper.toDomain
import com.atelbay.money_manager.feature.transactions.data.mapper.toEntity
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
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

    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        categoryDao.observeByType(type.value).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun save(transaction: Transaction): Long {
        val entity = transaction.toEntity().let {
            if (it.id == 0L) it.copy(createdAt = System.currentTimeMillis()) else it
        }

        val isNew = entity.id == 0L
        val id = if (isNew) {
            transactionDao.insert(entity)
        } else {
            val old = transactionDao.getById(entity.id)
            transactionDao.update(entity)
            // Revert old transaction's effect on balance
            if (old != null) {
                val oldDelta = if (old.type == "income") -old.amount else old.amount
                accountDao.updateBalance(old.accountId, oldDelta)
            }
            entity.id
        }

        // Apply new transaction's effect on balance
        val delta = if (entity.type == "income") entity.amount else -entity.amount
        accountDao.updateBalance(entity.accountId, delta)

        return id
    }

    override suspend fun delete(id: Long) {
        val entity = transactionDao.getById(id) ?: return
        // Revert balance
        val delta = if (entity.type == "income") -entity.amount else entity.amount
        accountDao.updateBalance(entity.accountId, delta)
        transactionDao.deleteById(id)
    }
}
