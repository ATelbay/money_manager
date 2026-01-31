package com.atelbay.money_manager.feature.transactions.domain.usecase

import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.feature.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class SaveTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Long = repository.save(transaction)
}
