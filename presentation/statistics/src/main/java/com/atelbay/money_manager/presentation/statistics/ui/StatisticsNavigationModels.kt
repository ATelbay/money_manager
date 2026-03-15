package com.atelbay.money_manager.presentation.statistics.ui

data class StatisticsCategoryDrillDownRequest(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val transactionType: String,
    val period: String,
    val startMillis: Long,
    val endMillis: Long,
)
