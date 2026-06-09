package com.atelbay.money_manager.core.model

data class DebtPayment(
    val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val date: Long,
    val note: String? = null,
    val transactionId: Long? = null,
    val createdAt: Long,
)
