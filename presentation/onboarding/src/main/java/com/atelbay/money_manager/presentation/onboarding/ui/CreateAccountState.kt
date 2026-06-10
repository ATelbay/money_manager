package com.atelbay.money_manager.presentation.onboarding.ui

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateAccountState(
    val accountName: String = "",
    val currency: String = "KZT",
    val initialBalance: String = "",
    val accountNameError: String? = null,
    val balanceError: String? = null,
    val availableCurrencies: ImmutableList<String> = persistentListOf("KZT"),
    val isCreating: Boolean = false,
)
