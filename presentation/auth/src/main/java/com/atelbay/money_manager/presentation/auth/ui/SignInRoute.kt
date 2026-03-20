package com.atelbay.money_manager.presentation.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun SignInRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = MoneyManagerTheme.strings

    SignInScreen(
        state = state,
        onSignInClick = { viewModel.signIn(strings) },
        onSignOutClick = viewModel::signOut,
        onErrorDismiss = viewModel::clearError,
        onBack = onBack,
        modifier = modifier,
    )
}
