package com.atelbay.money_manager.domain.recurring.usecase

import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import javax.inject.Inject

class GetRecurringTransactionByIdUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository,
) {
    suspend operator fun invoke(id: Long): RecurringTransaction? = repository.getById(id)
}
