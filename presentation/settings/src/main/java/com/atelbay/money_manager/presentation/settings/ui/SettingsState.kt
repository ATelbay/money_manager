package com.atelbay.money_manager.presentation.settings.ui

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val baseCurrency: SupportedCurrency = SupportedCurrencies.defaultBase,
    val targetCurrency: SupportedCurrency = SupportedCurrencies.defaultTarget,
    val rateDisplay: String = "",
    val lastUpdatedDisplay: String = "",
    val isRefreshingRate: Boolean = false,
    val rateErrorMessage: String? = null,
    val appVersion: String = "",
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
