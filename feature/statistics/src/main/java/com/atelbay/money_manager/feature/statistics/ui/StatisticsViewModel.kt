package com.atelbay.money_manager.feature.statistics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.feature.statistics.domain.model.StatsPeriod
import com.atelbay.money_manager.feature.statistics.domain.usecase.GetPeriodSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private fun loadSummary(period: StatsPeriod) {
        summaryJob?.cancel()
        summaryJob = getPeriodSummaryUseCase(period)
            .onEach { summary ->
                _state.update {
                    it.copy(
                        totalExpenses = summary.totalExpenses,
                        totalIncome = summary.totalIncome,
                        expensesByCategory = summary.expensesByCategory.toImmutableList(),
                        dailyExpenses = summary.dailyExpenses.toImmutableList(),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
