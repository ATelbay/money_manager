package com.atelbay.money_manager.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
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
import com.atelbay.money_manager.presentation.onboarding.ui.OnboardingSetupRoute
import com.atelbay.money_manager.presentation.categories.ui.edit.CategoryEditRoute
import com.atelbay.money_manager.presentation.categories.ui.list.CategoryListRoute
import com.atelbay.money_manager.presentation.accounts.ui.edit.AccountEditRoute
import com.atelbay.money_manager.presentation.accounts.ui.list.AccountListRoute
import com.atelbay.money_manager.presentation.statistics.ui.CategoryTransactionsRoute
import com.atelbay.money_manager.presentation.statistics.ui.StatisticsRoute
import com.atelbay.money_manager.presentation.settings.ui.CurrencyPickerRoute
import com.atelbay.money_manager.presentation.settings.ui.SettingsRoute
import com.atelbay.money_manager.presentation.transactions.ui.edit.TransactionEditRoute
import com.atelbay.money_manager.presentation.transactions.ui.list.TransactionListRoute
import com.atelbay.money_manager.presentation.auth.ui.SignInRoute
import com.atelbay.money_manager.presentation.importstatement.ui.ImportRoute
import com.atelbay.money_manager.presentation.budgets.ui.list.BudgetListRoute
import com.atelbay.money_manager.presentation.budgets.ui.edit.BudgetEditRoute
import com.atelbay.money_manager.presentation.recurring.ui.list.RecurringListRoute
import com.atelbay.money_manager.presentation.recurring.ui.edit.RecurringEditRoute
import com.atelbay.money_manager.presentation.debts.ui.list.DebtListRoute
import com.atelbay.money_manager.presentation.debts.ui.detail.DebtDetailRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoneyManagerNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    bottomBarPadding: Dp = 0.dp,
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
                            navController.navigate(OnboardingSetup) {
                                popUpTo<Onboarding> { inclusive = true }
                            }
                        },
                    )
                }

                composable<OnboardingSetup> {
                    OnboardingSetupRoute(
                        onSignInComplete = {
                            navController.navigate(Home) {
                                popUpTo<OnboardingSetup> { inclusive = true }
                            }
                        },
                        onCreateAccount = {
                            navController.navigate(CreateAccount)
                        },
                        onSkip = {
                            navController.navigate(Home) {
                                popUpTo<OnboardingSetup> { inclusive = true }
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
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Home>(
                    enterTransition = {
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    exitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                    popEnterTransition = {
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    popExitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        TransactionListRoute(
                            modifier = Modifier.padding(bottom = bottomBarPadding),
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
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    exitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                    popEnterTransition = {
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    popExitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                ) {
                    StatisticsRoute(
                        modifier = Modifier.padding(bottom = bottomBarPadding),
                        onCategoryClick = { request ->
                            navController.navigate(
                                StatisticsCategoryTransactions(
                                    categoryId = request.categoryId,
                                    categoryName = request.categoryName,
                                    categoryIcon = request.categoryIcon,
                                    categoryColor = request.categoryColor,
                                    transactionType = request.transactionType,
                                    period = request.period,
                                    startMillis = request.startMillis,
                                    endMillis = request.endMillis,
                                ),
                            )
                        },
                    )
                }

                composable<StatisticsCategoryTransactions> {
                    CategoryTransactionsRoute(
                        onBack = { navController.popBackStack() },
                        onTransactionClick = { id ->
                            navController.navigate(TransactionEdit(id = id))
                        },
                    )
                }

                composable<AccountList>(
                    enterTransition = {
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    exitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                    popEnterTransition = {
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    popExitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                ) {
                    AccountListRoute(
                        modifier = Modifier.padding(bottom = bottomBarPadding),
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
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    exitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                    popEnterTransition = {
                        if (initialState.isTopLevel()) tabEnterOrNone(reduceMotion)
                        else EnterTransition.None
                    },
                    popExitTransition = {
                        if (targetState.isTopLevel()) tabExitOrNone(reduceMotion)
                        else ExitTransition.None
                    },
                ) {
                    SettingsRoute(
                        modifier = Modifier.padding(bottom = bottomBarPadding),
                        onCategoriesClick = {
                            navController.navigate(CategoryList)
                        },
                        onCurrencyPickerClick = {
                            navController.navigate(CurrencyPicker())
                        },
                        onSignInClick = {
                            navController.navigate(SignIn)
                        },
                        onBudgetsClick = {
                            navController.navigate(BudgetList)
                        },
                        onRecurringClick = {
                            navController.navigate(RecurringList)
                        },
                        onDebtsClick = {
                            navController.navigate(DebtList)
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

                composable<BudgetList> {
                    BudgetListRoute(
                        onAddClick = {
                            navController.navigate(BudgetEdit())
                        },
                        onBudgetClick = { id ->
                            navController.navigate(BudgetEdit(id = id))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<BudgetEdit> {
                    BudgetEditRoute(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<RecurringList> {
                    RecurringListRoute(
                        onAddClick = {
                            navController.navigate(RecurringEdit())
                        },
                        onEditClick = { id ->
                            navController.navigate(RecurringEdit(id = id))
                        },
                        onBackClick = { navController.popBackStack() },
                    )
                }

                composable<RecurringEdit> {
                    RecurringEditRoute(
                        onSaveComplete = { navController.popBackStack() },
                        onBackClick = { navController.popBackStack() },
                    )
                }

                composable<DebtList> {
                    DebtListRoute(
                        onDebtClick = { id -> navController.navigate(DebtDetail(id)) },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<DebtDetail> {
                    DebtDetailRoute(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private val topLevelRoutes = TopLevelDestination.entries.map { it.route::class }

private fun NavBackStackEntry.isTopLevel(): Boolean =
    topLevelRoutes.any { destination.hasRoute(it) }

private fun tabEnterOrNone(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) fadeIn(tween(MoneyManagerMotion.ReducedDuration))
    else MoneyManagerMotion.tabEnter()

private fun tabExitOrNone(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) fadeOut(tween(MoneyManagerMotion.ReducedDuration))
    else MoneyManagerMotion.tabExit()
