package com.atelbay.money_manager.feature.transactions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.transactions.domain.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.feature.transactions.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val accounts: List<AccountEntity>,
    val selectedAccountId: Long?,
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    accountDao: AccountDao,
    userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow<TransactionType?>(null)
    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    private val _customDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        val dataFlow = combine(
            getTransactionsUseCase(),
            accountDao.observeAll(),
            userPreferences.selectedAccountId,
        ) { transactions, accounts, selectedAccountId ->
            DataParams(transactions, accounts, selectedAccountId)
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

            val (rangeStart, rangeEnd) = periodToRange(filters.period, filters.customRange)
            val startMillis = rangeStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = rangeEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val periodFiltered = accountFiltered.filter { it.date in startMillis until endMillis }

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

            val periodIncome = periodFiltered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val periodExpense = periodFiltered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            val balance = if (selectedAccount != null) {
                selectedAccount.balance
            } else {
                data.accounts.sumOf { it.balance }
            }
            val currency = if (selectedAccount != null) {
                selectedAccount.currency
            } else {
                data.accounts.firstOrNull()?.currency.orEmpty()
            }

            _state.update {
                it.copy(
                    transactions = searchFiltered.toImmutableList(),
                    searchQuery = filters.searchQuery,
                    balance = balance,
                    currency = currency,
                    isLoading = false,
                    selectedAccountName = selectedAccount?.name,
                    selectedTab = filters.tab,
                    selectedPeriod = filters.period,
                    customDateRange = filters.customRange,
                    periodIncome = periodIncome,
                    periodExpense = periodExpense,
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

    private fun periodToRange(
        period: Period,
        customRange: Pair<LocalDate, LocalDate>?,
    ): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            Period.TODAY -> today to today
            Period.WEEK -> today.minusDays(6) to today
            Period.MONTH -> today.minusDays(29) to today
            Period.YEAR -> today.minusYears(1).plusDays(1) to today
            Period.CUSTOM -> customRange ?: (today.minusDays(29) to today)
        }
    }
}
