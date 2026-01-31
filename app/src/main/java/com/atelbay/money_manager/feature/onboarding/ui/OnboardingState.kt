package com.atelbay.money_manager.feature.onboarding.ui

data class OnboardingState(
    val currentPage: Int = 0,
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

val OnboardingPages = listOf(
    OnboardingPage(
        title = "Добро пожаловать!",
        description = "Money Manager поможет вам контролировать расходы и доходы",
        icon = "account_balance_wallet",
    ),
    OnboardingPage(
        title = "Отслеживайте финансы",
        description = "Добавляйте транзакции, распределяйте по категориям и следите за балансом",
        icon = "bar_chart",
    ),
    OnboardingPage(
        title = "Анализируйте траты",
        description = "Графики и статистика помогут понять, куда уходят деньги",
        icon = "insights",
    ),
)

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String,
)
