package com.atelbay.money_manager.presentation.budgets.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BudgetEditRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BudgetEditScreen(
        state = state,
        onBack = onBack,
        onCategoryClick = { viewModel.toggleCategoryPicker(true) },
        onCategorySelect = viewModel::selectCategory,
        onCategoryDismiss = { viewModel.toggleCategoryPicker(false) },
        onLimitChange = viewModel::updateLimit,
        onSave = { viewModel.save(onBack) },
        modifier = modifier,
    )
}
