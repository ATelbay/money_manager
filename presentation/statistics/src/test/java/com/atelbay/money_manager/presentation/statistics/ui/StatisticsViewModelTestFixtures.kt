package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.usecase.GetPeriodSummaryUseCase
import com.atelbay.money_manager.domain.statistics.usecase.StatisticsPeriodRangeResolver
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar
import java.util.TimeZone

internal const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

internal fun utcMillis(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    hourOfDay: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    millisecond: Int = 0,
): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month)
    set(Calendar.DAY_OF_MONTH, dayOfMonth)
    set(Calendar.HOUR_OF_DAY, hourOfDay)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, second)
    set(Calendar.MILLISECOND, millisecond)
    timeInMillis
}

internal fun dailyTotals(
    startMillis: Long,
    count: Int,
    amountAtIndex: (Int) -> Double,
): List<DailyTotal> = List(count) { index ->
    DailyTotal(
        date = startMillis + index * ONE_DAY_MILLIS,
        amount = amountAtIndex(index),
    )
}

internal fun createViewModel(
    flows: Map<StatsPeriod, Flow<PeriodSummary>>,
    weekRange: StatisticsDateRange,
    monthRange: StatisticsDateRange,
    yearRange: StatisticsDateRange,
    rangeForMonth: StatisticsDateRange = monthRange,
): StatisticsViewModel {
    val getPeriodSummaryUseCase = mockk<GetPeriodSummaryUseCase>()
    val getTransactionsUseCase = mockk<GetTransactionsUseCase>()
    val getAccountsUseCase = mockk<GetAccountsUseCase>()
    val observeExchangeRateUseCase = mockk<ObserveExchangeRateUseCase>()
    val userPreferences = mockk<UserPreferences>()
    val rangeResolver = mockk<StatisticsPeriodRangeResolver>()
    val statisticsCurrencyDisplayResolver = mockk<StatisticsCurrencyDisplayResolver>()

    every { rangeResolver(StatsPeriod.WEEK, any()) } returns weekRange
    every { rangeResolver(StatsPeriod.MONTH, any()) } returns rangeForMonth
    every { rangeResolver(StatsPeriod.YEAR, any()) } returns yearRange

    every { getPeriodSummaryUseCase(StatsPeriod.WEEK, any()) } returns (flows[StatsPeriod.WEEK] ?: emptyFlow())
    every { getPeriodSummaryUseCase(StatsPeriod.MONTH, any()) } returns (flows[StatsPeriod.MONTH] ?: emptyFlow())
    every { getPeriodSummaryUseCase(StatsPeriod.YEAR, any()) } returns (flows[StatsPeriod.YEAR] ?: emptyFlow())
    every { getTransactionsUseCase() } returns flowOf(emptyList())
    every { getAccountsUseCase() } returns flowOf(emptyList())
    every { observeExchangeRateUseCase() } returns flowOf(null)
    every { userPreferences.baseCurrency } returns flowOf("KZT")
    every {
        statisticsCurrencyDisplayResolver.resolve(
            summary = any(),
            transactions = any(),
            accounts = any(),
            baseCurrency = any(),
            exchangeRate = any(),
        )
    } answers {
        val summary = firstArg<PeriodSummary>()
        StatisticsCurrencyResolution(
            currencyUiState = StatisticsCurrencyUiState(
                moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                displayMode = AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
            ),
            displayedTotalExpenses = summary.totalExpenses,
            displayedTotalIncome = summary.totalIncome,
            displayedExpensesByCategory = emptyList(),
            displayedIncomesByCategory = emptyList(),
            displayedDailyExpenses = summary.dailyExpenses.map {
                StatisticsDisplayDailyTotal(date = it.date, amount = it.amount)
            },
            displayedDailyIncome = summary.dailyIncome.map {
                StatisticsDisplayDailyTotal(date = it.date, amount = it.amount)
            },
            displayedMonthlyExpenses = summary.monthlyExpenses.map {
                StatisticsDisplayMonthlyTotal(
                    year = it.year,
                    month = it.month,
                    label = it.label,
                    amount = it.amount,
                )
            },
            displayedMonthlyIncome = summary.monthlyIncome.map {
                StatisticsDisplayMonthlyTotal(
                    year = it.year,
                    month = it.month,
                    label = it.label,
                    amount = it.amount,
                )
            },
        )
    }

    return StatisticsViewModel(
        getPeriodSummaryUseCase = getPeriodSummaryUseCase,
        getTransactionsUseCase = getTransactionsUseCase,
        getAccountsUseCase = getAccountsUseCase,
        observeExchangeRateUseCase = observeExchangeRateUseCase,
        userPreferences = userPreferences,
        rangeResolver = rangeResolver,
        statisticsCurrencyDisplayResolver = statisticsCurrencyDisplayResolver,
    )
}
