package com.atelbay.money_manager.presentation.transactions.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TransactionListRoute(
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TransactionListScreen(
        state = state,
        onTransactionClick = onTransactionClick,
        onAddClick = onAddClick,
        onImportClick = onImportClick,
        onDeleteTransaction = viewModel::deleteTransaction,
        onTabSelected = viewModel::selectTab,
        onPeriodSelected = viewModel::selectPeriod,
        onCustomDateRange = viewModel::setCustomDateRange,
        onSearchQueryChange = viewModel::updateSearchQuery,
        modifier = modifier,
    )
}
