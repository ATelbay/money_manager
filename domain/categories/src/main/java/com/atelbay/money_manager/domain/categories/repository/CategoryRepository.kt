package com.atelbay.money_manager.domain.categories.repository

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeByType(type: TransactionType): Flow<List<Category>>
    fun observeById(id: Long): Flow<Category?>
    suspend fun save(category: Category): Long
    suspend fun delete(id: Long)
}
