package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoryTransactionsRoute(
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryTransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CategoryTransactionsScreen(
        state = state,
        onBack = onBack,
        onTransactionClick = onTransactionClick,
        modifier = modifier,
    )
}
