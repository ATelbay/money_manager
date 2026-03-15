package com.atelbay.money_manager.domain.transactions.usecase

import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsByCategoryAndDateRangeUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(
        categoryId: Long,
        transactionType: TransactionType,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<Transaction>> = repository.observeByCategoryTypeAndDateRange(
        categoryId = categoryId,
        transactionType = transactionType,
        startMillis = startMillis,
        endMillis = endMillis,
    )
}
