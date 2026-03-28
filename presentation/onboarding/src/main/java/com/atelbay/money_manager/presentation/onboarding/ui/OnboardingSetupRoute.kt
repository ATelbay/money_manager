package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun OnboardingSetupRoute(
    onSignInComplete: () -> Unit,
    onCreateAccount: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = MoneyManagerTheme.strings

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect {
            onSignInComplete()
        }
    }

    OnboardingSetupScreen(
        state = state,
        onSignIn = { viewModel.signIn(strings) },
        onCreateAccount = onCreateAccount,
        onSkip = { viewModel.skip() },
        onErrorDismiss = viewModel::clearError,
        modifier = modifier,
    )
}
