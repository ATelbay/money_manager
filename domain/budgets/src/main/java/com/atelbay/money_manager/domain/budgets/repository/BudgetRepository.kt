package com.atelbay.money_manager.domain.budgets.repository

import com.atelbay.money_manager.core.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    suspend fun getById(id: Long): Budget?
    suspend fun getByCategoryId(categoryId: Long): Budget?
    suspend fun save(budget: Budget): Long
    suspend fun delete(id: Long)
}
