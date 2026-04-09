package com.atelbay.money_manager.presentation.debts.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtPayment
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.debts.usecase.AddDebtPaymentUseCase
import com.atelbay.money_manager.domain.debts.usecase.DeleteDebtPaymentUseCase
import com.atelbay.money_manager.domain.debts.usecase.DeleteDebtUseCase
import com.atelbay.money_manager.domain.debts.usecase.GetDebtWithPaymentsUseCase
import com.atelbay.money_manager.domain.debts.usecase.SaveDebtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDebtWithPaymentsUseCase: GetDebtWithPaymentsUseCase,
    private val addDebtPaymentUseCase: AddDebtPaymentUseCase,
    private val deleteDebtPaymentUseCase: DeleteDebtPaymentUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val saveDebtUseCase: SaveDebtUseCase,
    getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val debtId: Long = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(DebtDetailState())
    val state: StateFlow<DebtDetailState> = _state.asStateFlow()

    private val _accounts = MutableStateFlow<ImmutableList<Account>>(persistentListOf())
    val accounts: StateFlow<ImmutableList<Account>> = _accounts.asStateFlow()

    init {
        getDebtWithPaymentsUseCase(debtId)
            .onEach { (debt, payments) ->
                _state.update {
                    it.copy(
                        debt = debt,
                        payments = payments.sortedByDescending { p -> p.date }.toImmutableList(),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)

        getAccountsUseCase()
            .onEach { _accounts.value = it.toImmutableList() }
            .launchIn(viewModelScope)
    }

    fun addPayment(amount: Double, date: Long, note: String?, createTransaction: Boolean) {
        val debt = _state.value.debt ?: return
        _state.update { it.copy(showPaymentSheet = false) }
        viewModelScope.launch {
            val payment = DebtPayment(
                debtId = debtId,
                amount = amount,
                date = date,
                note = note?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
            )
            addDebtPaymentUseCase(payment, createTransaction, debt)
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch {
            deleteDebtPaymentUseCase(id)
        }
    }

    fun deleteDebt() {
        viewModelScope.launch {
            deleteDebtUseCase(debtId)
        }
    }

    fun saveDebt(debt: Debt) {
        viewModelScope.launch {
            saveDebtUseCase(debt)
        }
    }

    fun togglePaymentSheet() {
        _state.update { it.copy(showPaymentSheet = !it.showPaymentSheet) }
    }

    fun toggleEditSheet() {
        _state.update { it.copy(showEditSheet = !it.showEditSheet) }
    }
}
