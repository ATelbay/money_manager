package com.atelbay.money_manager.feature.accounts.ui.list

import com.atelbay.money_manager.core.model.Account
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AccountListState(
    val accounts: ImmutableList<Account> = persistentListOf(),
    val selectedAccountId: Long? = null,
    val totalBalance: Double = 0.0,
    val isLoading: Boolean = true,
)
