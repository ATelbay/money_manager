package com.atelbay.money_manager.presentation.transactions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.usecase.ConversionDirection
import com.atelbay.money_manager.domain.exchangerate.usecase.ConvertAmountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.GetUsdKztRateUseCase
import com.atelbay.money_manager.domain.transactions.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.domain.transactions.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private data class FilterParams(
    val tab: TransactionType?,
    val period: Period,
    val customRange: Pair<LocalDate, LocalDate>?,
    val searchQuery: String,
)

private data class DataParams(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val selectedAccountId: Long?,
    val baseCurrency: String,
    val exchangeRate: ExchangeRate?,
)

private data class SummaryMetrics(
    val balance: Double,
    val income: Double,
    val expense: Double,
    val displayCurrency: String,
    val isUsingConvertedTotals: Boolean,
    val isUsingFallbackCurrency: Boolean,
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
    getUsdKztRateUseCase: GetUsdKztRateUseCase,
    private val convertAmountUseCase: ConvertAmountUseCase,
    userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow<TransactionType?>(null)
    private val _selectedPeriod = MutableStateFlow(Period.ALL)
    private val _customDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        val dataFlow = combine(
            getTransactionsUseCase(),
            getAccountsUseCase(),
            userPreferences.selectedAccountId,
            userPreferences.baseCurrency,
            getUsdKztRateUseCase(),
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
            _customDateRange,
            _searchQuery.debounce(300),
        ) { tab, period, customRange, query ->
            FilterParams(tab, period, customRange, query)
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

            val periodFiltered = if (filters.period == Period.ALL) {
                accountFiltered
            } else {
                val (rangeStart, rangeEnd) = periodToRange(filters.period, filters.customRange)
                val startMillis =
                    rangeStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis =
                    rangeEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()
                accountFiltered.filter { it.date in startMillis until endMillis }
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

            val currency = if (selectedAccount != null) {
                selectedAccount.currency
            } else {
                data.accounts.firstOrNull()?.currency.orEmpty()
            }
            val transactionRows = mapTransactionRows(
                transactions = searchFiltered,
                accounts = data.accounts,
                baseCurrency = data.baseCurrency,
                exchangeRate = data.exchangeRate,
            )
            val summaryMetrics = resolveSummaryMetrics(
                selectedAccount = selectedAccount,
                accounts = data.accounts,
                periodTransactions = periodFiltered,
                accountCurrencyFallback = currency,
                baseCurrency = data.baseCurrency,
                exchangeRate = data.exchangeRate,
            )

            _state.update {
                it.copy(
                    transactions = searchFiltered.toImmutableList(),
                    transactionRows = transactionRows,
                    searchQuery = filters.searchQuery,
                    balance = summaryMetrics.balance,
                    currency = currency,
                    displayCurrency = summaryMetrics.displayCurrency,
                    isUsingConvertedTotals = summaryMetrics.isUsingConvertedTotals,
                    isUsingFallbackCurrency = summaryMetrics.isUsingFallbackCurrency,
                    isLoading = false,
                    selectedAccountName = selectedAccount?.name,
                    selectedTab = filters.tab,
                    selectedPeriod = filters.period,
                    customDateRange = filters.customRange,
                    periodIncome = summaryMetrics.income,
                    periodExpense = summaryMetrics.expense,
                )
            }
        }.launchIn(viewModelScope)
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

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        _customDateRange.value = start to end
        _selectedPeriod.value = Period.CUSTOM
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }

    private fun mapTransactionRows(
        transactions: List<Transaction>,
        accounts: List<Account>,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): ImmutableList<TransactionRowState> {
        val currenciesByAccountId = accounts.associateBy(Account::id)
        val normalizedBaseCurrency = normalizeCurrency(baseCurrency)

        return transactions.map { transaction ->
            val accountCurrency = currenciesByAccountId[transaction.accountId]?.currency
            val originalCurrency = accountCurrency?.let(::normalizeCurrency).orEmpty()
            val convertedResult = accountCurrency?.let {
                convertToBaseCurrency(
                    amount = transaction.amount,
                    sourceCurrency = it,
                    baseCurrency = normalizedBaseCurrency,
                    exchangeRate = exchangeRate,
                )
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
            )
        }.toImmutableList()
    }

    private fun periodToRange(
        period: Period,
        customRange: Pair<LocalDate, LocalDate>?,
    ): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            Period.ALL -> LocalDate.of(2000, 1, 1) to today
            Period.TODAY -> today to today
            Period.WEEK -> today.minusDays(6) to today
            Period.MONTH -> today.minusDays(29) to today
            Period.YEAR -> today.minusYears(1).plusDays(1) to today
            Period.CUSTOM -> customRange ?: (today.minusDays(29) to today)
        }
    }

    private fun resolveSummaryMetrics(
        selectedAccount: Account?,
        accounts: List<Account>,
        periodTransactions: List<Transaction>,
        accountCurrencyFallback: String,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): SummaryMetrics {
        val normalizedBaseCurrency = normalizeCurrency(baseCurrency)
        val canDisplayInBaseCurrency = canDisplayInBaseCurrency(
            selectedAccount = selectedAccount,
            accounts = accounts,
            transactions = periodTransactions,
            baseCurrency = normalizedBaseCurrency,
            exchangeRate = exchangeRate,
        )

        if (!canDisplayInBaseCurrency) {
            val fallbackBalance = if (selectedAccount != null) {
                selectedAccount.balance
            } else {
                accounts.sumOf { it.balance }
            }
            val fallbackIncome =
                periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val fallbackExpense =
                periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            return SummaryMetrics(
                balance = fallbackBalance,
                income = fallbackIncome,
                expense = fallbackExpense,
                displayCurrency = accountCurrencyFallback.ifBlank { normalizedBaseCurrency },
                isUsingConvertedTotals = false,
                isUsingFallbackCurrency = true,
            )
        }

        val accountCurrenciesById = accounts.associateBy(Account::id)
        val scopedAccounts = if (selectedAccount != null) listOf(selectedAccount) else accounts
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

        return SummaryMetrics(
            balance = convertedBalances.sumOf { it.amount },
            income = convertedIncome,
            expense = convertedExpense,
            displayCurrency = normalizedBaseCurrency,
            isUsingConvertedTotals = convertedBalances.any { it.wasConverted } ||
                convertedTransactions.any { (convertedAmount, _) -> convertedAmount.wasConverted },
            isUsingFallbackCurrency = false,
        )
    }

    private fun canDisplayInBaseCurrency(
        selectedAccount: Account?,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRate: ExchangeRate?,
    ): Boolean {
        val scopedAccounts = if (selectedAccount != null) listOf(selectedAccount) else accounts
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

        val rate = exchangeRate?.usdToKzt
        return when {
            normalizedSourceCurrency == KZT && baseCurrency == USD && rate != null && rate > 0.0 ->
                ConvertedAmount(
                    amount = convertAmountUseCase(
                        amount = amount,
                        rate = rate,
                        direction = ConversionDirection.KZT_TO_USD,
                    ),
                    wasConverted = true,
                    canDisplayInBaseCurrency = true,
                )

            normalizedSourceCurrency == USD && baseCurrency == KZT && rate != null && rate > 0.0 ->
                ConvertedAmount(
                    amount = convertAmountUseCase(
                        amount = amount,
                        rate = rate,
                        direction = ConversionDirection.USD_TO_KZT,
                    ),
                    wasConverted = true,
                    canDisplayInBaseCurrency = true,
                )

            else ->
                ConvertedAmount(
                    amount = amount,
                    wasConverted = false,
                    canDisplayInBaseCurrency = false,
                )
        }
    }

    private fun normalizeCurrency(currency: String): String {
        return currency.trim().uppercase().ifBlank { KZT }
    }

    private companion object {
        const val KZT = "KZT"
        const val USD = "USD"
    }
}
