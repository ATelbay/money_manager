package com.atelbay.money_manager.feature.transactions.domain.repository

import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    fun observeCategories(type: TransactionType): Flow<List<Category>>
    suspend fun save(transaction: Transaction): Long
    suspend fun delete(id: Long)
}
