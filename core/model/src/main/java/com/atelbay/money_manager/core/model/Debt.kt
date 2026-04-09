package com.atelbay.money_manager.core.model

data class Debt(
    val id: Long = 0,
    val contactName: String,
    val direction: DebtDirection,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double,
    val currency: String,
    val accountId: Long,
    val accountName: String = "",
    val note: String? = null,
    val createdAt: Long,
    val status: DebtStatus,
)
