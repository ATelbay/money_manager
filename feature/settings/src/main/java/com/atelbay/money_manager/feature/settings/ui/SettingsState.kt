package com.atelbay.money_manager.feature.settings.ui

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "",
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
