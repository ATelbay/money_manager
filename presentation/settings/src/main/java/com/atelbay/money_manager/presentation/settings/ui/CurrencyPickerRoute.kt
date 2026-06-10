package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CurrencyPickerRoute(
    initialActiveSide: CurrencyPickerSide,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var activeSide by rememberSaveable { mutableStateOf(initialActiveSide) }

    CurrencyPickerScreen(
        baseCurrency = state.baseCurrency,
        targetCurrency = state.targetCurrency,
        activeSide = activeSide,
        onSideChange = { activeSide = it },
        onSelect = { currency ->
            // Keep the pair distinct: picking the currency already used on the other side
            // swaps them instead of producing a meaningless X→X pair.
            if (activeSide == CurrencyPickerSide.FIRST) {
                if (currency.code == state.targetCurrency.code) {
                    viewModel.setTargetCurrency(state.baseCurrency)
                }
                viewModel.setBaseCurrency(currency)
            } else {
                if (currency.code == state.baseCurrency.code) {
                    viewModel.setBaseCurrency(state.targetCurrency)
                }
                viewModel.setTargetCurrency(currency)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}
