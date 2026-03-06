package com.atelbay.money_manager.presentation.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SignInRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SignInScreen(
        state = state,
        onSignInClick = { viewModel.signIn(context) },
        onSignOutClick = viewModel::signOut,
        onErrorDismiss = viewModel::clearError,
        onBack = onBack,
        modifier = modifier,
    )
}
