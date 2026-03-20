package com.atelbay.money_manager.presentation.accounts.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.common.buildSortedCurrencyList
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.ui.theme.AppStrings
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
import kotlin.coroutines.cancellation.CancellationException
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
                            originalBalance = account.balance,
                            originalCreatedAt = account.createdAt,
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

    fun save(strings: AppStrings, onComplete: () -> Unit) {
        val current = _state.value

        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = strings.errorEnterAccountName) }
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val isEditing = current.accountId != null
                saveAccountUseCase(
                    Account(
                        id = current.accountId ?: 0,
                        name = current.name.trim(),
                        currency = current.currency,
                        balance = if (isEditing) current.originalBalance else 0.0,
                        createdAt = if (isEditing) current.originalCreatedAt else System.currentTimeMillis(),
                    ),
                )
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, nameError = strings.errorUnknown) }
            }
        }
    }
}
