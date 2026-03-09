package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.CategorySummary
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.MonthlyTotal
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class StatisticsState(
    val period: StatsPeriod = StatsPeriod.MONTH,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val expensesByCategory: ImmutableList<CategorySummary> = persistentListOf(),
    val incomesByCategory: ImmutableList<CategorySummary> = persistentListOf(),
    val dailyExpenses: ImmutableList<DailyTotal> = persistentListOf(),
    val dailyIncome: ImmutableList<DailyTotal> = persistentListOf(),
    val monthlyExpenses: ImmutableList<MonthlyTotal> = persistentListOf(),
    val monthlyIncome: ImmutableList<MonthlyTotal> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
