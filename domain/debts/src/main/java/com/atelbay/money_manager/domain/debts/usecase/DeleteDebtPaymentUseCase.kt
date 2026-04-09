package com.atelbay.money_manager.domain.debts.usecase

import com.atelbay.money_manager.domain.debts.repository.DebtPaymentRepository
import javax.inject.Inject

class DeleteDebtPaymentUseCase @Inject constructor(
    private val debtPaymentRepository: DebtPaymentRepository,
) {
    suspend operator fun invoke(id: Long) {
        debtPaymentRepository.delete(id)
    }
}
