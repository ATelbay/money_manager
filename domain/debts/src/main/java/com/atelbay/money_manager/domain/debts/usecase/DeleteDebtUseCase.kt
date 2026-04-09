package com.atelbay.money_manager.domain.debts.usecase

import com.atelbay.money_manager.domain.debts.repository.DebtRepository
import javax.inject.Inject

class DeleteDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    suspend operator fun invoke(id: Long) {
        debtRepository.delete(id)
    }
}
