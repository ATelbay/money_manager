package com.atelbay.money_manager.presentation.budgets.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.domain.budgets.usecase.DeleteBudgetUseCase
import com.atelbay.money_manager.domain.budgets.usecase.GetBudgetsWithSpendingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
class BudgetListViewModel @Inject constructor(
    private val getBudgetsWithSpendingUseCase: GetBudgetsWithSpendingUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetListState())
    val state: StateFlow<BudgetListState> = _state.asStateFlow()

    init {
        getBudgetsWithSpendingUseCase()
            .onEach { budgets ->
                _state.update {
                    it.copy(
                        budgets = budgets.toImmutableList(),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            deleteBudgetUseCase(id)
        }
    }
}
