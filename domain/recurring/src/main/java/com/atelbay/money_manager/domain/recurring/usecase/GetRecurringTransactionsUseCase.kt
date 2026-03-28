package com.atelbay.money_manager.domain.recurring.usecase

import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.domain.recurring.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecurringTransactionsUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository,
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> = repository.observeAll()
}
