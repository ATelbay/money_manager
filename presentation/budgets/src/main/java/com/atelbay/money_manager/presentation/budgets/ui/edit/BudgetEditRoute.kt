package com.atelbay.money_manager.presentation.budgets.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun BudgetEditRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = MoneyManagerTheme.strings

    BudgetEditScreen(
        state = state,
        onBack = onBack,
        onCategoryClick = { viewModel.toggleCategoryPicker(true) },
        onCategorySelect = viewModel::selectCategory,
        onCategoryDismiss = { viewModel.toggleCategoryPicker(false) },
        onLimitChange = viewModel::updateLimit,
        onSave = {
            viewModel.save(
                onComplete = onBack,
                categoryError = strings.errorSelectCategory,
                limitError = strings.errorEnterValidLimit,
            )
        },
        modifier = modifier,
    )
}
