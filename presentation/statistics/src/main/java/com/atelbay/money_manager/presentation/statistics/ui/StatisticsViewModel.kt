package com.atelbay.money_manager.presentation.statistics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import com.atelbay.money_manager.domain.statistics.usecase.GetPeriodSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getPeriodSummaryUseCase: GetPeriodSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    private var summaryJob: Job? = null

    init {
        loadSummary(_state.value.period)
    }

    fun setPeriod(period: StatsPeriod) {
        if (_state.value.period == period) return
        _state.update { it.copy(period = period, isLoading = true) }
        loadSummary(period)
    }

    fun setTransactionType(type: TransactionType) {
        if (_state.value.transactionType == type) return
        _state.update { it.copy(transactionType = type) }
    }

    fun retry() {
        _state.update { it.copy(isLoading = true, error = null) }
        loadSummary(_state.value.period)
    }

    private fun loadSummary(period: StatsPeriod) {
        summaryJob?.cancel()
        summaryJob = getPeriodSummaryUseCase(period)
            .distinctUntilChanged()
            .onEach { summary ->
                _state.update {
                    it.copy(
                        totalExpenses = summary.totalExpenses,
                        totalIncome = summary.totalIncome,
                        expensesByCategory = summary.expensesByCategory.toImmutableList(),
                        incomesByCategory = summary.incomesByCategory.toImmutableList(),
                        dailyExpenses = summary.dailyExpenses.toImmutableList(),
                        dailyIncome = summary.dailyIncome.toImmutableList(),
                        monthlyExpenses = summary.monthlyExpenses.toImmutableList(),
                        monthlyIncome = summary.monthlyIncome.toImmutableList(),
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { e ->
                _state.update {
                    it.copy(
                        error = e.message ?: "Unknown error",
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
