package com.atelbay.money_manager.data.budgets.repository

import com.atelbay.money_manager.core.database.dao.BudgetDao
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.model.Budget
import com.atelbay.money_manager.data.budgets.mapper.toDomain
import com.atelbay.money_manager.data.budgets.mapper.toEntity
import com.atelbay.money_manager.data.sync.SyncManager
import com.atelbay.money_manager.domain.budgets.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager,
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> =
        combine(
            budgetDao.observeAll(),
            categoryDao.observeAll().distinctUntilChanged(),
        ) { budgets, categories ->
            val categoryMap = categories.associateBy { it.id }
            budgets.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override suspend fun getById(id: Long): Budget? {
        val entity = budgetDao.getById(id) ?: return null
        val category = categoryDao.getById(entity.categoryId)
        return entity.toDomain(category)
    }

    override suspend fun getByCategoryId(categoryId: Long): Budget? {
        val entity = budgetDao.getByCategoryId(categoryId) ?: return null
        val category = categoryDao.getById(entity.categoryId)
        return entity.toDomain(category)
    }

    override suspend fun save(budget: Budget): Long {
        val now = System.currentTimeMillis()
        val baseEntity = budget.toEntity()
        val savedId = if (baseEntity.id == 0L) {
            val newEntity = baseEntity.copy(createdAt = now, updatedAt = now)
            budgetDao.insert(newEntity)
        } else {
            val existing = budgetDao.getById(baseEntity.id)
            val updatedEntity = baseEntity.copy(
                createdAt = existing?.createdAt ?: now,
                remoteId = existing?.remoteId,
                isDeleted = existing?.isDeleted ?: false,
                updatedAt = now,
            )
            budgetDao.update(updatedEntity)
            updatedEntity.id
        }
        syncManager.syncBudget(savedId)
        return savedId
    }

    override suspend fun delete(id: Long) {
        budgetDao.softDeleteById(id, System.currentTimeMillis())
        syncManager.syncBudget(id)
    }
}
