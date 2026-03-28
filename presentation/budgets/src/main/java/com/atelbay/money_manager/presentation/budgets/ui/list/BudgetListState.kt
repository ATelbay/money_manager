package com.atelbay.money_manager.presentation.budgets.ui.list

import com.atelbay.money_manager.core.model.Budget
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class BudgetListState(
    val budgets: ImmutableList<Budget> = persistentListOf(),
    val isLoading: Boolean = true,
)
