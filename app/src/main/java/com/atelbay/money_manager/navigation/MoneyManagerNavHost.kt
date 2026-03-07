package com.atelbay.money_manager.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.atelbay.money_manager.core.ui.components.LocalAnimatedVisibilityScope
import com.atelbay.money_manager.core.ui.components.LocalSharedTransitionScope
import com.atelbay.money_manager.presentation.onboarding.ui.CreateAccountRoute
import com.atelbay.money_manager.presentation.onboarding.ui.OnboardingRoute
import com.atelbay.money_manager.presentation.categories.ui.edit.CategoryEditRoute
import com.atelbay.money_manager.presentation.categories.ui.list.CategoryListRoute
import com.atelbay.money_manager.presentation.accounts.ui.edit.AccountEditRoute
import com.atelbay.money_manager.presentation.accounts.ui.list.AccountListRoute
import com.atelbay.money_manager.presentation.statistics.ui.StatisticsRoute
import com.atelbay.money_manager.presentation.settings.ui.CurrencyPickerRoute
import com.atelbay.money_manager.presentation.settings.ui.SettingsRoute
import com.atelbay.money_manager.presentation.transactions.ui.edit.TransactionEditRoute
import com.atelbay.money_manager.presentation.transactions.ui.list.TransactionListRoute
import com.atelbay.money_manager.presentation.auth.ui.SignInRoute
import com.atelbay.money_manager.presentation.importstatement.ui.ImportRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoneyManagerNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    onFabNavigate: (() -> Unit)? = null,
) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier,
                enterTransition = {
                    fadeIn(tween(220, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(220, easing = LinearOutSlowInEasing)) { it / 4 }
                },
                exitTransition = {
                    fadeOut(tween(200, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(200, easing = FastOutLinearInEasing)) { -it / 4 }
                },
                popEnterTransition = {
                    fadeIn(tween(220, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(220, easing = LinearOutSlowInEasing)) { -it / 4 }
                },
                popExitTransition = {
                    fadeOut(tween(200, easing = FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(200, easing = FastOutLinearInEasing)) { it / 4 }
                },
            ) {
                composable<Onboarding> {
                    OnboardingRoute(
                        onFinished = {
                            navController.navigate(CreateAccount) {
                                popUpTo<Onboarding> { inclusive = true }
                            }
                        },
                    )
                }

                composable<CreateAccount> {
                    CreateAccountRoute(
                        onAccountCreated = {
                            navController.navigate(Home) {
                                popUpTo<CreateAccount> { inclusive = true }
                            }
                        },
                    )
                }

                composable<Home>(
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(200)) },
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        TransactionListRoute(
                            onTransactionClick = { id ->
                                navController.navigate(TransactionEdit(id = id))
                            },
                            onAddClick = onFabNavigate ?: {
                                navController.navigate(TransactionEdit())
                            },
                            onImportClick = {
                                navController.navigate(Import())
                            },
                        )
                    }
                }

                composable<Import> { backStackEntry ->
                    ImportRoute(
                        onBack = { navController.popBackStack() },
                        initialPdfUri = backStackEntry.toRoute<Import>().pdfUri,
                    )
                }

                composable<TransactionEdit>(
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        TransactionEditRoute(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                composable<CategoryList> {
                    CategoryListRoute(
                        onCategoryClick = { id ->
                            navController.navigate(CategoryEdit(id = id))
                        },
                        onAddClick = {
                            navController.navigate(CategoryEdit())
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<CategoryEdit> {
                    CategoryEditRoute(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Statistics>(
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(200)) },
                ) {
                    StatisticsRoute()
                }

                composable<AccountList>(
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(200)) },
                ) {
                    AccountListRoute(
                        onAccountClick = { id ->
                            navController.navigate(AccountEdit(id = id))
                        },
                        onAddClick = {
                            navController.navigate(AccountEdit())
                        },
                    )
                }

                composable<AccountEdit> {
                    AccountEditRoute(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Settings>(
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(200)) },
                ) {
                    SettingsRoute(
                        onCategoriesClick = {
                            navController.navigate(CategoryList)
                        },
                        onCurrencyPickerClick = {
                            navController.navigate(CurrencyPicker())
                        },
                        onSignInClick = {
                            navController.navigate(SignIn)
                        },
                    )
                }

                composable<SignIn> {
                    SignInRoute(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<CurrencyPicker> { backStackEntry ->
                    CurrencyPickerRoute(
                        initialActiveSide = backStackEntry.toRoute<CurrencyPicker>().activeSide,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
