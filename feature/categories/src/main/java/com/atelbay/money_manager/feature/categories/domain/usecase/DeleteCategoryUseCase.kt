package com.atelbay.money_manager.feature.categories.domain.usecase

import com.atelbay.money_manager.feature.categories.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
