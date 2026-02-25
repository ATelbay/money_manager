package com.atelbay.money_manager.domain.categories.usecase

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.domain.categories.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoryByIdUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(id: Long): Flow<Category?> = repository.observeById(id)
}
