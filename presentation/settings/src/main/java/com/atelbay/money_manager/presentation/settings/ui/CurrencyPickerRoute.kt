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
            if (activeSide == CurrencyPickerSide.FIRST) {
                viewModel.setBaseCurrency(currency)
            } else {
                viewModel.setTargetCurrency(currency)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}
