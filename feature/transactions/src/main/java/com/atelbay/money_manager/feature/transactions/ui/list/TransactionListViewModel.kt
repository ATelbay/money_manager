package com.atelbay.money_manager.feature.transactions.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.feature.transactions.domain.usecase.DeleteTransactionUseCase
import com.atelbay.money_manager.feature.transactions.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    accountDao: AccountDao,
    userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    init {
        combine(
            getTransactionsUseCase(),
            accountDao.observeAll(),
            userPreferences.selectedAccountId,
        ) { transactions, accounts, selectedAccountId ->
            val selectedAccount = selectedAccountId?.let { id ->
                accounts.find { it.id == id }
            }
            val filteredTransactions = if (selectedAccount != null) {
                transactions.filter { it.accountId == selectedAccount.id }
            } else {
                transactions
            }
            val balance = if (selectedAccount != null) {
                selectedAccount.balance
            } else {
                accounts.sumOf { it.balance }
            }
            val currency = if (selectedAccount != null) {
                selectedAccount.currency
            } else {
                accounts.firstOrNull()?.currency.orEmpty()
            }
            _state.update {
                it.copy(
                    transactions = filteredTransactions.toImmutableList(),
                    balance = balance,
                    currency = currency,
                    isLoading = false,
                    selectedAccountName = selectedAccount?.name,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            deleteTransactionUseCase(id)
        }
    }
}
