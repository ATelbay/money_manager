package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.MoneyManagerButton
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

@Composable
fun OnboardingSetupScreen(
    state: OnboardingSetupState,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onSkip: () -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.signInError) {
        state.signInError?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismiss()
        }
    }

    val isLoading = state.isSigningIn || state.isSyncing

    Scaffold(
        modifier = modifier.testTag("onboardingSetup:screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Teal,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = s.onboardingSetupTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = Teal,
                    modifier = Modifier.testTag("onboardingSetup:loading"),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (state.isSyncing) s.onboardingSyncing else s.signInWithGoogle,
                    style = typography.caption,
                    color = colors.textSecondary,
                )
            } else {
                // Option 1: Sign in with Google
                MoneyManagerButton(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboardingSetup:signIn"),
                ) {
                    Text(
                        text = s.onboardingSignInRestore,
                        style = typography.cardTitle,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Create new account
                OutlinedButton(
                    onClick = onCreateAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboardingSetup:createAccount"),
                ) {
                    Text(
                        text = s.onboardingCreateNewAccount,
                        style = typography.cardTitle,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Option 3: Skip
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("onboardingSetup:skip"),
                ) {
                    Text(
                        text = s.onboardingSkipForNow,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}
