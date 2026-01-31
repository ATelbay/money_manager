package com.atelbay.money_manager.feature.transactions.domain.usecase

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> =
        repository.observeCategories(type)
}
