package com.atelbay.money_manager.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.database.dao.AccountDao
import com.atelbay.money_manager.core.database.entity.AccountEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setPage(page: Int) {
        _state.update { it.copy(currentPage = page) }
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
            accountDao.insert(
                AccountEntity(
                    name = name,
                    currency = current.currency,
                    balance = balance,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            userPreferences.setOnboardingCompleted()
            onComplete()
        }
    }
}
