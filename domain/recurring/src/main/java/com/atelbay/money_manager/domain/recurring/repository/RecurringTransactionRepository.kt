package com.atelbay.money_manager.domain.recurring.repository

import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>
    suspend fun getById(id: Long): RecurringTransaction?
    suspend fun getActiveRecurrings(): List<RecurringTransaction>
    suspend fun save(recurring: RecurringTransaction): Long
    suspend fun delete(id: Long)
    suspend fun toggleActive(id: Long, isActive: Boolean)
    suspend fun updateLastGeneratedDate(id: Long, date: Long)

    /**
     * Atomically inserts all [transactions] for the given recurring template,
     * updates account balances, and sets [lastGeneratedDate] — all within a
     * single Room transaction. If the process dies mid-way, nothing is persisted.
     */
    suspend fun generateTransactionsAtomically(
        recurringId: Long,
        transactions: List<Transaction>,
        lastGeneratedDate: Long,
    )
}
