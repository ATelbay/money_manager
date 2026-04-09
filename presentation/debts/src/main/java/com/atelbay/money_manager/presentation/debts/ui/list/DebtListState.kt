package com.atelbay.money_manager.presentation.debts.ui.list

import com.atelbay.money_manager.core.model.Debt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DebtListState(
    val debts: ImmutableList<Debt> = persistentListOf(),
    val isLoading: Boolean = true,
    val selectedFilter: DebtFilter = DebtFilter.ALL,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
)
