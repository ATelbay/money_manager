package com.atelbay.money_manager.domain.debts.usecase

import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtPayment
import com.atelbay.money_manager.domain.debts.repository.DebtPaymentRepository
import javax.inject.Inject

class AddDebtPaymentUseCase @Inject constructor(
    private val debtPaymentRepository: DebtPaymentRepository,
) {
    suspend operator fun invoke(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long =
        debtPaymentRepository.save(payment, createTransaction, debt)
}
