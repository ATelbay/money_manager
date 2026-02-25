package com.atelbay.money_manager.domain.transactions.usecase

import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(id: Long): Flow<Transaction?> = repository.observeById(id)
}
