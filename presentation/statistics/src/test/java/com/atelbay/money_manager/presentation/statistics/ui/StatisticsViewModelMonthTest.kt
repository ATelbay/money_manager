package com.atelbay.money_manager.presentation.statistics.ui

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.usecase.GetPeriodSummaryUseCase
import com.atelbay.money_manager.domain.statistics.usecase.StatisticsPeriodRangeResolver
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StatisticsViewModelMonthTest {

    private val testDispatcher = StandardTestDispatcher()
    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()

    private val defaultMonthRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.FEBRUARY, 15),
        endMillis = utcMillis(2026, Calendar.MARCH, 16, 23, 59, 59, 999),
    )
    private val janAnchoredRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.JANUARY, 1),
        endMillis = utcMillis(2026, Calendar.JANUARY, 30, 23, 59, 59, 999),
    )
    private val weekRange = StatisticsDateRange(
        startMillis = utcMillis(2026, Calendar.JANUARY, 10),
        endMillis = utcMillis(2026, Calendar.JANUARY, 16, 23, 59, 59, 999),
    )
    private val yearRange = StatisticsDateRange(
        startMillis = utcMillis(2025, Calendar.APRIL, 1),
        endMillis = utcMillis(2026, Calendar.MARCH, 16, 23, 59, 59, 999),
    )

    private lateinit var getPeriodSummaryUseCase: GetPeriodSummaryUseCase
    private lateinit var getTransactionsUseCase: GetTransactionsUseCase
    private lateinit var getAccountsUseCase: GetAccountsUseCase
    private lateinit var observeExchangeRateUseCase: ObserveExchangeRateUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var rangeResolver: StatisticsPeriodRangeResolver
    private lateinit var statisticsCurrencyDisplayResolver: StatisticsCurrencyDisplayResolver

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        getPeriodSummaryUseCase = mockk()
        getTransactionsUseCase = mockk()
        getAccountsUseCase = mockk()
        observeExchangeRateUseCase = mockk()
        userPreferences = mockk()
        rangeResolver = mockk()
        statisticsCurrencyDisplayResolver = mockk()

        // Default stub: any period + any anchorMillis
        every { rangeResolver(StatsPeriod.WEEK, any()) } returns weekRange
        every { rangeResolver(StatsPeriod.MONTH, null) } returns defaultMonthRange
        every { rangeResolver(StatsPeriod.YEAR, any()) } returns yearRange

        every { getPeriodSummaryUseCase(any(), any()) } returns flowOf(emptySummary(defaultMonthRange))
        every { getTransactionsUseCase() } returns flowOf(emptyList())
        every { getTransactionsUseCase(any(), any()) } returns flowOf(emptyList())
        every { getAccountsUseCase() } returns flowOf(emptyList())
        every { observeExchangeRateUseCase() } returns flowOf(null)
        every { userPreferences.baseCurrency } returns flowOf("KZT")
        every { userPreferences.languageCode } returns flowOf("en")
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
                displayedDailyExpenses = emptyList(),
                displayedDailyIncome = emptyList(),
                displayedMonthlyExpenses = emptyList(),
                displayedMonthlyIncome = emptyList(),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `setMonth with non-null YearMonth updates selectedMonth and reloads with anchorMillis`() =
        runTest(testDispatcher) {
            val janAnchorMillis = YearMonth.of(2026, 1)
                .atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            every { rangeResolver(StatsPeriod.MONTH, janAnchorMillis) } returns janAnchoredRange
            every { getPeriodSummaryUseCase(StatsPeriod.MONTH, janAnchorMillis) } returns
                flowOf(emptySummary(janAnchoredRange))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMonth(YearMonth.of(2026, 1))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(YearMonth.of(2026, 1), state.selectedMonth)
            assertEquals(janAnchoredRange, state.dateRange)

            verify { getPeriodSummaryUseCase(StatsPeriod.MONTH, janAnchorMillis) }
        }

    @Test
    fun `setPeriod clears selectedMonth to null`() = runTest(testDispatcher) {
        val janAnchorMillis = YearMonth.of(2026, 1)
            .atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        every { rangeResolver(StatsPeriod.MONTH, janAnchorMillis) } returns janAnchoredRange
        every { getPeriodSummaryUseCase(StatsPeriod.MONTH, janAnchorMillis) } returns
            flowOf(emptySummary(janAnchoredRange))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setMonth(YearMonth.of(2026, 1))
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 1), viewModel.state.value.selectedMonth)

        viewModel.setPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedMonth)
    }

    @Test
    fun `setMonth with null clears selectedMonth and reloads with default range`() =
        runTest(testDispatcher) {
            val janAnchorMillis = YearMonth.of(2026, 1)
                .atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            every { rangeResolver(StatsPeriod.MONTH, janAnchorMillis) } returns janAnchoredRange
            every { getPeriodSummaryUseCase(StatsPeriod.MONTH, janAnchorMillis) } returns
                flowOf(emptySummary(janAnchoredRange))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMonth(YearMonth.of(2026, 1))
            advanceUntilIdle()

            assertEquals(YearMonth.of(2026, 1), viewModel.state.value.selectedMonth)
            assertEquals(janAnchoredRange, viewModel.state.value.dateRange)

            viewModel.setMonth(null)
            advanceUntilIdle()

            assertNull(viewModel.state.value.selectedMonth)
            assertEquals(defaultMonthRange, viewModel.state.value.dateRange)

            verify { getPeriodSummaryUseCase(StatsPeriod.MONTH, null) }
        }

    private fun createViewModel() = StatisticsViewModel(
        getPeriodSummaryUseCase = getPeriodSummaryUseCase,
        getTransactionsUseCase = getTransactionsUseCase,
        getAccountsUseCase = getAccountsUseCase,
        observeExchangeRateUseCase = observeExchangeRateUseCase,
        userPreferences = userPreferences,
        rangeResolver = rangeResolver,
        statisticsCurrencyDisplayResolver = statisticsCurrencyDisplayResolver,
        defaultDispatcher = testDispatcher,
    )

    private fun emptySummary(dateRange: StatisticsDateRange) = PeriodSummary(
        dateRange = dateRange,
        totalExpenses = 0.0,
        totalIncome = 0.0,
        expensesByCategory = emptyList(),
        incomesByCategory = emptyList(),
        dailyExpenses = emptyList(),
        dailyIncome = emptyList(),
        monthlyExpenses = emptyList(),
        monthlyIncome = emptyList(),
    )
}
