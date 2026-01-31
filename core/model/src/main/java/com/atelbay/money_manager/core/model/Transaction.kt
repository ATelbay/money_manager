package com.atelbay.money_manager.core.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val accountId: Long,
    val note: String?,
    val date: Long,
    val createdAt: Long,
)
