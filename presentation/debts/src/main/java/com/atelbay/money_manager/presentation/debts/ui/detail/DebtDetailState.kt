package com.atelbay.money_manager.presentation.debts.ui.detail

import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtPayment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DebtDetailState(
    val debt: Debt? = null,
    val payments: ImmutableList<DebtPayment> = persistentListOf(),
    val isLoading: Boolean = true,
    val showPaymentSheet: Boolean = false,
    val showEditSheet: Boolean = false,
)
