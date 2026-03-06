package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onCategoriesClick: () -> Unit,
    onCurrencyPickerClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onThemeModeChange = viewModel::setThemeMode,
        onRefreshRateClick = viewModel::refreshExchangeRate,
        onCategoriesClick = onCategoriesClick,
        onCurrencyPickerClick = onCurrencyPickerClick,
        onSignInClick = onSignInClick,
        modifier = modifier,
    )
}
