package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateAccountRoute(
    onAccountCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CreateAccountScreen(
        state = state,
        onAccountNameChange = viewModel::setAccountName,
        onCurrencyChange = viewModel::setCurrency,
        onBalanceChange = viewModel::setInitialBalance,
        onCreateClick = { viewModel.createAccount(onAccountCreated) },
        modifier = modifier,
    )
}
