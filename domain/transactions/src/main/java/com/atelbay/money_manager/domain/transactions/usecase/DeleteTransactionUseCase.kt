package com.atelbay.money_manager.domain.transactions.usecase

import com.atelbay.money_manager.domain.transactions.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
