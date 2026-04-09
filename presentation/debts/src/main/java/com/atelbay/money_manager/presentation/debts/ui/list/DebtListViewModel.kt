package com.atelbay.money_manager.presentation.debts.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtStatus
import com.atelbay.money_manager.domain.accounts.usecase.GetAccountsUseCase
import com.atelbay.money_manager.domain.debts.usecase.DeleteDebtUseCase
import com.atelbay.money_manager.domain.debts.usecase.GetDebtsUseCase
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
import com.atelbay.money_manager.core.model.Debt

@HiltViewModel
class DebtListViewModel @Inject constructor(
    private val getDebtsUseCase: GetDebtsUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val saveDebtUseCase: SaveDebtUseCase,
    getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DebtListState())
    val state: StateFlow<DebtListState> = _state.asStateFlow()

    private val _accounts = MutableStateFlow<ImmutableList<Account>>(persistentListOf())
    val accounts: StateFlow<ImmutableList<Account>> = _accounts.asStateFlow()

    private var allDebts = emptyList<Debt>()

    init {
        getDebtsUseCase()
            .onEach { debts ->
                allDebts = debts
                applyFilter(_state.value.selectedFilter, debts)
            }
            .launchIn(viewModelScope)

        getAccountsUseCase()
            .onEach { _accounts.value = it.toImmutableList() }
            .launchIn(viewModelScope)
    }

    fun setFilter(filter: DebtFilter) {
        _state.update { it.copy(selectedFilter = filter) }
        applyFilter(filter, allDebts)
    }

    fun deleteDebt(id: Long) {
        viewModelScope.launch {
            deleteDebtUseCase(id)
        }
    }

    fun saveDebt(debt: Debt) {
        viewModelScope.launch {
            saveDebtUseCase(debt)
        }
    }

    private fun applyFilter(filter: DebtFilter, debts: List<Debt>) {
        val filtered = when (filter) {
            DebtFilter.ALL -> debts.filter { it.status == DebtStatus.ACTIVE }
            DebtFilter.LENT -> debts.filter { it.direction == DebtDirection.LENT && it.status == DebtStatus.ACTIVE }
            DebtFilter.BORROWED -> debts.filter { it.direction == DebtDirection.BORROWED && it.status == DebtStatus.ACTIVE }
            DebtFilter.PAID_OFF -> debts.filter { it.status == DebtStatus.PAID_OFF }
        }
        val activeDebts = debts.filter { it.status == DebtStatus.ACTIVE }
        _state.update {
            it.copy(
                debts = filtered.toImmutableList(),
                isLoading = false,
                totalLent = activeDebts
                    .filter { d -> d.direction == DebtDirection.LENT }
                    .sumOf { d -> d.remainingAmount },
                totalBorrowed = activeDebts
                    .filter { d -> d.direction == DebtDirection.BORROWED }
                    .sumOf { d -> d.remainingAmount },
            )
        }
    }
}
