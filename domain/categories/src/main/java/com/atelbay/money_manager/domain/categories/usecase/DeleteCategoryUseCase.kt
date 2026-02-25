package com.atelbay.money_manager.domain.categories.usecase

import com.atelbay.money_manager.domain.categories.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
