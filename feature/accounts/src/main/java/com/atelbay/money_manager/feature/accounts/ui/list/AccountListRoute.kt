package com.atelbay.money_manager.feature.accounts.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountListRoute(
    onAccountClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AccountListScreen(
        state = state,
        onAccountClick = onAccountClick,
        onAddClick = onAddClick,
        onSelectAccount = viewModel::selectAccount,
        onDeleteAccount = viewModel::deleteAccount,
        modifier = modifier,
    )
}
