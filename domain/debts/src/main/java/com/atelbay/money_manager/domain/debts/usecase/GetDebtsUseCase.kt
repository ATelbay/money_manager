package com.atelbay.money_manager.domain.debts.usecase

import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.domain.debts.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDebtsUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    operator fun invoke(): Flow<List<Debt>> = debtRepository.observeAll()
}
