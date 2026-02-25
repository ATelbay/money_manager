package com.atelbay.money_manager.presentation.onboarding.ui

data class CreateAccountState(
    val accountName: String = "",
    val currency: String = "KZT",
    val initialBalance: String = "",
    val accountNameError: String? = null,
    val balanceError: String? = null,
    val availableCurrencies: List<String> = listOf(
        "KZT", "USD", "EUR", "RUB", "GBP", "CNY", "TRY",
    ),
    val isCreating: Boolean = false,
)
