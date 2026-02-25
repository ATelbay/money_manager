package com.atelbay.money_manager.domain.categories.usecase

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.categories.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> =
        repository.observeByType(type)
}
