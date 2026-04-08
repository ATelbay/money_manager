package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.animation.animateColorAsState
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import com.atelbay.money_manager.core.model.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onRefreshRateClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onCurrencyPickerClick: () -> Unit,
    onSignInClick: () -> Unit,
    onRetrySyncClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onBudgetsClick: () -> Unit,
    onRecurringClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    Scaffold(
        modifier = modifier.testTag("settings:screen"),
        contentWindowInsets = contentWindowInsets,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.settingsTitle,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionHeader(s.sectionAccount)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    val accountTitle = state.currentUser?.displayName
                        ?: state.currentUser?.email
                        ?: s.signInWithGoogle
                    val accountSubtitle = if (state.currentUser != null) {
                        state.currentUser.email
                    } else {
                        s.syncData
                    }
                    SettingRow(
                        icon = Icons.Default.AccountCircle,
                        iconColor = Teal,
                        title = accountTitle,
                        subtitle = accountSubtitle,
                        hasChevron = true,
                        onClick = onSignInClick,
                        modifier = Modifier.testTag(
                            if (state.currentUser == null) "settings:signIn" else "settings:accountRow",
                        ),
                    )

                    if (state.currentUser != null) {
                        val syncColor = when (state.syncStatus) {
                            is SyncStatus.Syncing -> Color(0xFFFBBF24)
                            is SyncStatus.Synced -> Color(0xFF4ADE80)
                            is SyncStatus.Failed -> Color(0xFFF87171)
                            is SyncStatus.Idle -> colors.textSecondary
                        }
                        val syncTitle = when (state.syncStatus) {
                            is SyncStatus.Syncing -> s.syncing
                            is SyncStatus.Synced -> s.synced
                            is SyncStatus.Failed -> s.syncError
                            is SyncStatus.Idle -> s.syncData
                        }
                        val syncSubtitle = when {
                            state.lastSyncDisplay.isNotEmpty() -> s.updatedAt(state.lastSyncDisplay)
                            state.syncStatus is SyncStatus.Failed -> s.syncRetryHint
                            else -> null
                        }
                        val syncIcon = if (state.syncStatus is SyncStatus.Failed) {
                            Icons.Default.SyncProblem
                        } else {
                            Icons.Default.Sync
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = colors.borderSubtle,
                        )
                        SettingRow(
                            icon = syncIcon,
                            iconColor = syncColor,
                            title = syncTitle,
                            subtitle = syncSubtitle,
                            onClick = if (state.syncStatus is SyncStatus.Failed) onRetrySyncClick else null,
                            modifier = Modifier.testTag("settings:syncStatus"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(s.sectionGeneral)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingRow(
                        icon = Icons.Default.GridView,
                        iconColor = Teal,
                        title = s.categoriesManagement,
                        subtitle = s.categoriesSubtitle,
                        hasChevron = true,
                        onClick = onCategoriesClick,
                        modifier = Modifier.testTag("settings:categories"),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    SettingRow(
                        icon = Icons.Default.AttachMoney,
                        iconColor = Color(0xFFFBBF24),
                        title = s.rateLabel,
                        subtitle = when {
                            !state.hasRateSnapshot -> s.rateNotLoaded
                            !state.rateDisplay.isNullOrBlank() -> state.rateDisplay
                            else -> null
                        },
                        hasChevron = true,
                        onClick = onCurrencyPickerClick,
                        modifier = Modifier.testTag("settings:exchangeRate"),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    val refreshTitle = if (state.lastUpdatedDisplay.isNotEmpty()) {
                        s.rateAsOf(state.lastUpdatedDisplay)
                    } else {
                        s.loadRate
                    }
                    SettingRow(
                        icon = Icons.Default.Refresh,
                        iconColor = Teal,
                        title = refreshTitle,
                        subtitle = if (state.isRefreshingRate) {
                            s.updatingRate
                        } else {
                            s.loadRateSubtitle
                        },
                        onClick = onRefreshRateClick,
                        modifier = Modifier.testTag("settings:refreshRate"),
                    )

                    if (state.rateErrorMessage != null) {
                        Text(
                            text = s.rateError,
                            style = typography.caption,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("settings:rateError"),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    SettingRow(
                        icon = Icons.Default.PieChart,
                        iconColor = Color(0xFF34D399),
                        title = s.budgets,
                        hasChevron = true,
                        onClick = onBudgetsClick,
                        modifier = Modifier.testTag("settings:budgets"),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    SettingRow(
                        icon = Icons.Default.Repeat,
                        iconColor = Color(0xFF818CF8),
                        title = s.recurringTransactions,
                        hasChevron = true,
                        onClick = onRecurringClick,
                        modifier = Modifier.testTag("settings:recurring"),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    SettingRow(
                        icon = Icons.Default.FileDownload,
                        iconColor = Color(0xFF60A5FA),
                        title = s.exportCsv,
                        subtitle = if (state.isExporting) s.exporting else null,
                        onClick = onExportCsvClick,
                        modifier = Modifier.testTag("settings:exportCsv"),
                    )

                    if (state.exportError != null) {
                        Text(
                            text = state.exportError,
                            style = typography.caption,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("settings:exportError"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(s.sectionAppearance)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = s.themeLabel,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    ThemeSelector(
                        selected = state.themeMode,
                        onSelect = onThemeModeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings:themeSelector"),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = s.languageLabel,
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    LanguageSelector(
                        selected = state.appLanguage,
                        onSelect = onLanguageChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings:languageSelector"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(s.sectionAbout)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingRow(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFFA78BFA),
                        title = s.versionLabel,
                        rightText = state.appVersion.ifEmpty { "1.0.0" },
                        modifier = Modifier.testTag("settings:version"),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    SettingRow(
                        icon = Icons.Default.Code,
                        iconColor = Teal,
                        title = s.developerLabel,
                        rightText = "ATelbay",
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ── Section Header ──

@Composable
private fun SectionHeader(title: String) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Text(
        text = title.uppercase(),
        style = typography.caption,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

// ── Setting Row ──

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    rightText: String? = null,
    hasChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val alpha = if (enabled) 1f else 0.6f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f * alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor.copy(alpha = alpha),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.cardTitle,
                color = colors.textPrimary.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typography.caption,
                    color = colors.textSecondary.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (rightText != null) {
            Text(
                text = rightText,
                style = typography.cardTitle,
                color = colors.textSecondary,
            )
        }

        if (hasChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = alpha),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Theme Selector ──

@Composable
private fun ThemeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    data class ThemeOption(
        val mode: ThemeMode,
        val label: String,
        val icon: ImageVector,
    )

    val s = MoneyManagerTheme.strings
    val options = listOf(
        ThemeOption(ThemeMode.SYSTEM, s.themeSystem, Icons.Default.Smartphone),
        ThemeOption(ThemeMode.LIGHT, s.themeLight, Icons.Default.LightMode),
        ThemeOption(ThemeMode.DARK, s.themeDark, Icons.Default.DarkMode),
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = selected == option.mode

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Teal else Color.Transparent,
                animationSpec = MoneyManagerMotion.ColorTransition,
                label = "themeBg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else colors.textSecondary,
                animationSpec = MoneyManagerMotion.ColorTransition,
                label = "themeContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onSelect(option.mode) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.label,
                        style = MoneyManagerTheme.typography.caption,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

// ── Language Selector ──

@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppLanguage.entries.forEach { language ->
            val isSelected = selected == language

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Teal else Color.Transparent,
                animationSpec = MoneyManagerMotion.ColorTransition,
                label = "langBg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else colors.textSecondary,
                animationSpec = MoneyManagerMotion.ColorTransition,
                label = "langContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onSelect(language) }
                    .testTag("settings:language_${language.code}"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = language.displayName,
                        style = MoneyManagerTheme.typography.caption,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun SettingsScreenPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        SettingsScreen(
            state = SettingsState(
                baseCurrency = SupportedCurrencies.fromCode("USD"),
                rateDisplay = "1 USD = 512.34 KZT",
                lastUpdatedDisplay = "03.03.2026 08:15",
                themeMode = ThemeMode.DARK,
                appVersion = "1.0.0",
            ),
            onThemeModeChange = {},
            onLanguageChange = {},
            onRefreshRateClick = {},
            onCategoriesClick = {},
            onCurrencyPickerClick = {},
            onSignInClick = {},
            onRetrySyncClick = {},
            onExportCsvClick = {},
            onBudgetsClick = {},
            onRecurringClick = {},
        )
    }
}
