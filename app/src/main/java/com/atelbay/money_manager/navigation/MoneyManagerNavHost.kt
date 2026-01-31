package com.atelbay.money_manager.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.atelbay.money_manager.feature.onboarding.ui.OnboardingRoute

@Composable
fun MoneyManagerNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Onboarding> {
            OnboardingRoute(
                onOnboardingComplete = {
                    navController.navigate(Home) {
                        popUpTo<Onboarding> { inclusive = true }
                    }
                },
            )
        }

        composable<Home> {
            // Placeholder — будет заменён на TransactionListScreen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Home")
            }
        }
    }
}
