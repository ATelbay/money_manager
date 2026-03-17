package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.MonthlyTotal
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import java.time.YearMonth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class StatisticsChartPoint(
    val bucketStartMillis: Long,
    val displayLabel: String,
    val amount: Double?,
    val isToday: Boolean = false,
)

data class StatisticsChartState(
    val title: String = "",
    val dateRangeLabel: String = "",
    val points: ImmutableList<StatisticsChartPoint> = persistentListOf(),
    val isScrollable: Boolean = false,
)

data class StatisticsCategoryDisplayItem(
    val category: CategorySummary,
    val displayAmount: Double?,
    val displayPercentage: Int = category.percentage,
)

data class StatisticsDisplayDailyTotal(
    val date: Long,
    val amount: Double?,
)

data class StatisticsDisplayMonthlyTotal(
    val year: Int,
    val month: Int,
    val label: String,
    val amount: Double?,
)

data class StatisticsState(
    val period: StatsPeriod = StatsPeriod.MONTH,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateRange: StatisticsDateRange? = null,
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val expensesByCategory: ImmutableList<CategorySummary> = persistentListOf(),
    val incomesByCategory: ImmutableList<CategorySummary> = persistentListOf(),
    val dailyExpenses: ImmutableList<DailyTotal> = persistentListOf(),
    val dailyIncome: ImmutableList<DailyTotal> = persistentListOf(),
    val monthlyExpenses: ImmutableList<MonthlyTotal> = persistentListOf(),
    val monthlyIncome: ImmutableList<MonthlyTotal> = persistentListOf(),
    val displayedTotalExpenses: Double? = null,
    val displayedTotalIncome: Double? = null,
    val displayedExpensesByCategory: ImmutableList<StatisticsCategoryDisplayItem> = persistentListOf(),
    val displayedIncomesByCategory: ImmutableList<StatisticsCategoryDisplayItem> = persistentListOf(),
    val displayedDailyExpenses: ImmutableList<StatisticsDisplayDailyTotal> = persistentListOf(),
    val displayedDailyIncome: ImmutableList<StatisticsDisplayDailyTotal> = persistentListOf(),
    val displayedMonthlyExpenses: ImmutableList<StatisticsDisplayMonthlyTotal> = persistentListOf(),
    val displayedMonthlyIncome: ImmutableList<StatisticsDisplayMonthlyTotal> = persistentListOf(),
    val currencyUiState: StatisticsCurrencyUiState = StatisticsCurrencyUiState(),
    val chart: StatisticsChartState = StatisticsChartState(),
    val selectedMonth: YearMonth? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
