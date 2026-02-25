package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    OnboardingScreen(
        state = state,
        onPageChanged = viewModel::setPage,
        onNextClick = {
            if (state.currentPage < OnboardingPages.size - 1) {
                viewModel.setPage(state.currentPage + 1)
            } else {
                onFinished()
            }
        },
        onSkipClick = onFinished,
        modifier = modifier,
    )
}
