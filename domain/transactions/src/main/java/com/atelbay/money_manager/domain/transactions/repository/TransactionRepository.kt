package com.atelbay.money_manager.domain.transactions.repository

import com.atelbay.money_manager.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    suspend fun save(transaction: Transaction): Long
    suspend fun delete(id: Long)
}
