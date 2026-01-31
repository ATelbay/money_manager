package com.atelbay.money_manager.feature.statistics.domain.model

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val totalAmount: Double,
    val percentage: Float,
)

data class DailyTotal(
    val date: Long,
    val amount: Double,
)

data class PeriodSummary(
    val totalExpenses: Double,
    val totalIncome: Double,
    val expensesByCategory: List<CategorySummary>,
    val dailyExpenses: List<DailyTotal>,
)

enum class StatsPeriod {
    WEEK,
    MONTH,
    YEAR,
}
