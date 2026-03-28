package com.atelbay.money_manager.presentation.recurring.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun RecurringEditRoute(
    onSaveComplete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = MoneyManagerTheme.strings

    RecurringEditScreen(
        state = state,
        onBack = onBackClick,
        onAmountChange = viewModel::updateAmount,
        onTypeChange = viewModel::selectType,
        onCategoryClick = { viewModel.toggleCategoryPicker(true) },
        onCategorySelect = viewModel::selectCategory,
        onCategoryDismiss = { viewModel.toggleCategoryPicker(false) },
        onAccountSelect = viewModel::selectAccount,
        onFrequencySelect = viewModel::selectFrequency,
        onDayOfMonthSelect = viewModel::setDayOfMonth,
        onDayOfWeekSelect = viewModel::setDayOfWeek,
        onStartDateClick = { viewModel.toggleStartDatePicker(true) },
        onStartDateSelect = viewModel::setStartDate,
        onStartDateDismiss = { viewModel.toggleStartDatePicker(false) },
        onEndDateClick = { viewModel.toggleEndDatePicker(true) },
        onEndDateSelect = viewModel::setEndDate,
        onEndDateDismiss = { viewModel.toggleEndDatePicker(false) },
        onNoteChange = viewModel::updateNote,
        onSave = { viewModel.save(onSaveComplete, strings.errorEnterValidAmount) },
        modifier = modifier,
    )
}
