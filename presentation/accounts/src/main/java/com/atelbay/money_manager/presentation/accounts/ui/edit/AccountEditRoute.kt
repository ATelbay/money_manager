package com.atelbay.money_manager.presentation.accounts.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.AppStrings
import com.atelbay.money_manager.core.ui.theme.LocalStrings

@Composable
fun AccountEditRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings: AppStrings = LocalStrings.current

    AccountEditScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::setName,
        onCurrencyChange = viewModel::setCurrency,
        onSave = { viewModel.save(strings, onBack) },
        modifier = modifier,
    )
}
