package com.atelbay.money_manager.domain.categories.usecase

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.domain.categories.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): Long = repository.save(category)
}
