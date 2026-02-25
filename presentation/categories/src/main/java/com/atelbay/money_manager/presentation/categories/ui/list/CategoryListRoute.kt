package com.atelbay.money_manager.presentation.categories.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoryListRoute(
    onCategoryClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CategoryListScreen(
        state = state,
        onTypeSelect = viewModel::selectType,
        onCategoryClick = onCategoryClick,
        onAddClick = onAddClick,
        onDeleteCategory = viewModel::deleteCategory,
        onBack = onBack,
        modifier = modifier,
    )
}
