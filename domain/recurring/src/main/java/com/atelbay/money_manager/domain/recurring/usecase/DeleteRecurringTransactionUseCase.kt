package com.atelbay.money_manager.domain.recurring.usecase

import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import javax.inject.Inject

class DeleteRecurringTransactionUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
