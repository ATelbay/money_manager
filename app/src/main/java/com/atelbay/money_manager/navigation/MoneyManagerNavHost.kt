package com.atelbay.money_manager.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.atelbay.money_manager.core.ui.components.LocalAnimatedVisibilityScope
import com.atelbay.money_manager.core.ui.components.LocalSharedTransitionScope
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import com.atelbay.money_manager.core.ui.util.LocalReduceMotion
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
    val reduceMotion = LocalReduceMotion.current

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier,
                enterTransition = {
                    if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                    else MoneyManagerMotion.drillInEnter()
                },
                exitTransition = {
                    if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                    else MoneyManagerMotion.drillInExit()
                },
                popEnterTransition = {
                    if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                    else MoneyManagerMotion.drillInPopEnter()
                },
                popExitTransition = {
                    if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                    else MoneyManagerMotion.drillInPopExit()
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
                    enterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    exitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
                    popEnterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    popExitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
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
                    enterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    exitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
                    popEnterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    popExitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
                ) {
                    StatisticsRoute()
                }

                composable<AccountList>(
                    enterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    exitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
                    popEnterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    popExitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
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
                    enterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    exitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
                    popEnterTransition = {
                        if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabEnter()
                    },
                    popExitTransition = {
                        if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
                        else MoneyManagerMotion.tabExit()
                    },
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
