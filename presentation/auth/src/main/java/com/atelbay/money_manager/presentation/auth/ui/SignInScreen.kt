package com.atelbay.money_manager.presentation.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    state: SignInState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            snackbarHostState.showSnackbar(state.errorMessage)
            onErrorDismiss()
        }
    }

    Scaffold(
        modifier = modifier.testTag("signIn:screen"),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.accountSectionTitle,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.back,
                            tint = colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (state.user == null) {
                SignedOutContent(
                    isLoading = state.isLoading,
                    onSignInClick = onSignInClick,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                SignedInContent(
                    displayName = state.user.displayName,
                    email = state.user.email,
                    photoUrl = state.user.photoUrl,
                    onSignOutClick = onSignOutClick,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SignedOutContent(
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(80.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = MoneyManagerTheme.strings.signInSubtitle,
            style = typography.cardTitle,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSignInClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1F1F1F),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("signIn:googleButton"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                GoogleLogo()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = MoneyManagerTheme.strings.signInWithGoogle,
                    style = typography.cardTitle,
                    color = Color(0xFF1F1F1F),
                )
            }
        }
    }
}

@Composable
private fun SignedInContent(
    displayName: String?,
    email: String?,
    photoUrl: String?,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(modifier = modifier.padding(top = 16.dp)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signIn:userCard"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = MoneyManagerTheme.strings.userAvatar,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(48.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (!displayName.isNullOrBlank()) {
                        Text(
                            text = displayName,
                            style = typography.cardTitle,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = typography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSignOutClick,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("signIn:signOutButton"),
        ) {
            Text(
                text = MoneyManagerTheme.strings.signOut,
                style = typography.cardTitle,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun GoogleLogo() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        // Google 'G' logo using coloured segments via Canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
            val radius = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Blue arc (top-right)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.55f),
            )
            // Red arc (top-left)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = -180f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.55f),
            )
            // Yellow arc (bottom-left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.55f),
            )
            // Green arc (bottom-right)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.55f),
            )
        }
    }
}
