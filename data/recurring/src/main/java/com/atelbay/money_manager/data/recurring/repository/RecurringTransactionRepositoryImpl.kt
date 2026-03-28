package com.atelbay.money_manager.data.recurring.repository

import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.RecurringTransactionDao
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.data.recurring.mapper.toDomain
import com.atelbay.money_manager.data.recurring.mapper.toEntity
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionRepositoryImpl @Inject constructor(
    private val recurringDao: RecurringTransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
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
        )
    }

    override suspend fun getActiveRecurrings(): List<RecurringTransaction> {
        val entities = recurringDao.getActiveRecurrings()
        return entities.map { entity ->
            val category = categoryDao.getById(entity.categoryId)
            val account = accountDao.getById(entity.accountId)
            entity.toDomain(
                categoryName = category?.name.orEmpty(),
                categoryIcon = category?.icon.orEmpty(),
                categoryColor = category?.color ?: 0xFF90A4AEL,
                accountName = account?.name.orEmpty(),
            )
        }
    }

    override suspend fun save(recurring: RecurringTransaction): Long {
        val now = System.currentTimeMillis()
        val baseEntity = recurring.toEntity()
        return if (baseEntity.id == 0L) {
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
    }

    override suspend fun delete(id: Long) {
        recurringDao.softDelete(id, System.currentTimeMillis())
    }

    override suspend fun toggleActive(id: Long, isActive: Boolean) {
        val entity = recurringDao.getById(id) ?: return
        recurringDao.update(entity.copy(isActive = isActive, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateLastGeneratedDate(id: Long, date: Long) {
        recurringDao.updateLastGeneratedDate(id, date, System.currentTimeMillis())
    }
}
