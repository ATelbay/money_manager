package com.atelbay.money_manager.feature.accounts.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountEditRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AccountEditScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::setName,
        onCurrencyChange = viewModel::setCurrency,
        onSave = { viewModel.save(onBack) },
        modifier = modifier,
    )
}
