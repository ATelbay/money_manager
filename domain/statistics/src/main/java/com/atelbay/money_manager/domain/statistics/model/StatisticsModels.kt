package com.atelbay.money_manager.domain.statistics.model

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val totalAmount: Double,
    val percentage: Int,
)

data class DailyTotal(
    val date: Long,
    val amount: Double,
)

data class MonthlyTotal(
    val year: Int,
    val month: Int,
    val amount: Double,
    val label: String,
)

data class StatisticsDateRange(
    val startMillis: Long,
    val endMillis: Long,
)

enum class TransactionType {
    EXPENSE,
    INCOME,
}

data class PeriodSummary(
    val dateRange: StatisticsDateRange,
    val totalExpenses: Double,
    val totalIncome: Double,
    val expensesByCategory: List<CategorySummary>,
    val incomesByCategory: List<CategorySummary>,
    val dailyExpenses: List<DailyTotal>,
    val dailyIncome: List<DailyTotal>,
    val monthlyExpenses: List<MonthlyTotal>,
    val monthlyIncome: List<MonthlyTotal>,
)

enum class StatsPeriod {
    WEEK,
    MONTH,
    YEAR,
}
