package com.atelbay.money_manager.core.model

data class RecurringTransaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val accountId: Long,
    val accountName: String,
    val note: String?,
    val frequency: Frequency,
    val startDate: Long,
    val endDate: Long?,
    val dayOfMonth: Int?,
    val dayOfWeek: Int?,
    val lastGeneratedDate: Long?,
    val isActive: Boolean,
    val createdAt: Long,
)
