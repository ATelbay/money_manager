package com.atelbay.money_manager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.ui.theme.LocalStrings
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.appStringsFor
import com.atelbay.money_manager.navigation.Home
import com.atelbay.money_manager.navigation.Import
import com.atelbay.money_manager.navigation.MoneyManagerBottomBar
import com.atelbay.money_manager.navigation.MoneyManagerNavHost
import com.atelbay.money_manager.navigation.NavigationAction
import com.atelbay.money_manager.navigation.Onboarding
import com.atelbay.money_manager.navigation.PendingNavigationManager
import com.atelbay.money_manager.navigation.TopLevelDestination
import com.atelbay.money_manager.navigation.TransactionEdit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

private const val BottomBarAnimationDurationMs = 150

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var pendingNavigationManager: PendingNavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractPdfUri(intent)?.let { pendingNavigationManager.enqueue(NavigationAction.OpenImport(it)) }
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = "system")

            val languageCode by userPreferences.languageCode
                .collectAsStateWithLifecycle(initialValue = "ru")

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                )
                onDispose {}
            }

            CompositionLocalProvider(LocalStrings provides appStringsFor(languageCode)) {
                MoneyManagerTheme(themeMode = themeMode) {
                    val onboardingCompleted by userPreferences.isOnboardingCompleted
                        .collectAsStateWithLifecycle(initialValue = null)

                    val completed = onboardingCompleted
                    if (completed != null) {
                        val navController = rememberNavController()
                        MoneyManagerApp(
                            navController = navController,
                            startDestination = if (completed) Home else Onboarding,
                            onboardingCompleted = completed,
                            pendingNavigationManager = pendingNavigationManager,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractPdfUri(intent)?.let { pendingNavigationManager.enqueue(NavigationAction.OpenImport(it)) }
    }

    private fun extractPdfUri(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_SEND -> {
            // Manifest filter already guarantees mimeType=application/pdf;
            // fall back to ClipData for apps that don't set EXTRA_STREAM
            @Suppress("DEPRECATION")
            val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            val clipUri = intent.clipData?.getItemAt(0)?.uri
            (streamUri ?: clipUri)?.toString()
        }
        Intent.ACTION_VIEW -> {
            // Manifest filter already guarantees mimeType=application/pdf;
            // intent.type is often null for ACTION_VIEW — trust the filter, not the field
            intent.data?.toString()
        }
        else -> null
    }
}

@Composable
private fun MoneyManagerApp(
    navController: NavHostController,
    startDestination: Any,
    onboardingCompleted: Boolean,
    pendingNavigationManager: PendingNavigationManager,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTopLevel = TopLevelDestination.entries.find { dest ->
        currentDestination?.hasRoute(dest.route::class) == true
    }

    val showBottomBar = currentTopLevel != null

    // Keep last known destination so bottom bar shows correctly during exit animation
    var lastTopLevel by remember { mutableStateOf(currentTopLevel) }
    SideEffect {
        if (currentTopLevel != null) {
            lastTopLevel = currentTopLevel
        }
    }
    val bottomBarDestination = currentTopLevel ?: lastTopLevel

    // Force-hide state for delayed FAB navigation
    var forceHideBottomBar by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val effectiveShowBottomBar = showBottomBar && !forceHideBottomBar
    val bottomBarVisibility = remember { MutableTransitionState(effectiveShowBottomBar) }
    bottomBarVisibility.targetState = effectiveShowBottomBar

    // Execute pending navigation after bottom bar has finished hiding
    LaunchedEffect(pendingNavAction) {
        val action = pendingNavAction ?: return@LaunchedEffect
        delay(BottomBarAnimationDurationMs.toLong() + 10L)
        action()
        forceHideBottomBar = false
        pendingNavAction = null
    }

    // Gate: open the channel only after onboarding is complete
    LaunchedEffect(onboardingCompleted) {
        pendingNavigationManager.setReady(onboardingCompleted)
    }
    // Prevent a stale navController from receiving queued actions after config change
    DisposableEffect(Unit) {
        onDispose { pendingNavigationManager.setReady(false) }
    }
    // Long-lived collector: drains the channel as actions arrive
    LaunchedEffect(Unit) {
        pendingNavigationManager.readyActions.collect { action ->
            when (action) {
                is NavigationAction.OpenImport ->
                    navController.navigate(Import(pdfUri = action.pdfUri)) {
                        launchSingleTop = true
                    }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visibleState = bottomBarVisibility,
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = BottomBarAnimationDurationMs,
                        easing = FastOutSlowInEasing,
                    ),
                ) { fullHeight -> fullHeight } + fadeIn(
                    animationSpec = tween(durationMillis = BottomBarAnimationDurationMs),
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = BottomBarAnimationDurationMs,
                        easing = FastOutSlowInEasing,
                    ),
                ) { fullHeight -> fullHeight } + fadeOut(
                    animationSpec = tween(durationMillis = BottomBarAnimationDurationMs),
                ),
            ) {
                MoneyManagerBottomBar(
                    currentDestination = bottomBarDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        val animatedBottomPadding by animateDpAsState(
            targetValue = padding.calculateBottomPadding(),
            animationSpec = tween(
                durationMillis = BottomBarAnimationDurationMs,
                easing = FastOutSlowInEasing,
            ),
            label = "navHostBottomPadding",
        )
        MoneyManagerNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = animatedBottomPadding),
            onFabNavigate = {
                forceHideBottomBar = true
                pendingNavAction = { navController.navigate(TransactionEdit()) }
            },
        )
    }
}
