package com.atelbay.money_manager.presentation.recurring.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.domain.recurring.usecase.DeleteRecurringTransactionUseCase
import com.atelbay.money_manager.domain.recurring.usecase.GetRecurringTransactionsUseCase
import com.atelbay.money_manager.domain.recurring.usecase.SaveRecurringTransactionUseCase
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
class RecurringListViewModel @Inject constructor(
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val saveRecurringTransactionUseCase: SaveRecurringTransactionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringListState())
    val state: StateFlow<RecurringListState> = _state.asStateFlow()

    init {
        getRecurringTransactionsUseCase()
            .onEach { recurrings ->
                _state.update {
                    it.copy(
                        recurrings = recurrings.toImmutableList(),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            deleteRecurringTransactionUseCase(id)
        }
    }

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            val recurring = _state.value.recurrings.find { it.id == id } ?: return@launch
            saveRecurringTransactionUseCase(recurring.copy(isActive = isActive))
        }
    }
}
