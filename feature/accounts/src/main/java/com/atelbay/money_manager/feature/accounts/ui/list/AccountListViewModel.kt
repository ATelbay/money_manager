package com.atelbay.money_manager.feature.accounts.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.feature.accounts.domain.usecase.DeleteAccountUseCase
import com.atelbay.money_manager.feature.accounts.domain.usecase.GetAccountsUseCase
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
class AccountListViewModel @Inject constructor(
    getAccountsUseCase: GetAccountsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountListState())
    val state: StateFlow<AccountListState> = _state.asStateFlow()

    init {
        combine(
            getAccountsUseCase(),
            userPreferences.selectedAccountId,
        ) { accounts, selectedId ->
            _state.update {
                it.copy(
                    accounts = accounts.toImmutableList(),
                    selectedAccountId = selectedId,
                    totalBalance = accounts.sumOf { a -> a.balance },
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun selectAccount(id: Long?) {
        viewModelScope.launch {
            userPreferences.setSelectedAccountId(id)
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            // If deleting the selected account, clear selection
            if (_state.value.selectedAccountId == id) {
                userPreferences.setSelectedAccountId(null)
            }
            deleteAccountUseCase(id)
        }
    }
}
