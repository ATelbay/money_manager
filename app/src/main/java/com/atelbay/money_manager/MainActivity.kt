package com.atelbay.money_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                MoneyManagerBottomBar(
                    currentDestination = currentTopLevel,
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
            modifier = Modifier.padding(padding),
        )
    }
}
