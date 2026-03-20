package com.atelbay.money_manager.presentation.settings.ui

import com.atelbay.money_manager.core.auth.AuthUser
import com.atelbay.money_manager.core.model.SyncStatus

data class SettingsState(
    val currentUser: AuthUser? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.RUSSIAN,
    val baseCurrency: SupportedCurrency = SupportedCurrencies.defaultBase,
    val targetCurrency: SupportedCurrency = SupportedCurrencies.defaultTarget,
    val rateDisplay: String? = null,
    val hasRateSnapshot: Boolean = false,
    val lastUpdatedDisplay: String = "",
    val isRefreshingRate: Boolean = false,
    val rateErrorMessage: String? = null,
    val appVersion: String = "",
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncDisplay: String = "",
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage(val code: String, val displayName: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    KAZAKH("kk", "Қазақша"),
}
