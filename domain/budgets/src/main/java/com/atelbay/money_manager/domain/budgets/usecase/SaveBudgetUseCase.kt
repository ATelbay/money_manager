package com.atelbay.money_manager.domain.budgets.usecase

import com.atelbay.money_manager.core.model.Budget
import com.atelbay.money_manager.domain.budgets.repository.BudgetRepository
import javax.inject.Inject

class SaveBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(
        categoryId: Long,
        monthlyLimit: Double,
        budgetId: Long? = null,
    ): Long {
        val existing = when {
            budgetId != null -> budgetRepository.getById(budgetId)
            else -> budgetRepository.getByCategoryId(categoryId)
        }
        val budget = if (existing != null) {
            existing.copy(categoryId = categoryId, monthlyLimit = monthlyLimit)
        } else {
            Budget(
                categoryId = categoryId,
                categoryName = "",
                categoryIcon = "",
                categoryColor = 0L,
                monthlyLimit = monthlyLimit,
                spent = 0.0,
                remaining = monthlyLimit,
                percentage = 0f,
            )
        }
        return budgetRepository.save(budget)
    }
}
