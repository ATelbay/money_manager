package com.atelbay.money_manager.core.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val monthlyLimit: Long,
    val spent: Long,
    val remaining: Long,
    val percentage: Float,
)
