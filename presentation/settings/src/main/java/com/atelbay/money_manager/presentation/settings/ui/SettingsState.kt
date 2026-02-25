package com.atelbay.money_manager.presentation.settings.ui

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "",
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
