package com.atelbay.money_manager.presentation.recurring.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RecurringListRoute(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RecurringListScreen(
        state = state,
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        onDeleteRecurring = viewModel::deleteRecurring,
        onToggleActive = viewModel::toggleActive,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
