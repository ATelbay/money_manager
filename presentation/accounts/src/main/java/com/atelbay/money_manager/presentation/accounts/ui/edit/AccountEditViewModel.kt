package com.atelbay.money_manager.presentation.accounts.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.common.buildSortedCurrencyList
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountByIdUseCase
import com.atelbay.money_manager.domain.accounts.usecase.SaveAccountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccountByIdUseCase: GetAccountByIdUseCase,
    private val saveAccountUseCase: SaveAccountUseCase,
    observeExchangeRateUseCase: ObserveExchangeRateUseCase,
) : ViewModel() {

    private val accountId: Long? = savedStateHandle.get<Long>("id")

    private val _state = MutableStateFlow(AccountEditState(accountId = accountId))
    val state: StateFlow<AccountEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rate = observeExchangeRateUseCase().first()
            if (rate != null) {
                val sorted = buildSortedCurrencyList(rate.quotes.keys).toImmutableList()
                _state.update { it.copy(availableCurrencies = sorted) }
            }

            if (accountId != null) {
                val account = getAccountByIdUseCase(accountId).first()
                if (account != null) {
                    _state.update {
                        it.copy(
                            name = account.name,
                            currency = account.currency,
                            isLoading = false,
                        )
                    }
                    return@launch
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun setName(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun setCurrency(currency: String) {
        _state.update { it.copy(currency = currency) }
    }

    fun save(onComplete: () -> Unit) {
        val current = _state.value

        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = "Введите название счёта") }
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveAccountUseCase(
                Account(
                    id = current.accountId ?: 0,
                    name = current.name.trim(),
                    currency = current.currency,
                    balance = 0.0,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            onComplete()
        }
    }
}
