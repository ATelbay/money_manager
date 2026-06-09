package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.CategoryMetadata
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import java.time.YearMonth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class StatisticsChartPoint(
    val bucketStartMillis: Long,
    val displayLabel: String,
    val amount: Long?,
    val isToday: Boolean = false,
)

data class StatisticsChartState(
    val points: ImmutableList<StatisticsChartPoint> = persistentListOf(),
    val isScrollable: Boolean = false,
    val allAmountsZero: Boolean = false,
)

data class StatisticsCategoryDisplayItem(
    val category: CategoryMetadata,
    val displayAmount: Long?,
    val displayPercentage: Int = 0,
)

data class StatisticsDisplayDailyTotal(
    val date: Long,
    val amount: Long?,
)

data class StatisticsDisplayMonthlyTotal(
    val year: Int,
    val month: Int,
    val label: String,
    val amount: Long?,
)

data class StatisticsState(
    val period: StatsPeriod = StatsPeriod.MONTH,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateRange: StatisticsDateRange? = null,
    val displayedTotalExpenses: Long? = null,
    val displayedTotalIncome: Long? = null,
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
