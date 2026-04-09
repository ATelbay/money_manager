package com.atelbay.money_manager.domain.debts.repository

import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtPayment
import kotlinx.coroutines.flow.Flow

interface DebtPaymentRepository {
    fun observeByDebtId(debtId: Long): Flow<List<DebtPayment>>
    suspend fun save(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long
    suspend fun delete(id: Long)
    suspend fun deleteAllByDebtId(debtId: Long)
}
