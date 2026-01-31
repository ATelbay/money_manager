package com.atelbay.money_manager.feature.statistics.ui

import com.atelbay.money_manager.feature.statistics.domain.model.CategorySummary
import com.atelbay.money_manager.feature.statistics.domain.model.DailyTotal
import com.atelbay.money_manager.feature.statistics.domain.model.StatsPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class StatisticsState(
    val period: StatsPeriod = StatsPeriod.MONTH,
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val expensesByCategory: ImmutableList<CategorySummary> = persistentListOf(),
    val dailyExpenses: ImmutableList<DailyTotal> = persistentListOf(),
    val isLoading: Boolean = true,
)
