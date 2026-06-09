package com.atelbay.money_manager.data.budgets.mapper

import com.atelbay.money_manager.core.database.entity.BudgetEntity
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.model.Budget

fun BudgetEntity.toDomain(category: CategoryEntity?): Budget = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = category?.name.orEmpty(),
    categoryIcon = category?.icon.orEmpty(),
    categoryColor = category?.color ?: 0L,
    monthlyLimit = monthlyLimit,
    spent = 0L,
    remaining = monthlyLimit,
    percentage = 0f,
)

// createdAt is supplied by the repository on insert/update (kept 0L here so the mapper stays pure).
fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    monthlyLimit = monthlyLimit,
    createdAt = 0L,
)
