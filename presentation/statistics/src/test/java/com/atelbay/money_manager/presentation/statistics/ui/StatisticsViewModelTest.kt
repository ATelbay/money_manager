package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
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
        Dispatchers.setMain(testDispatcher)
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
    fun `initial state exposes month chart metadata before summary arrives`() = runTest(testDispatcher) {

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
        assertTrue(state.chart.points.isEmpty())
        assertTrue(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `success path derives daily chart points and today marker for week and month`() = runTest(testDispatcher) {

        val monthExpenseTotals = displayDailyTotals(
            startMillis = monthRange.startMillis,
            count = 30,
            amountAtIndex = { index -> if (index == 29) 0L else (index + 1) * 1000L },
        )
        val monthIncomeTotals = displayDailyTotals(
            startMillis = monthRange.startMillis,
            count = 30,
            amountAtIndex = { index -> if (index == 29) 0L else (index + 1) * 500L },
        )
        val weekExpenseTotals = displayDailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> if (index == 6) 0L else (index + 1) * 10_000L },
        )
        val weekIncomeTotals = displayDailyTotals(
            startMillis = weekRange.startMillis,
            count = 7,
            amountAtIndex = { index -> if (index == 6) 0L else (index + 1) * 5_000L },
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to flowOf(skeletonSummary(monthRange)),
                StatsPeriod.WEEK to flowOf(skeletonSummary(weekRange)),
                StatsPeriod.YEAR to emptyFlow(),
            ),
            resolutions = mapOf(
                monthRange to resolutionOf(
                    displayedDailyExpenses = monthExpenseTotals,
                    displayedDailyIncome = monthIncomeTotals,
                ),
                weekRange to resolutionOf(
                    displayedDailyExpenses = weekExpenseTotals,
                    displayedDailyIncome = weekIncomeTotals,
                ),
            ),
        )

        advanceUntilIdle()

        val monthState = viewModel.state.value
        assertEquals(30, monthState.chart.points.size)
        assertEquals(1, monthState.chart.points.count { it.isToday })
        assertEquals(0L, monthState.chart.points.last().amount ?: -1L)
        assertEquals(startOfDayUtc(monthRange.endMillis), monthState.chart.points.last().bucketStartMillis)
        assertTrue(monthState.chart.points.last().isToday)

        viewModel.setTransactionType(TransactionType.INCOME)

        val incomeState = viewModel.state.value
        assertEquals(monthIncomeTotals.last().amount, incomeState.chart.points.last().amount ?: -1L)
        assertTrue(incomeState.chart.points.last().isToday)

        viewModel.setPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        val weekState = viewModel.state.value
        assertEquals(StatsPeriod.WEEK, weekState.period)
        assertEquals(7, weekState.chart.points.size)
        assertEquals(1, weekState.chart.points.count { it.isToday })
        assertEquals(
            SimpleDateFormat("EEE", Locale.US).format(Date(weekRange.endMillis)),
            weekState.chart.points.last().displayLabel,
        )
        assertEquals(0L, weekState.chart.points.last().amount ?: -1L)
        assertTrue(weekState.chart.points.last().isToday)
    }

    @Test
    fun `year success path uses monthly title and never marks a point as today`() = runTest(testDispatcher) {

        val yearExpenseTotals = listOf(
            StatisticsDisplayMonthlyTotal(2025, Calendar.APRIL, "Apr", 1_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.MAY, "May", 2_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.JUNE, "Jun", 3_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.JULY, "Jul", 4_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.AUGUST, "Aug", 5_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.SEPTEMBER, "Sep", 6_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.OCTOBER, "Oct", 7_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.NOVEMBER, "Nov", 8_000L),
            StatisticsDisplayMonthlyTotal(2025, Calendar.DECEMBER, "Dec", 9_000L),
            StatisticsDisplayMonthlyTotal(2026, Calendar.JANUARY, "Jan", 10_000L),
            StatisticsDisplayMonthlyTotal(2026, Calendar.FEBRUARY, "Feb", 11_000L),
            StatisticsDisplayMonthlyTotal(2026, Calendar.MARCH, "Mar", 12_000L),
        )

        val viewModel = createViewModel(
            flows = mapOf(
                StatsPeriod.MONTH to emptyFlow(),
                StatsPeriod.WEEK to emptyFlow(),
                StatsPeriod.YEAR to flowOf(skeletonSummary(yearRange)),
            ),
            resolutions = mapOf(
                yearRange to resolutionOf(
                    displayedMonthlyExpenses = yearExpenseTotals,
                    displayedMonthlyIncome = yearExpenseTotals,
                ),
            ),
        )

        viewModel.setPeriod(StatsPeriod.YEAR)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(12, state.chart.points.size)
        assertEquals(yearExpenseTotals.map { it.label }, state.chart.points.map { it.displayLabel })
        assertFalse(state.chart.points.any { it.isToday })
    }

    @Test
    fun `error path keeps chart title and resolved date range available`() = runTest(testDispatcher) {

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
        assertTrue(state.chart.points.isEmpty())
        assertEquals(unavailableError, state.error)
        assertFalse(state.isLoading)
    }

    private fun createViewModel(
        flows: Map<StatsPeriod, Flow<PeriodSummary>>,
        resolutions: Map<StatisticsDateRange, StatisticsCurrencyResolution> = emptyMap(),
    ): StatisticsViewModel = createViewModel(
        flows = flows,
        weekRange = weekRange,
        monthRange = monthRange,
        yearRange = yearRange,
        resolutions = resolutions,
        testDispatcher = testDispatcher,
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
