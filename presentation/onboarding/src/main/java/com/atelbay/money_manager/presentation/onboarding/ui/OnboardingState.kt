package com.atelbay.money_manager.presentation.onboarding.ui

data class OnboardingState(
    val currentPage: Int = 0,
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
