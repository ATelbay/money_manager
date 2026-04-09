package com.atelbay.money_manager.domain.debts.usecase

import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtPayment
import com.atelbay.money_manager.domain.debts.repository.DebtPaymentRepository
import com.atelbay.money_manager.domain.debts.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetDebtWithPaymentsUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val debtPaymentRepository: DebtPaymentRepository,
) {
    operator fun invoke(id: Long): Flow<Pair<Debt?, List<DebtPayment>>> =
        combine(
            debtRepository.observeById(id),
            debtPaymentRepository.observeByDebtId(id),
        ) { debt, payments -> debt to payments }
}
