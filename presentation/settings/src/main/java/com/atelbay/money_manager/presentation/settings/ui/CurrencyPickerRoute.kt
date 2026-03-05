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
        selected = state.baseCurrency,
        onSelect = { currency ->
            viewModel.setBaseCurrency(currency)
            val opposite = if (currency.code == "KZT") {
                SupportedCurrencies.fromCode("USD")
            } else {
                SupportedCurrencies.fromCode("KZT")
            }
            viewModel.setTargetCurrency(opposite)
            onBack()
        },
        onBack = onBack,
        modifier = modifier,
    )
}
