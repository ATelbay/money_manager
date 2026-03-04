package com.atelbay.money_manager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.atelbay.money_manager.presentation.onboarding.ui.CreateAccountRoute
import com.atelbay.money_manager.presentation.onboarding.ui.OnboardingRoute
import com.atelbay.money_manager.presentation.categories.ui.edit.CategoryEditRoute
import com.atelbay.money_manager.presentation.categories.ui.list.CategoryListRoute
import com.atelbay.money_manager.presentation.accounts.ui.edit.AccountEditRoute
import com.atelbay.money_manager.presentation.accounts.ui.list.AccountListRoute
import com.atelbay.money_manager.presentation.statistics.ui.StatisticsRoute
import com.atelbay.money_manager.presentation.settings.ui.SettingsRoute
import com.atelbay.money_manager.presentation.transactions.ui.edit.TransactionEditRoute
import com.atelbay.money_manager.presentation.transactions.ui.list.TransactionListRoute
import com.atelbay.money_manager.presentation.importstatement.ui.ImportRoute

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

        composable<Home> {
            TransactionListRoute(
                onTransactionClick = { id ->
                    navController.navigate(TransactionEdit(id = id))
                },
                onAddClick = {
                    navController.navigate(TransactionEdit())
                },
                onImportClick = {
                    navController.navigate(Import)
                },
            )
        }

        composable<Import> {
            ImportRoute(
                onBack = { navController.popBackStack() },
            )
        }

        composable<TransactionEdit> {
            TransactionEditRoute(
                onBack = { navController.popBackStack() },
            )
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

        composable<Statistics> {
            StatisticsRoute()
        }

        composable<AccountList> {
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

        composable<Settings> {
            SettingsRoute(
                onCategoriesClick = {
                    navController.navigate(CategoryList)
                },
            )
        }
    }
}
