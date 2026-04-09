package com.atelbay.money_manager.domain.debts.usecase

import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.domain.debts.repository.DebtRepository
import javax.inject.Inject

class SaveDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    suspend operator fun invoke(debt: Debt): Long {
        require(debt.contactName.isNotBlank()) { "Contact name must not be empty" }
        require(debt.totalAmount > 0) { "Total amount must be positive" }
        return debtRepository.save(debt)
    }
}
