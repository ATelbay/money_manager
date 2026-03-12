package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.common.buildSortedCurrencyList
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.domain.accounts.usecase.SaveAccountUseCase
import com.atelbay.money_manager.domain.exchangerate.usecase.ObserveExchangeRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val saveAccountUseCase: SaveAccountUseCase,
    private val userPreferences: UserPreferences,
    observeExchangeRateUseCase: ObserveExchangeRateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateAccountState())
    val state: StateFlow<CreateAccountState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rate = observeExchangeRateUseCase().first()
            if (rate != null) {
                val sorted = buildSortedCurrencyList(rate.quotes.keys)
                _state.update { it.copy(availableCurrencies = sorted) }
            }
        }
    }

    fun setAccountName(name: String) {
        _state.update { it.copy(accountName = name, accountNameError = null) }
    }

    fun setCurrency(currency: String) {
        _state.update { it.copy(currency = currency) }
    }

    fun setInitialBalance(balance: String) {
        val filtered = balance.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(initialBalance = filtered, balanceError = null) }
    }

    fun createAccount(onComplete: () -> Unit) {
        val current = _state.value
        val name = current.accountName.trim()
        val balanceText = current.initialBalance.trim()

        var hasError = false
        if (name.isBlank()) {
            _state.update { it.copy(accountNameError = "Введите название счёта") }
            hasError = true
        }

        val balance = if (balanceText.isEmpty()) {
            0.0
        } else {
            balanceText.toDoubleOrNull() ?: run {
                _state.update { it.copy(balanceError = "Некорректная сумма") }
                hasError = true
                0.0
            }
        }

        if (hasError) return

        _state.update { it.copy(isCreating = true) }

        viewModelScope.launch {
            saveAccountUseCase(
                Account(
                    name = name,
                    currency = current.currency,
                    balance = balance,
                ),
            )
            userPreferences.setOnboardingCompleted()
            onComplete()
        }
    }
}
