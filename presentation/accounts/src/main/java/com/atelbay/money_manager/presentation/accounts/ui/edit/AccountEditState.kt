package com.atelbay.money_manager.presentation.accounts.ui.edit

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AccountEditState(
    val accountId: Long? = null,
    val name: String = "",
    val currency: String = "KZT",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val availableCurrencies: ImmutableList<String> = persistentListOf(
        "KZT", "RUB", "USD", "EUR", "GBP", "TRY", "CNY", "JPY",
    ),
) {
    val isEditing: Boolean get() = accountId != null
}
