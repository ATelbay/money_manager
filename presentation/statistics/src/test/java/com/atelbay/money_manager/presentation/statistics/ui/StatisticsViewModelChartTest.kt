package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.DailyTotal
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StatisticsViewModelChartTest {

    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()

    // A past week range: Jan 10–16, 2026. None of these days is today (2026-03-17).
    private val weekRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.JANUARY, 10),
        endMillis = utcMillis(2026, Calendar.JANUARY, 16, 23, 59, 59, 999),
    )

    // A past month range: Feb 15 – Mar 16, 2026.
    // endMillis is Mar 16 which IS today (2026-03-17 UTC midnight → Mar 16 end-of-day
    // is still before Mar 17 midnight, so todayIndex = last point).
    private val monthRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.FEBRUARY, 15),
        endMillis = utcMillis(2026, Calendar.MARCH, 16, 23, 59, 59, 999),
    )

    // A truly past range where no point is today.
    private val pastMonthRange = StatisticsDateRange(
        startMillis = utcMillis(2025, Calendar.NOVEMBER, 1),
        endMillis = utcMillis(2025, Calendar.NOVEMBER, 30, 23, 59, 59, 999),
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
    fun `chartModelProducer is initialized and not null`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to emptyFlow(),
                StatsPeriod.WEEK to emptyFlow(),
                StatsPeriod.YEAR to emptyFlow(),
            ),
        )

        assertNotNull(viewModel.chartModelProducer)
    }

    @Test
    fun `loading week data with 7 points updates chart state and does not crash chartModelProducer`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val weekExpenseTotals = dailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> (index + 1) * 100.0 },
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.WEEK to flowOf(
                    summary(dateRange = weekRange, dailyExpenses = weekExpenseTotals),
                ),
                StatsPeriod.MONTH to emptyFlow(),
                StatsPeriod.YEAR to emptyFlow(),
            ),
        )

        viewModel.setPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(StatsPeriod.WEEK, state.period)
        assertEquals(7, state.chart.points.size)
        // chartModelProducer.runTransaction runs async; we verify no crash and producer exists
        assertNotNull(viewModel.chartModelProducer)
    }

    @Test
    fun `isToday marks exactly the last day of the range regardless of calendar today`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        // The ViewModel sets isToday = (bucketStart == startOfDay(endMillis)), not actual calendar
        // today. For a 30-day range the last point is the only one marked as today.
        val pastExpenseTotals = dailyTotals(
            startMillis = pastMonthRange.startMillis,
            count = 30,
            amountAtIndex = { index -> (index + 1) * 10.0 },
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to flowOf(
                    summary(dateRange = pastMonthRange, dailyExpenses = pastExpenseTotals),
                ),
                StatsPeriod.WEEK to emptyFlow(),
                StatsPeriod.YEAR to emptyFlow(),
            ),
            rangeForMonth = pastMonthRange,
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(30, state.chart.points.size)
        // Exactly one point (the last) should be marked as today (end-of-range day)
        assertEquals(1, state.chart.points.count { it.isToday })
        assertTrue("Last point should be marked as today", state.chart.points.last().isToday)
        // All earlier points are not marked as today
        assertTrue("No earlier point should be marked as today",
            state.chart.points.dropLast(1).none { it.isToday })
        // todayIndex points to the last element
        val todayIndex = state.chart.points.indexOfFirst { it.isToday }
        assertEquals(29, todayIndex)
    }

    @Test
    fun `toggling transaction type updates chart state without crash`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val weekExpenseTotals = dailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> (index + 1) * 100.0 },
        )
        val weekIncomeTotals = dailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> (index + 1) * 50.0 },
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.WEEK to flowOf(
                    summary(
                        dateRange = weekRange,
                        dailyExpenses = weekExpenseTotals,
                        dailyIncome = weekIncomeTotals,
                    ),
                ),
                StatsPeriod.MONTH to emptyFlow(),
                StatsPeriod.YEAR to emptyFlow(),
            ),
        )

        viewModel.setPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        val expenseState = viewModel.state.value
        assertEquals(TransactionType.EXPENSE, expenseState.transactionType)
        val expenseAmounts = expenseState.chart.points.map { it.amount }

        viewModel.setTransactionType(TransactionType.INCOME)
        advanceUntilIdle()

        val incomeState = viewModel.state.value
        assertEquals(TransactionType.INCOME, incomeState.transactionType)
        val incomeAmounts = incomeState.chart.points.map { it.amount }

        // Income amounts should differ from expense amounts
        assertTrue("Income chart amounts should differ from expense amounts",
            incomeAmounts != expenseAmounts)
        // chartModelProducer should still be live after the type toggle
        assertNotNull(viewModel.chartModelProducer)
    }

    // region helpers

    private fun createViewModel(
        flows: Map<StatsPeriod, kotlinx.coroutines.flow.Flow<PeriodSummary>>,
        rangeForMonth: StatisticsDateRange = monthRange,
    ): StatisticsViewModel = createViewModel(
        flows = flows,
        weekRange = weekRange,
        monthRange = monthRange,
        yearRange = yearRange,
        rangeForMonth = rangeForMonth,
    )

    private fun summary(
        dateRange: StatisticsDateRange,
        dailyExpenses: List<DailyTotal> = emptyList(),
        dailyIncome: List<DailyTotal> = emptyList(),
    ) = PeriodSummary(
        dateRange = dateRange,
        totalExpenses = dailyExpenses.sumOf { it.amount },
        totalIncome = dailyIncome.sumOf { it.amount },
        expensesByCategory = emptyList(),
        incomesByCategory = emptyList(),
        dailyExpenses = dailyExpenses,
        dailyIncome = dailyIncome,
        monthlyExpenses = emptyList(),
        monthlyIncome = emptyList(),
    )

    // endregion
}
