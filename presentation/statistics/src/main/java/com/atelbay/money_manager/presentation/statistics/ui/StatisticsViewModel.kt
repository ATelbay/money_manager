package com.atelbay.money_manager.presentation.statistics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.ui.theme.appStringsFor
import com.atelbay.money_manager.core.ui.util.MoneyDisplayMode
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.statistics.model.PeriodSummary
import com.atelbay.money_manager.domain.statistics.model.StatisticsDateRange
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import com.atelbay.money_manager.domain.statistics.usecase.GetPeriodSummaryUseCase
import com.atelbay.money_manager.domain.statistics.usecase.StatisticsPeriodRangeResolver
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import com.atelbay.money_manager.core.common.startOfDay
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.atelbay.money_manager.core.common.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getPeriodSummaryUseCase: GetPeriodSummaryUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val observeExchangeRateUseCase: ObserveExchangeRateUseCase,
    private val userPreferences: UserPreferences,
    private val rangeResolver: StatisticsPeriodRangeResolver,
    private val statisticsCurrencyDisplayResolver: StatisticsCurrencyDisplayResolver,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private var cachedLanguageCode: String = Locale.getDefault().language

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    val chartModelProducer = CartesianChartModelProducer()

    private var summaryJob: Job? = null
    private var chartUpdateJob: Job? = null
    private var visibleMaxY: Double = 0.0

    init {
        userPreferences.languageCode
            .onEach { newCode ->
                val changed = cachedLanguageCode != newCode
                cachedLanguageCode = newCode
                if (changed) {
                    val s = _state.value
                    val anchorMillis = s.selectedMonth?.let {
                        it.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    loadSummary(s.period, anchorMillis)
                }
            }
            .launchIn(viewModelScope)
        loadSummary(_state.value.period)
    }

    fun setPeriod(period: StatsPeriod) {
        if (_state.value.period == period) return
        _state.update { it.copy(selectedMonth = null) }
        loadSummary(period)
    }

    fun setMonth(yearMonth: YearMonth?) {
        if (yearMonth != null) {
            val anchorMillis = yearMonth.atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            _state.update { it.copy(selectedMonth = yearMonth) }
            loadSummary(_state.value.period, anchorMillis)
        } else {
            _state.update { it.copy(selectedMonth = null) }
            loadSummary(_state.value.period)
        }
    }

    fun setTransactionType(type: TransactionType) {
        if (_state.value.transactionType == type) return
        _state.update { current ->
            current.copy(transactionType = type).withChartContract()
        }
        chartUpdateJob?.cancel()
        chartUpdateJob = viewModelScope.launch { updateChartModel() }
    }

    fun onVisibleMaxChanged(maxY: Double) {
        if (maxY == visibleMaxY) return
        visibleMaxY = maxY
        chartUpdateJob?.cancel()
        chartUpdateJob = viewModelScope.launch { updateChartModel() }
    }

    fun retry() {
        val state = _state.value
        val anchorMillis = state.selectedMonth?.let {
            it.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        loadSummary(state.period, anchorMillis)
    }

    fun refreshCurrentPeriod() {
        val state = _state.value
        val anchorMillis = state.selectedMonth?.let {
            it.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val currentRange = state.dateRange
        val latestRange = rangeResolver(state.period, anchorMillis)
        if (currentRange != latestRange) {
            loadSummary(state.period, anchorMillis)
        }
    }

    private fun loadSummary(period: StatsPeriod, anchorMillis: Long? = null) {
        val resolvedRange = rangeResolver(period, anchorMillis)
        _state.update { current ->
            current.copy(
                period = period,
                dateRange = resolvedRange,
                isLoading = true,
                error = null,
            ).withChartContract(dateRange = resolvedRange)
        }

        summaryJob?.cancel()
        summaryJob = combine(
            getPeriodSummaryUseCase(period, anchorMillis),
            getTransactionsUseCase(resolvedRange.startMillis, resolvedRange.endMillis),
            getAccountsUseCase(),
            userPreferences.baseCurrency,
            observeExchangeRateUseCase(),
        ) { summary, transactions, accounts, baseCurrency, exchangeRate ->
            StatisticsSnapshot(
                summary = summary,
                transactions = transactions,
                accounts = accounts,
                baseCurrency = baseCurrency,
                exchangeRate = exchangeRate,
            )
        }
            .distinctUntilChanged()
            .onEach { snapshot ->
                val currencyResolution = statisticsCurrencyDisplayResolver.resolve(
                    summary = snapshot.summary,
                    transactions = snapshot.transactions,
                    accounts = snapshot.accounts,
                    baseCurrency = snapshot.baseCurrency,
                    exchangeRate = snapshot.exchangeRate,
                )

                _state.update { current ->
                    current.copy(
                        dateRange = snapshot.summary.dateRange,
                        displayedTotalExpenses = currencyResolution.displayedTotalExpenses,
                        displayedTotalIncome = currencyResolution.displayedTotalIncome,
                        displayedExpensesByCategory = currencyResolution.displayedExpensesByCategory.toImmutableList(),
                        displayedIncomesByCategory = currencyResolution.displayedIncomesByCategory.toImmutableList(),
                        displayedDailyExpenses = currencyResolution.displayedDailyExpenses.toImmutableList(),
                        displayedDailyIncome = currencyResolution.displayedDailyIncome.toImmutableList(),
                        displayedMonthlyExpenses = currencyResolution.displayedMonthlyExpenses.toImmutableList(),
                        displayedMonthlyIncome = currencyResolution.displayedMonthlyIncome.toImmutableList(),
                        currencyUiState = currencyResolution.currencyUiState,
                        isLoading = false,
                        error = null,
                    ).withChartContract(dateRange = snapshot.summary.dateRange)
                }
                chartUpdateJob?.cancel()
                chartUpdateJob = viewModelScope.launch { updateChartModel() }
            }
            .catch { e ->
                _state.update { current ->
                    current.copy(
                        error = e.message ?: "Unknown error",
                        isLoading = false,
                    ).withChartContract()
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    private fun createInitialState(): StatisticsState {
        val initialPeriod = StatsPeriod.MONTH
        val resolvedRange = rangeResolver(initialPeriod)
        return StatisticsState(
            period = initialPeriod,
            dateRange = resolvedRange,
        ).withChartContract(dateRange = resolvedRange)
    }

    private fun StatisticsState.withChartContract(
        dateRange: StatisticsDateRange? = this.dateRange,
    ): StatisticsState {
        val points = buildChartPoints(
            state = this,
            period = period,
            transactionType = transactionType,
            dateRange = dateRange,
        )

        return copy(
            chart = StatisticsChartState(
                points = points.toImmutableList(),
                isScrollable = true,
            ),
        )
    }

    private suspend fun updateChartModel() {
        val currentState = _state.value
        val points = currentState.chart.points
        if (points.isEmpty()) return

        val amounts: List<Double> = points.map { it.amount ?: 0.0 }
        val allZero = amounts.all { it == 0.0 }
        if (allZero) {
            _state.update { it.copy(chart = it.chart.copy(allAmountsZero = true)) }
            return
        }
        _state.update { it.copy(chart = it.chart.copy(allAmountsZero = false)) }

        val moneyDisplay = currentState.currencyUiState.moneyDisplay
        val period = currentState.period

        val indices: List<Number> = points.indices.map { it.toDouble() }
        val amountNumbers: List<Number> = amounts

        val dateFormat = when (period) {
            StatsPeriod.WEEK, StatsPeriod.MONTH -> SimpleDateFormat("MMM d", localizedStrings().locale)
            StatsPeriod.YEAR -> SimpleDateFormat("MMM yyyy", localizedStrings().locale)
        }

        chartModelProducer.runTransaction {
            columnSeries { series(x = indices, y = amountNumbers) }
            extras { store ->
                store[xToLabelMapKey] = points.mapIndexed { i, p ->
                    i.toDouble() to p.displayLabel
                }.toMap()
                store[xToDateStringKey] = points.mapIndexed { i, p ->
                    i.toDouble() to dateFormat.format(Date(p.bucketStartMillis))
                }.toMap()
                store[todayIndexKey] = points.indexOfFirst { it.isToday }
                store[currencySymbolKey] = moneyDisplay.primaryLabel
                store[currencyPrefixKey] = moneyDisplay.displayMode == MoneyDisplayMode.SYMBOL_FIRST
                store[visibleMaxYKey] = visibleMaxY
            }
        }
    }

    private fun buildChartPoints(
        state: StatisticsState,
        period: StatsPeriod,
        transactionType: TransactionType,
        dateRange: StatisticsDateRange?,
    ): List<StatisticsChartPoint> {
        val todayStart = dateRange?.endMillis?.let(::startOfDay)
        return when (period) {
            StatsPeriod.WEEK, StatsPeriod.MONTH -> {
                val totals = when (transactionType) {
                    TransactionType.EXPENSE -> state.displayedDailyExpenses
                    TransactionType.INCOME -> state.displayedDailyIncome
                }
                val labelPattern = if (period == StatsPeriod.WEEK) "EEE" else "d"
                val labelFormatter = SimpleDateFormat(labelPattern, localizedStrings().locale)
                totals.map { total ->
                    val bucketStart = startOfDay(total.date)
                    StatisticsChartPoint(
                        bucketStartMillis = bucketStart,
                        displayLabel = labelFormatter.format(Date(total.date)),
                        amount = total.amount,
                        isToday = todayStart != null && bucketStart == todayStart,
                    )
                }
            }

            StatsPeriod.YEAR -> {
                val totals = when (transactionType) {
                    TransactionType.EXPENSE -> state.displayedMonthlyExpenses
                    TransactionType.INCOME -> state.displayedMonthlyIncome
                }
                val monthLabelFormatter = SimpleDateFormat("MMM", localizedStrings().locale)
                totals.map { total ->
                    val cal = Calendar.getInstance(TimeZone.getDefault())
                    cal.set(Calendar.YEAR, total.year)
                    cal.set(Calendar.MONTH, total.month)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val raw = monthLabelFormatter.format(cal.time).removeSuffix(".").take(3)
                    StatisticsChartPoint(
                        bucketStartMillis = monthStart(total.year, total.month),
                        displayLabel = raw,
                        amount = total.amount,
                    )
                }
            }
        }
    }

    private fun localizedStrings() = appStringsFor(cachedLanguageCode)

    private fun monthStart(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private data class StatisticsSnapshot(
        val summary: PeriodSummary,
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val baseCurrency: String,
        val exchangeRate: ExchangeRate?,
    )
}
