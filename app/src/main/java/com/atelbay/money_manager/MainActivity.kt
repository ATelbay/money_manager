package com.atelbay.money_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.runtime.DisposableEffect
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
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.navigation.Home
import com.atelbay.money_manager.navigation.MoneyManagerBottomBar
import com.atelbay.money_manager.navigation.MoneyManagerNavHost
import com.atelbay.money_manager.navigation.Onboarding
import com.atelbay.money_manager.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val BottomBarAnimationDurationMs = 220

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = "system")

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

            MoneyManagerTheme(themeMode = themeMode) {
                val onboardingCompleted by userPreferences.isOnboardingCompleted
                    .collectAsStateWithLifecycle(initialValue = null)

                val completed = onboardingCompleted
                if (completed != null) {
                    val navController = rememberNavController()
                    MoneyManagerApp(
                        navController = navController,
                        startDestination = if (completed) Home else Onboarding,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyManagerApp(
    navController: NavHostController,
    startDestination: Any,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTopLevel = TopLevelDestination.entries.find { dest ->
        currentDestination?.hasRoute(dest.route::class) == true
    }

    val showBottomBar = currentTopLevel != null
    var lastTopLevel by remember { mutableStateOf(currentTopLevel) }
    SideEffect {
        if (currentTopLevel != null) {
            lastTopLevel = currentTopLevel
        }
    }
    val bottomBarDestination = currentTopLevel ?: lastTopLevel
    val bottomBarVisibility = remember { MutableTransitionState(showBottomBar) }
    bottomBarVisibility.targetState = showBottomBar

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
        MoneyManagerNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        )
    }
}
