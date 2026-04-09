package com.atelbay.money_manager.presentation.debts.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DebtListRoute(
    onDebtClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    DebtListScreen(
        state = state,
        accounts = accounts,
        onDebtClick = onDebtClick,
        onDeleteDebt = viewModel::deleteDebt,
        onSaveDebt = viewModel::saveDebt,
        onFilterChange = viewModel::setFilter,
        onBack = onBack,
        modifier = modifier,
    )
}
