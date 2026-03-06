package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CurrencyPickerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CurrencyPickerScreen(
        baseCurrency = state.baseCurrency,
        targetCurrency = state.targetCurrency,
        onBaseCurrencySelect = viewModel::setBaseCurrency,
        onTargetCurrencySelect = viewModel::setTargetCurrency,
        onBack = onBack,
        modifier = modifier,
    )
}
