package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun StatisticsRoute(
    onCategoryClick: (StatisticsCategoryDrillDownRequest) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshCurrentPeriod()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    StatisticsScreen(
        state = state,
        onPeriodChange = viewModel::setPeriod,
        onTransactionTypeChange = viewModel::setTransactionType,
        onCategoryClick = { category ->
            state.dateRange?.let { dateRange ->
                onCategoryClick(
                    StatisticsCategoryDrillDownRequest(
                        categoryId = category.categoryId,
                        categoryName = category.categoryName,
                        categoryIcon = category.categoryIcon,
                        categoryColor = category.categoryColor,
                        transactionType = state.transactionType.name,
                        period = state.period.name,
                        startMillis = dateRange.startMillis,
                        endMillis = dateRange.endMillis,
                    ),
                )
            }
        },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}
