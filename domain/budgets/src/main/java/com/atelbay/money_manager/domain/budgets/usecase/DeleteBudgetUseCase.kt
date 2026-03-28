package com.atelbay.money_manager.domain.budgets.usecase

import com.atelbay.money_manager.domain.budgets.repository.BudgetRepository
import javax.inject.Inject

class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(id: Long) {
        budgetRepository.delete(id)
    }
}
