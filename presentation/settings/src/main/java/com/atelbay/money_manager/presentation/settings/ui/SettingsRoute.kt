package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun SettingsRoute(
    onCategoriesClick: () -> Unit,
    onCurrencyPickerClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = MoneyManagerTheme.strings

    SettingsScreen(
        state = state,
        onThemeModeChange = viewModel::setThemeMode,
        onLanguageChange = viewModel::setLanguage,
        onRefreshRateClick = { viewModel.refreshExchangeRate(strings) },
        onCategoriesClick = onCategoriesClick,
        onCurrencyPickerClick = onCurrencyPickerClick,
        onSignInClick = onSignInClick,
        onRetrySyncClick = viewModel::retrySync,
        modifier = modifier,
    )
}
