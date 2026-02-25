package com.atelbay.money_manager.presentation.categories.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoryEditRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CategoryEditScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::setName,
        onTypeChange = viewModel::setType,
        onIconSelect = viewModel::selectIcon,
        onColorSelect = viewModel::selectColor,
        onSave = { viewModel.save(onBack) },
        modifier = modifier,
    )
}
