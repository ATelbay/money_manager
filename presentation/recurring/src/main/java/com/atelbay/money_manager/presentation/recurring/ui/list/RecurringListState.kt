package com.atelbay.money_manager.presentation.recurring.ui.list

import com.atelbay.money_manager.core.model.RecurringTransaction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RecurringListState(
    val recurrings: ImmutableList<RecurringTransaction> = persistentListOf(),
    val isLoading: Boolean = true,
)
