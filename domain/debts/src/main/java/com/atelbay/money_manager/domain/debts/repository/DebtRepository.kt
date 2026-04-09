package com.atelbay.money_manager.domain.debts.repository

import com.atelbay.money_manager.core.model.Debt
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun observeAll(): Flow<List<Debt>>
    fun observeById(id: Long): Flow<Debt?>
    suspend fun getById(id: Long): Debt?
    suspend fun save(debt: Debt): Long
    suspend fun delete(id: Long)
}
