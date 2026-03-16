package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.MonthlyTotal
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StatisticsViewModelTest {

    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()

    private val weekRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.JANUARY, 10),
        endMillis = utcMillis(2026, Calendar.JANUARY, 16, 23, 59, 59, 999),
    )
    private val monthRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.FEBRUARY, 15),
        endMillis = utcMillis(2026, Calendar.MARCH, 16, 23, 59, 59, 999),
    )
    private val yearRange = StatisticsDateRange(
        startMillis = utcMillis(2025, Calendar.APRIL, 1),
        endMillis = utcMillis(2026, Calendar.MARCH, 16, 23, 59, 59, 999),
    )

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `initial state exposes month chart metadata before summary arrives`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to emptyFlow(),
                StatsPeriod.WEEK to emptyFlow(),
                StatsPeriod.YEAR to emptyFlow(),
            ),
        )

        val state = viewModel.state.value
        assertEquals(StatsPeriod.MONTH, state.period)
        assertEquals(TransactionType.EXPENSE, state.transactionType)
        assertEquals(monthRange, state.dateRange)
        assertEquals("Expenses by day", state.chart.title)
        assertEquals("Feb 15 - Mar 16, 2026", state.chart.dateRangeLabel)
        assertTrue(state.chart.points.isEmpty())
        assertTrue(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `success path derives daily chart points and today marker for week and month`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val monthExpenseTotals = dailyTotals(
            startMillis = monthRange.startMillis,
            count = 30,
            amountAtIndex = { index -> if (index == 29) 0.0 else (index + 1) * 10.0 },
        )
        val monthIncomeTotals = dailyTotals(
            startMillis = monthRange.startMillis,
            count = 30,
            amountAtIndex = { index -> if (index == 29) 0.0 else (index + 1) * 5.0 },
        )
        val weekExpenseTotals = dailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> if (index == 6) 0.0 else (index + 1) * 100.0 },
        )
        val weekIncomeTotals = dailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> if (index == 6) 0.0 else (index + 1) * 50.0 },
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to flowOf(
                    summary(
                        dateRange = monthRange,
                        dailyExpenses = monthExpenseTotals,
                        dailyIncome = monthIncomeTotals,
                    ),
                ),
                StatsPeriod.WEEK to flowOf(
                    summary(
                        dateRange = weekRange,
                        dailyExpenses = weekExpenseTotals,
                        dailyIncome = weekIncomeTotals,
                    ),
                ),
                StatsPeriod.YEAR to emptyFlow(),
            ),
        )

        advanceUntilIdle()

        val monthState = viewModel.state.value
        assertEquals("Expenses by day", monthState.chart.title)
        assertEquals("Feb 15 - Mar 16, 2026", monthState.chart.dateRangeLabel)
        assertEquals(30, monthState.chart.points.size)
        assertEquals(1, monthState.chart.points.count { it.isToday })
        assertEquals(0.0, monthState.chart.points.last().amount ?: -1.0, 0.0)
        assertEquals(startOfDayUtc(monthRange.endMillis), monthState.chart.points.last().bucketStartMillis)
        assertTrue(monthState.chart.points.last().isToday)

        viewModel.setTransactionType(TransactionType.INCOME)

        val incomeState = viewModel.state.value
        assertEquals("Income by day", incomeState.chart.title)
        assertEquals(monthIncomeTotals.last().amount, incomeState.chart.points.last().amount ?: -1.0, 0.0)
        assertTrue(incomeState.chart.points.last().isToday)

        viewModel.setPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        val weekState = viewModel.state.value
        assertEquals(StatsPeriod.WEEK, weekState.period)
        assertEquals("Income by day", weekState.chart.title)
        assertEquals("Jan 10-16, 2026", weekState.chart.dateRangeLabel)
        assertEquals(7, weekState.chart.points.size)
        assertEquals(1, weekState.chart.points.count { it.isToday })
        assertEquals(
            SimpleDateFormat("EEE", Locale.US).format(Date(weekRange.endMillis)),
            weekState.chart.points.last().displayLabel,
        )
        assertEquals(0.0, weekState.chart.points.last().amount ?: -1.0, 0.0)
        assertTrue(weekState.chart.points.last().isToday)
    }

    @Test
    fun `year success path uses monthly title and never marks a point as today`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val yearExpenseTotals = listOf(
            MonthlyTotal(2025, Calendar.APRIL, 10.0, "Apr"),
            MonthlyTotal(2025, Calendar.MAY, 20.0, "May"),
            MonthlyTotal(2025, Calendar.JUNE, 30.0, "Jun"),
            MonthlyTotal(2025, Calendar.JULY, 40.0, "Jul"),
            MonthlyTotal(2025, Calendar.AUGUST, 50.0, "Aug"),
            MonthlyTotal(2025, Calendar.SEPTEMBER, 60.0, "Sep"),
            MonthlyTotal(2025, Calendar.OCTOBER, 70.0, "Oct"),
            MonthlyTotal(2025, Calendar.NOVEMBER, 80.0, "Nov"),
            MonthlyTotal(2025, Calendar.DECEMBER, 90.0, "Dec"),
            MonthlyTotal(2026, Calendar.JANUARY, 100.0, "Jan"),
            MonthlyTotal(2026, Calendar.FEBRUARY, 110.0, "Feb"),
            MonthlyTotal(2026, Calendar.MARCH, 120.0, "Mar"),
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to emptyFlow(),
                StatsPeriod.WEEK to emptyFlow(),
                StatsPeriod.YEAR to flowOf(
                    summary(
                        dateRange = yearRange,
                        monthlyExpenses = yearExpenseTotals,
                        monthlyIncome = yearExpenseTotals,
                    ),
                ),
            ),
        )

        viewModel.setPeriod(StatsPeriod.YEAR)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Expenses by month", state.chart.title)
        assertEquals("Apr 1, 2025 - Mar 16, 2026", state.chart.dateRangeLabel)
        assertEquals(12, state.chart.points.size)
        assertEquals(yearExpenseTotals.map { it.label }, state.chart.points.map { it.displayLabel })
        assertFalse(state.chart.points.any { it.isToday })
    }

    @Test
    fun `error path keeps chart title and resolved date range available`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val unavailableError = "Summary unavailable"
        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to flow<PeriodSummary> { throw IllegalStateException(unavailableError) },
                StatsPeriod.WEEK to emptyFlow(),
                StatsPeriod.YEAR to emptyFlow(),
            ),
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Expenses by day", state.chart.title)
        assertEquals("Feb 15 - Mar 16, 2026", state.chart.dateRangeLabel)
        assertTrue(state.chart.points.isEmpty())
        assertEquals(unavailableError, state.error)
        assertFalse(state.isLoading)
    }

    private fun createViewModel(
        flows: Map<StatsPeriod, Flow<PeriodSummary>>,
    ): StatisticsViewModel = createViewModel(
        flows = flows,
        weekRange = weekRange,
        monthRange = monthRange,
        yearRange = yearRange,
    )

    private fun summary(
        dateRange: StatisticsDateRange,
        dailyExpenses: List<DailyTotal> = emptyList(),
        dailyIncome: List<DailyTotal> = emptyList(),
        monthlyExpenses: List<MonthlyTotal> = emptyList(),
        monthlyIncome: List<MonthlyTotal> = emptyList(),
    ) = PeriodSummary(
        dateRange = dateRange,
        totalExpenses = dailyExpenses.sumOf { it.amount } + monthlyExpenses.sumOf { it.amount },
        totalIncome = dailyIncome.sumOf { it.amount } + monthlyIncome.sumOf { it.amount },
        expensesByCategory = emptyList(),
        incomesByCategory = emptyList(),
        dailyExpenses = dailyExpenses,
        dailyIncome = dailyIncome,
        monthlyExpenses = monthlyExpenses,
        monthlyIncome = monthlyIncome,
    )

    private fun startOfDayUtc(timestamp: Long): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}
