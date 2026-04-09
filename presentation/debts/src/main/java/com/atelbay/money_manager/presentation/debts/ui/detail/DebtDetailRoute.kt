package com.atelbay.money_manager.presentation.debts.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DebtDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    // Navigate back after debt deletion
    LaunchedEffect(state.debt) {
        if (!state.isLoading && state.debt == null) {
            onBack()
        }
    }

    DebtDetailScreen(
        state = state,
        accounts = accounts,
        onAddPayment = viewModel::addPayment,
        onDeletePayment = viewModel::deletePayment,
        onDeleteDebt = {
            viewModel.deleteDebt()
            onBack()
        },
        onSaveDebt = viewModel::saveDebt,
        onTogglePaymentSheet = viewModel::togglePaymentSheet,
        onToggleEditSheet = viewModel::toggleEditSheet,
        onBack = onBack,
        modifier = modifier,
    )
}
