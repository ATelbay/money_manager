package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBaseCurrencyChange: (SupportedCurrency) -> Unit,
    onTargetCurrencyChange: (SupportedCurrency) -> Unit,
    onRefreshRateClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    var isCurrencySheetVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("settings:screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                actions = {
                    IconButton(
                        onClick = { isCurrencySheetVisible = true },
                        modifier = Modifier.testTag("settings:currencySheetAction"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "Выбрать валюты",
                            tint = colors.textPrimary,
                        )
                    }
                },
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

            // ── ОБЩЕЕ ──
            SectionHeader("Общее")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingRow(
                        icon = Icons.Default.GridView,
                        iconColor = Teal,
                        title = "Категории",
                        subtitle = "Управление категориями",
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
                        title = "Курс USD/KZT",
                        subtitle = state.rateDisplay.ifEmpty { "Курс ещё не загружен" },
                        rightText = if (state.isRefreshingRate) "..." else null,
                        modifier = Modifier.testTag("settings:exchangeRate"),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colors.borderSubtle,
                    )

                    RefreshRateRow(
                        lastUpdatedDisplay = state.lastUpdatedDisplay,
                        isRefreshingRate = state.isRefreshingRate,
                        onRefreshRateClick = onRefreshRateClick,
                        modifier = Modifier.testTag("settings:refreshRate"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CurrencyPairSummaryRow(
                        baseCurrency = state.baseCurrency,
                        targetCurrency = state.targetCurrency,
                        onClick = { isCurrencySheetVisible = true },
                        modifier = Modifier.testTag("settings:currencySummary"),
                    )

                    if (state.rateErrorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.rateErrorMessage,
                            style = typography.caption,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.testTag("settings:rateError"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── ОФОРМЛЕНИЕ ──
            SectionHeader("Оформление")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Тема",
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── О ПРИЛОЖЕНИИ ──
            SectionHeader("О приложении")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingRow(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFFA78BFA),
                        title = "Версия",
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
                        title = "Разработчик",
                        rightText = "ATelbay",
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        if (isCurrencySheetVisible) {
            CurrencyPickerBottomSheet(
                baseCurrency = state.baseCurrency,
                targetCurrency = state.targetCurrency,
                onBaseCurrencyChange = onBaseCurrencyChange,
                onTargetCurrencyChange = onTargetCurrencyChange,
                onDismiss = { isCurrencySheetVisible = false },
            )
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

// ── Currency Summary ──

@Composable
private fun CurrencyPairSummaryRow(
    baseCurrency: SupportedCurrency,
    targetCurrency: SupportedCurrency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(modifier = modifier) {
        Text(
            text = "Валютная пара",
            style = typography.caption,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.glassBgStart.copy(alpha = 0.32f))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Базовая: ${baseCurrency.code} · ${baseCurrency.name}",
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Целевая: ${targetCurrency.code} · ${targetCurrency.name}",
                    style = typography.caption,
                    color = colors.textSecondary,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private enum class CurrencySelectionTarget {
    BASE,
    TARGET,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    baseCurrency: SupportedCurrency,
    targetCurrency: SupportedCurrency,
    onBaseCurrencyChange: (SupportedCurrency) -> Unit,
    onTargetCurrencyChange: (SupportedCurrency) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    var query by rememberSaveable { mutableStateOf("") }
    var selectionTarget by rememberSaveable { mutableStateOf(CurrencySelectionTarget.BASE) }
    val filteredCurrencies = SupportedCurrencies.all.filter { currency ->
        query.isBlank() ||
            currency.code.contains(query, ignoreCase = true) ||
            currency.name.contains(query, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.glassBgEnd,
        modifier = modifier.testTag("settings:currencySheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Выбор валют",
                style = typography.sectionHeader,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Выберите поле, затем найдите валюту по коду или названию",
                style = typography.caption,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CurrencyTargetButton(
                    title = "Базовая",
                    currency = baseCurrency,
                    isSelected = selectionTarget == CurrencySelectionTarget.BASE,
                    onClick = { selectionTarget = CurrencySelectionTarget.BASE },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings:currencySheetBaseTarget"),
                )
                CurrencyTargetButton(
                    title = "Целевая",
                    currency = targetCurrency,
                    isSelected = selectionTarget == CurrencySelectionTarget.TARGET,
                    onClick = { selectionTarget = CurrencySelectionTarget.TARGET },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings:currencySheetTargetTarget"),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MoneyManagerTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Поиск по коду или названию",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                tag = "settings:currencySearch",
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filteredCurrencies.isEmpty()) {
                    item {
                        Text(
                            text = "Ничего не найдено",
                            style = typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                } else {
                    items(filteredCurrencies, key = { it.code }) { currency ->
                        CurrencySheetItem(
                            currency = currency,
                            isSelected = when (selectionTarget) {
                                CurrencySelectionTarget.BASE -> currency.code == baseCurrency.code
                                CurrencySelectionTarget.TARGET -> currency.code == targetCurrency.code
                            },
                            onClick = {
                                when (selectionTarget) {
                                    CurrencySelectionTarget.BASE -> onBaseCurrencyChange(currency)
                                    CurrencySelectionTarget.TARGET -> onTargetCurrencyChange(currency)
                                }
                            },
                            modifier = Modifier.testTag(
                                "settings:currencySheet${currency.code.lowercase().replaceFirstChar { it.uppercase() }}",
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyTargetButton(
    title: String,
    currency: SupportedCurrency,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Teal.copy(alpha = 0.16f)
                else colors.glassBgStart.copy(alpha = 0.24f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = typography.caption,
            color = if (isSelected) Teal else colors.textSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currency.code,
            style = typography.cardTitle,
            color = colors.textPrimary,
        )
        Text(
            text = currency.name,
            style = typography.caption,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun CurrencySheetItem(
    currency: SupportedCurrency,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Teal.copy(alpha = 0.14f)
                else colors.glassBgStart.copy(alpha = 0.2f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.code,
                style = typography.cardTitle,
                color = colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currency.name,
                style = typography.caption,
                color = colors.textSecondary,
            )
        }

        if (isSelected) {
            Text(
                text = "Выбрано",
                style = typography.caption,
                color = Teal,
            )
        }
    }
}

// ── Refresh Rate Row ──

@Composable
private fun RefreshRateRow(
    lastUpdatedDisplay: String,
    isRefreshingRate: Boolean,
    onRefreshRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val title = "Курс на ${lastUpdatedDisplay.ifEmpty { "--.--.---- --:--" }}"
    val subtitle = if (isRefreshingRate) {
        "Выполняется обновление"
    } else {
        "Загрузить актуальный курс НБК"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Teal.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Teal,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.cardTitle,
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        IconButton(
            onClick = onRefreshRateClick,
            enabled = !isRefreshingRate,
            modifier = Modifier.testTag("settings:refreshRateAction"),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = if (isRefreshingRate) {
                    colors.textSecondary.copy(alpha = 0.6f)
                } else {
                    Teal
                },
            )
        }
    }
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
        // Icon container
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

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.cardTitle,
                color = colors.textPrimary.copy(alpha = alpha),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typography.caption,
                    color = colors.textSecondary.copy(alpha = alpha),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Right text
        if (rightText != null) {
            Text(
                text = rightText,
                style = typography.cardTitle,
                color = colors.textSecondary,
            )
        }

        // Chevron
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

    val options = listOf(
        ThemeOption(ThemeMode.SYSTEM, "Система", Icons.Default.Smartphone),
        ThemeOption(ThemeMode.LIGHT, "Светлая", Icons.Default.LightMode),
        ThemeOption(ThemeMode.DARK, "Тёмная", Icons.Default.DarkMode),
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = selected == option.mode

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Teal else Color.Transparent,
                animationSpec = tween(250),
                label = "themeBg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else colors.textSecondary,
                animationSpec = tween(250),
                label = "themeContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .then(
                        if (!isSelected) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Transparent)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(option.mode) },
                contentAlignment = Alignment.Center,
            ) {
                // Border for unselected
                if (!isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                            .padding(0.dp),
                    )
                }

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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun SettingsScreenPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        SettingsScreen(
            state = SettingsState(
                baseCurrency = SupportedCurrencies.fromCode("USD"),
                targetCurrency = SupportedCurrencies.fromCode("EUR"),
                rateDisplay = "1 USD = 512.34 KZT",
                lastUpdatedDisplay = "03.03.2026 08:15",
                themeMode = ThemeMode.DARK,
                appVersion = "1.0.0",
            ),
            onThemeModeChange = {},
            onBaseCurrencyChange = {},
            onTargetCurrencyChange = {},
            onRefreshRateClick = {},
            onCategoriesClick = {},
        )
    }
}
