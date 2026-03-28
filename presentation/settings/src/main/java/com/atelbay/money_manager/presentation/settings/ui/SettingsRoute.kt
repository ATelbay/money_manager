package com.atelbay.money_manager.presentation.settings.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun SettingsRoute(
    onCategoriesClick: () -> Unit,
    onCurrencyPickerClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onRecurringClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = MoneyManagerTheme.strings
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.shareIntent.collect { intent ->
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    SettingsScreen(
        state = state,
        onThemeModeChange = viewModel::setThemeMode,
        onLanguageChange = viewModel::setLanguage,
        onRefreshRateClick = { viewModel.refreshExchangeRate(strings) },
        onCategoriesClick = onCategoriesClick,
        onCurrencyPickerClick = onCurrencyPickerClick,
        onSignInClick = onSignInClick,
        onRetrySyncClick = viewModel::retrySync,
        onExportCsvClick = { viewModel.exportCsv(context, strings) },
        onBudgetsClick = onBudgetsClick,
        onRecurringClick = onRecurringClick,
        modifier = modifier,
    )
}
