package com.atelbay.money_manager.data.recurring.repository

import androidx.room.withTransaction
import com.atelbay.money_manager.core.database.MoneyManagerDatabase
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.RecurringTransactionDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.TransactionEntity
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.data.recurring.mapper.toDomain
import com.atelbay.money_manager.data.recurring.mapper.toEntity
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionRepositoryImpl @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
) : RecurringTransactionRepository {

    override fun observeAll(): Flow<List<RecurringTransaction>> =
        combine(
            recurringDao.observeAll(),
            categoryDao.observeAll().distinctUntilChanged(),
            accountDao.observeAll().distinctUntilChanged(),
        ) { recurrings, categories, accounts ->
            val categoryMap = categories.associateBy { it.id }
            val accountMap = accounts.associateBy { it.id }
            recurrings.map { entity ->
                val category = categoryMap[entity.categoryId]
                val account = accountMap[entity.accountId]
                entity.toDomain(
                    categoryName = category?.name.orEmpty(),
                    categoryIcon = category?.icon.orEmpty(),
                    categoryColor = category?.color ?: 0xFF90A4AEL,
                    accountName = account?.name.orEmpty(),
                    accountCurrency = account?.currency ?: "KZT",
                )
            }
        }

    override suspend fun getById(id: Long): RecurringTransaction? {
        val entity = recurringDao.getById(id) ?: return null
        val category = categoryDao.getById(entity.categoryId)
        val account = accountDao.getById(entity.accountId)
        return entity.toDomain(
            categoryName = category?.name.orEmpty(),
            categoryIcon = category?.icon.orEmpty(),
            categoryColor = category?.color ?: 0xFF90A4AEL,
            accountName = account?.name.orEmpty(),
            accountCurrency = account?.currency ?: "KZT",
        )
    }

    override suspend fun getActiveRecurrings(): List<RecurringTransaction> {
        val entities = recurringDao.getActiveRecurrings()
        if (entities.isEmpty()) return emptyList()

        val categoryMap = categoryDao.getAll().associateBy { it.id }
        val accountMap = accountDao.getAllForSync().associateBy { it.id }

        return entities.map { entity ->
            val category = categoryMap[entity.categoryId]
            val account = accountMap[entity.accountId]
            entity.toDomain(
                categoryName = category?.name.orEmpty(),
                categoryIcon = category?.icon.orEmpty(),
                categoryColor = category?.color ?: 0xFF90A4AEL,
                accountName = account?.name.orEmpty(),
                accountCurrency = account?.currency ?: "KZT",
            )
        }
    }

    override suspend fun save(recurring: RecurringTransaction): Long {
        val now = System.currentTimeMillis()
        val baseEntity = recurring.toEntity()
        val savedId = if (baseEntity.id == 0L) {
            recurringDao.insert(baseEntity.copy(createdAt = now, updatedAt = now))
        } else {
            val existing = recurringDao.getById(baseEntity.id)
            recurringDao.update(
                baseEntity.copy(
                    createdAt = existing?.createdAt ?: now,
                    remoteId = existing?.remoteId,
                    isDeleted = existing?.isDeleted ?: false,
                    updatedAt = now,
                )
            )
            baseEntity.id
        }
        syncManager.syncRecurring(savedId)
        return savedId
    }

    override suspend fun delete(id: Long) {
        recurringDao.softDelete(id, System.currentTimeMillis())
        syncManager.syncRecurring(id)
    }

    override suspend fun toggleActive(id: Long, isActive: Boolean) {
        val entity = recurringDao.getById(id) ?: return
        recurringDao.update(entity.copy(isActive = isActive, updatedAt = System.currentTimeMillis()))
        syncManager.syncRecurring(id)
    }

    override suspend fun updateLastGeneratedDate(id: Long, date: Long) {
        recurringDao.updateLastGeneratedDate(id, date, System.currentTimeMillis())
    }

    override suspend fun generateTransactionsAtomically(
        recurringId: Long,
        transactions: List<Transaction>,
        lastGeneratedDate: Long,
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            for (transaction in transactions) {
                val entity = TransactionEntity(
                    id = 0,
                    amount = transaction.amount,
                    type = transaction.type.value,
                    categoryId = transaction.categoryId,
                    accountId = transaction.accountId,
                    note = transaction.note,
                    date = transaction.date,
                    createdAt = now,
                    updatedAt = now,
                )
                transactionDao.insert(entity)
                val delta = if (transaction.type.value == "income") transaction.amount else -transaction.amount
                accountDao.updateBalance(transaction.accountId, delta, now)
            }
            recurringDao.updateLastGeneratedDate(recurringId, lastGeneratedDate, now)
        }
    }
}
