package com.atelbay.money_manager.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.currentPage < OnboardingPages.size) {
        OnboardingScreen(
            state = state,
            onPageChanged = viewModel::setPage,
            onNextClick = {
                if (state.currentPage < OnboardingPages.size - 1) {
                    viewModel.setPage(state.currentPage + 1)
                } else {
                    viewModel.setPage(OnboardingPages.size)
                }
            },
            onSkipClick = {
                viewModel.setPage(OnboardingPages.size)
            },
            modifier = modifier,
        )
    } else {
        CreateAccountScreen(
            state = state,
            onAccountNameChange = viewModel::setAccountName,
            onCurrencyChange = viewModel::setCurrency,
            onBalanceChange = viewModel::setInitialBalance,
            onCreateClick = { viewModel.createAccount(onOnboardingComplete) },
            modifier = modifier,
        )
    }
}
