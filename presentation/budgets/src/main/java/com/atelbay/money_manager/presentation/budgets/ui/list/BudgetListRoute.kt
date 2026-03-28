package com.atelbay.money_manager.presentation.budgets.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BudgetListRoute(
    onAddClick: () -> Unit,
    onBudgetClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BudgetListScreen(
        state = state,
        onAddClick = onAddClick,
        onBudgetClick = onBudgetClick,
        onDeleteBudget = viewModel::deleteBudget,
        onBack = onBack,
        modifier = modifier,
    )
}
