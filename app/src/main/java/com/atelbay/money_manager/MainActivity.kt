package com.atelbay.money_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.navigation.Home
import com.atelbay.money_manager.navigation.MoneyManagerNavHost
import com.atelbay.money_manager.navigation.Onboarding
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
            MoneyManagerTheme {
                val onboardingCompleted by userPreferences.isOnboardingCompleted
                    .collectAsStateWithLifecycle(initialValue = null)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val completed = onboardingCompleted
                    if (completed != null) {
                        val navController = rememberNavController()
                        MoneyManagerNavHost(
                            navController = navController,
                            startDestination = if (completed) Home else Onboarding,
                        )
                    }
                }
            }
        }
    }
}
