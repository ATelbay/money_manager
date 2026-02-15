package com.atelbay.money_manager.feature.transactions.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TransactionEditRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TransactionEditScreen(
        state = state,
        onBack = onBack,
        onTypeChange = viewModel::setType,
        onAmountChange = viewModel::setAmount,
        onCategoryClick = { viewModel.toggleCategorySheet(true) },
        onCategorySelect = viewModel::selectCategory,
        onCategoryDismiss = { viewModel.toggleCategorySheet(false) },
        onDateClick = { viewModel.toggleDatePicker(true) },
        onDateSelect = viewModel::setDate,
        onDateDismiss = { viewModel.toggleDatePicker(false) },
        onNoteChange = viewModel::setNote,
        onSave = { viewModel.save(onBack) },
        onDelete = if (state.isEditing) {
            {
                viewModel.deleteTransaction(onBack)
            }
        } else {
            null
        },
        modifier = modifier,
    )
}
