package com.atelbay.money_manager.domain.recurring.repository

import com.atelbay.money_manager.core.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>
    suspend fun getById(id: Long): RecurringTransaction?
    suspend fun getActiveRecurrings(): List<RecurringTransaction>
    suspend fun save(recurring: RecurringTransaction): Long
    suspend fun delete(id: Long)
    suspend fun toggleActive(id: Long, isActive: Boolean)
    suspend fun updateLastGeneratedDate(id: Long, date: Long)
}
