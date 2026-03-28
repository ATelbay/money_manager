package com.atelbay.money_manager.presentation.transactions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayMode
import com.atelbay.money_manager.core.ui.util.AggregateCurrencyDisplayResolver
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import com.atelbay.money_manager.core.ui.util.normalizeCurrencyCode
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import com.atelbay.money_manager.domain.transactions.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import com.atelbay.money_manager.core.common.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private data class CustomDateRange(val startMillis: Long, val endMillis: Long)

private data class FilterParams(
    val tab: TransactionType?,
    val period: Period,
    val searchQuery: String,
    val customDateRange: CustomDateRange?,
)

private data class DataParams(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val selectedAccountId: Long?,
    val baseCurrency: String,
    val exchangeRate: ExchangeRate?,
)

private data class SummaryMetrics(
    val balance: Double?,
    val income: Double?,
    val expense: Double?,
    val moneyDisplay: MoneyDisplayPresentation,
)

private data class ConvertedAmount(
    val amount: Double,
    val wasConverted: Boolean,
    val canDisplayInBaseCurrency: Boolean,
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    getAccountsUseCase: GetAccountsUseCase,
    observeExchangeRateUseCase: ObserveExchangeRateUseCase,
    private val convertAmountUseCase: ConvertAmountUseCase,
    private val userPreferences: UserPreferences,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow<TransactionType?>(null)
    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    private val _searchQuery = MutableStateFlow("")
    private val _customDateRange = MutableStateFlow<CustomDateRange?>(null)

    init {
        val dataFlow = combine(
            getTransactionsUseCase(),
            getAccountsUseCase(),
            userPreferences.selectedAccountId,
            userPreferences.baseCurrency,
            observeExchangeRateUseCase(),
        ) { transactions, accounts, selectedAccountId, baseCurrency, exchangeRate ->
            DataParams(
                transactions = transactions,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                baseCurrency = baseCurrency,
                exchangeRate = exchangeRate,
            )
        }

        @OptIn(FlowPreview::class)
        val filterFlow = combine(
            _selectedTab,
            _selectedPeriod,
            _searchQuery.debounce(300),
            _customDateRange,
        ) { tab, period, query, customRange ->
            FilterParams(tab, period, query, customRange)
        }

        combine(dataFlow, filterFlow) { data, filters ->
            val selectedAccount = data.selectedAccountId?.let { id ->
                data.accounts.find { it.id == id }
            }

            val accountFiltered = if (selectedAccount != null) {
                data.transactions.filter { it.accountId == selectedAccount.id }
            } else {
                data.transactions
            }

            val periodFiltered = when {
                filters.period == Period.ALL -> accountFiltered
                filters.period == Period.CUSTOM && filters.customDateRange != null -> {
                    val startMillis = filters.customDateRange.startMillis
                    val endMillis = filters.customDateRange.endMillis
                    accountFiltered.filter { it.date in startMillis..endMillis }
                }
                else -> {
                    val (rangeStart, rangeEnd) = periodToRange(filters.period)
                    val startMillis =
                        rangeStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis =
                        rangeEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli()
                    accountFiltered.filter { it.date in startMillis until endMillis }
                }
            }

            val tabFiltered = if (filters.tab != null) {
                periodFiltered.filter { it.type == filters.tab }
            } else {
                periodFiltered
            }

            val searchFiltered = if (filters.searchQuery.isBlank()) {
                tabFiltered
            } else {
                val q = filters.searchQuery.lowercase()
                tabFiltered.filter { t ->
                    t.categoryName.lowercase().contains(q) ||
                            t.note?.lowercase()?.contains(q) == true
                }
            }

            val normalizedBase = normalizeCurrency(data.baseCurrency)
            val canConvertAll = canDisplayInBaseCurrency(
                selectedAccount = selectedAccount,
                accounts = data.accounts,
                transactions = periodFiltered,
                baseCurrency = normalizedBase,
                exchangeRate = data.exchangeRate,
            )

            val transactionRows = mapTransactionRows(
                transactions = searchFiltered,
                accounts = data.accounts,
                baseCurrency = data.baseCurrency,
                exchangeRate = data.exchangeRate,
                canConvertAll = canConvertAll,
            )
            val summaryMetrics = resolveSummaryMetrics(
                selectedAccount = selectedAccount,
                accounts = data.accounts,
                periodTransactions = periodFiltered,
                baseCurrency = data.baseCurrency,
                exchangeRate = data.exchangeRate,
                canConvertAll = canConvertAll,
            )

            // Compute daily net sums only when all amounts are in the same currency
            val dailyNetSums = if (canConvertAll) {
                computeDailyNetSums(transactionRows)
            } else {
                emptyMap()
            }

            _state.update {
                it.copy(
                    transactionRows = transactionRows,
                    searchQuery = filters.searchQuery,
                    balance = summaryMetrics.balance,
                    summaryMoneyDisplay = summaryMetrics.moneyDisplay,
                    isLoading = false,
                    selectedAccountName = selectedAccount?.name,
                    selectedAccountId = data.selectedAccountId,
                    selectedTab = filters.tab,
                    selectedPeriod = filters.period,
                    periodIncome = summaryMetrics.income,
                    periodExpense = summaryMetrics.expense,
                    accounts = data.accounts.toImmutableList(),
                    dailyNetSums = dailyNetSums,
                    // Preserve UI-driven flags so fast combine emissions don't overwrite them.
                    showDatePickerDialog = it.showDatePickerDialog,
                    customDateRangeStart = filters.customDateRange?.startMillis,
                    customDateRangeEnd = filters.customDateRange?.endMillis,
                )
            }
        }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun selectTab(type: TransactionType?) {
        _selectedTab.value = type
    }

    fun selectPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }

    fun selectAccount(accountId: Long?) {
        viewModelScope.launch {
            userPreferences.setSelectedAccountId(accountId)
        }
    }

    fun toggleAccountPicker() {
        _state.update { it.copy(showAccountPicker = !it.showAccountPicker) }
    }

    fun dismissAccountPicker() {
        _state.update { it.copy(showAccountPicker = false) }
    }

    fun toggleDatePickerDialog() {
        _state.update { it.copy(showDatePickerDialog = !it.showDatePickerDialog) }
    }

    /**
     * Sets a custom date range filter.
     *
     * [startMillis] and [endMillis] are UTC-midnight timestamps as returned by
     * Material [DateRangePicker]. [endMillis] is adjusted to end-of-day
     * (23:59:59.999) so transactions on the last selected day are included.
     */
    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val adjustedEnd = endMillis + 86_400_000L - 1L  // end of day (23:59:59.999)
        _customDateRange.value = CustomDateRange(startMillis, adjustedEnd)
        _selectedPeriod.value = Period.CUSTOM
        _state.update { it.copy(showDatePickerDialog = false) }
    }

    fun setCustomMonth(year: Int, month: Int) {
        val startLocal = LocalDate.of(year, month, 1)
        val endLocal = startLocal.withDayOfMonth(startLocal.lengthOfMonth())
        val zone = ZoneId.systemDefault()
        val startMillis = startLocal.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endLocal.atStartOfDay(zone).toInstant().toEpochMilli() + 86_400_000L - 1L
        _customDateRange.value = CustomDateRange(startMillis, endMillis)
        _selectedPeriod.value = Period.CUSTOM
        _state.update { it.copy(showDatePickerDialog = false) }
    }

    private fun computeDailyNetSums(
        transactionRows: ImmutableList<TransactionRowState>,
    ): Map<String, Double> {
        val zone = ZoneId.systemDefault()
        return transactionRows
            .groupBy { row ->
                Instant.ofEpochMilli(row.transaction.date)
                    .atZone(zone)
                    .toLocalDate()
                    .toString()
            }
            .mapValues { (_, rows) ->
                rows.sumOf { row ->
                    val amount = row.displayAmount
                    if (row.transaction.type == TransactionType.INCOME) amount else -amount
                }
            }
    }

    private fun mapTransactionRows(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
        canConvertAll: Boolean,
    ): ImmutableList<TransactionRowState> {
        val currenciesByAccountId = accounts.associateBy(Account::id)
        val normalizedBaseCurrency = normalizeCurrency(baseCurrency)

        return transactions.map { transaction ->
            val accountCurrency = currenciesByAccountId[transaction.accountId]?.currency
            val originalCurrency = accountCurrency?.let(::normalizeCurrency).orEmpty()

            val convertedResult = if (canConvertAll) {
                accountCurrency?.let {
                    convertToBaseCurrency(
                        amount = transaction.amount,
                        sourceCurrency = it,
                        baseCurrency = normalizedBaseCurrency,
                        exchangeRate = exchangeRate,
                    )
                }
            } else {
                null
            }
            val hasConvertedAmount = convertedResult?.let {
                it.canDisplayInBaseCurrency && it.wasConverted
            } == true

            TransactionRowState(
                transaction = transaction,
                originalAmount = transaction.amount,
                originalCurrency = originalCurrency,
                convertedAmount = convertedResult
                    ?.takeIf { hasConvertedAmount }
                    ?.amount,
                convertedCurrency = normalizedBaseCurrency.takeIf { hasConvertedAmount },
                conversionStatus = if (hasConvertedAmount) {
                    ConversionStatus.AVAILABLE
                } else {
                    ConversionStatus.UNAVAILABLE
                },
                displayMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat(
                    normalizedBaseCurrency.takeIf { hasConvertedAmount } ?: originalCurrency,
                ),
                secondaryMoneyDisplay = originalCurrency
                    .takeIf { hasConvertedAmount }
                    ?.let(MoneyDisplayFormatter::resolveAndFormat),
            )
        }.toImmutableList()
    }

    private fun periodToRange(period: Period): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            Period.ALL -> LocalDate.of(2000, 1, 1) to today
            Period.TODAY -> today to today
            Period.WEEK -> today.minusDays(6) to today
            Period.MONTH -> today.minusDays(29) to today
            Period.YEAR -> today.minusYears(1).plusDays(1) to today
            // CUSTOM range is handled separately via _customDateRange millis; fall back to today.
            Period.CUSTOM -> today to today
        }
    }

    private fun resolveSummaryMetrics(
        selectedAccount: Account?,
        accounts: List<Account>,
        periodTransactions: List<Transaction>,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
        canConvertAll: Boolean,
    ): SummaryMetrics {
        val normalizedBaseCurrency = normalizeCurrency(baseCurrency)
        val accountCurrenciesById = accounts.associateBy(Account::id)
        val scopedAccounts = if (selectedAccount != null) listOf(selectedAccount) else accounts
        val scopedCurrencies = buildSet {
            scopedAccounts
                .map { normalizeCurrency(it.currency) }
                .filterTo(this) { it.isNotBlank() }
            periodTransactions
                .mapNotNull { accountCurrenciesById[it.accountId]?.currency }
                .map(::normalizeCurrency)
                .filterTo(this) { it.isNotBlank() }
        }
        val displayResolution = AggregateCurrencyDisplayResolver.resolve(
            baseCurrency = normalizedBaseCurrency,
            scopedCurrencies = scopedCurrencies,
            canDisplayInBaseCurrency = canConvertAll,
        )

        return when (displayResolution.displayMode) {
            AggregateCurrencyDisplayMode.UNAVAILABLE -> {
                SummaryMetrics(
                    balance = null,
                    income = null,
                    expense = null,
                    moneyDisplay = MoneyDisplayFormatter.format(
                        MoneyDisplayFormatter.unavailable(),
                    ),
                )
            }
            AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY -> {
                val fallbackBalance = scopedAccounts.sumOf { it.balance }
                val fallbackIncome = periodTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val fallbackExpense = periodTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                SummaryMetrics(
                    balance = fallbackBalance,
                    income = fallbackIncome,
                    expense = fallbackExpense,
                    moneyDisplay = displayResolution.displayCurrency
                        ?.let(MoneyDisplayFormatter::resolveAndFormat)
                        ?: MoneyDisplayFormatter.format(MoneyDisplayFormatter.unavailable()),
                )
            }
            AggregateCurrencyDisplayMode.CONVERTED -> {
                val convertedBalances = scopedAccounts.map {
                    convertToBaseCurrency(
                        amount = it.balance,
                        sourceCurrency = it.currency,
                        baseCurrency = normalizedBaseCurrency,
                        exchangeRate = exchangeRate,
                    )
                }
                val convertedTransactions = periodTransactions.mapNotNull { transaction ->
                    val sourceCurrency = accountCurrenciesById[transaction.accountId]?.currency
                        ?: return@mapNotNull null

                    convertToBaseCurrency(
                        amount = transaction.amount,
                        sourceCurrency = sourceCurrency,
                        baseCurrency = normalizedBaseCurrency,
                        exchangeRate = exchangeRate,
                    ) to transaction.type
                }

                val convertedIncome = convertedTransactions
                    .filter { (_, type) -> type == TransactionType.INCOME }
                    .sumOf { (convertedAmount, _) -> convertedAmount.amount }
                val convertedExpense = convertedTransactions
                    .filter { (_, type) -> type == TransactionType.EXPENSE }
                    .sumOf { (convertedAmount, _) -> convertedAmount.amount }

                SummaryMetrics(
                    balance = convertedBalances.sumOf { it.amount },
                    income = convertedIncome,
                    expense = convertedExpense,
                    moneyDisplay = displayResolution.displayCurrency
                        ?.let(MoneyDisplayFormatter::resolveAndFormat)
                        ?: MoneyDisplayFormatter.format(MoneyDisplayFormatter.unavailable()),
                )
            }
        }
    }

    private fun canDisplayInBaseCurrency(
        selectedAccount: Account?,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): Boolean {
        val scopedAccounts = if (selectedAccount != null) listOf(selectedAccount) else accounts
        if (scopedAccounts.isEmpty() && transactions.isEmpty()) {
            return true
        }
        if (scopedAccounts.any {
                !convertToBaseCurrency(
                    amount = it.balance,
                    sourceCurrency = it.currency,
                    baseCurrency = baseCurrency,
                    exchangeRate = exchangeRate,
                ).canDisplayInBaseCurrency
            }
        ) {
            return false
        }

        val currenciesByAccountId = accounts.associateBy(Account::id)
        return transactions.all { transaction ->
            val sourceCurrency = currenciesByAccountId[transaction.accountId]?.currency ?: return@all false
            convertToBaseCurrency(
                amount = transaction.amount,
                sourceCurrency = sourceCurrency,
                baseCurrency = baseCurrency,
                exchangeRate = exchangeRate,
            ).canDisplayInBaseCurrency
        }
    }

    private fun convertToBaseCurrency(
        amount: Double,
        sourceCurrency: String,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): ConvertedAmount {
        val normalizedSourceCurrency = normalizeCurrency(sourceCurrency)
        if (normalizedSourceCurrency == baseCurrency) {
            return ConvertedAmount(
                amount = amount,
                wasConverted = false,
                canDisplayInBaseCurrency = true,
            )
        }

        val quotes = exchangeRate?.quotes
            ?: return ConvertedAmount(amount, wasConverted = false, canDisplayInBaseCurrency = false)

        return try {
            ConvertedAmount(
                amount = convertAmountUseCase(
                    amount = amount,
                    sourceCurrency = normalizedSourceCurrency,
                    targetCurrency = baseCurrency,
                    quotes = quotes,
                ),
                wasConverted = true,
                canDisplayInBaseCurrency = true,
            )
        } catch (_: IllegalArgumentException) {
            ConvertedAmount(
                amount = amount,
                wasConverted = false,
                canDisplayInBaseCurrency = false,
            )
        }
    }

    private fun normalizeCurrency(currency: String): String {
        return currency.normalizeCurrencyCode(fallback = KZT)
    }

    private companion object {
        const val KZT = "KZT"
    }
}
