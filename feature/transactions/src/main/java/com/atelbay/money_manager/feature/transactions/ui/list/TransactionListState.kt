package com.atelbay.money_manager.feature.transactions.ui.list

import com.atelbay.money_manager.core.model.Transaction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class TransactionListState(
    val transactions: ImmutableList<Transaction> = persistentListOf(),
    val balance: Double = 0.0,
    val currency: String = "",
    val isLoading: Boolean = true,
    val selectedAccountName: String? = null,
)
