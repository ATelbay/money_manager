package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

private enum class CurrencySelectionTarget {
    BASE,
    TARGET,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerScreen(
    baseCurrency: SupportedCurrency,
    targetCurrency: SupportedCurrency,
    onBaseCurrencySelect: (SupportedCurrency) -> Unit,
    onTargetCurrencySelect: (SupportedCurrency) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    var searchQuery by remember { mutableStateOf("") }
    var selectionTarget by remember { mutableStateOf(CurrencySelectionTarget.BASE) }
    val filteredOptions = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) SupportedCurrencies.all
        else SupportedCurrencies.all.filter { c ->
            c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
        }
    }

    Scaffold(
        modifier = modifier.testTag("currencyPicker:screen"),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Валютная пара",
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("currencyPicker:back"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = colors.textPrimary,
                        )
                    }
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
                .padding(horizontal = 16.dp),
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currencyPicker:summary"),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Текущая пара",
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${baseCurrency.code} / ${targetCurrency.code}",
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${baseCurrency.name} -> ${targetCurrency.name}",
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CurrencyTargetButton(
                    title = "Базовая",
                    currency = baseCurrency,
                    isSelected = selectionTarget == CurrencySelectionTarget.BASE,
                    onClick = { selectionTarget = CurrencySelectionTarget.BASE },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currencyPicker:baseTarget"),
                )
                CurrencyTargetButton(
                    title = "Целевая",
                    currency = targetCurrency,
                    isSelected = selectionTarget == CurrencySelectionTarget.TARGET,
                    onClick = { selectionTarget = CurrencySelectionTarget.TARGET },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currencyPicker:targetTarget"),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MoneyManagerTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Поиск валюты",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                tag = "currencyPicker:search",
            )

            LazyColumn {
                itemsIndexed(filteredOptions) { index, currency ->
                    val isSelected = when (selectionTarget) {
                        CurrencySelectionTarget.BASE -> baseCurrency.code == currency.code
                        CurrencySelectionTarget.TARGET -> targetCurrency.code == currency.code
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                when (selectionTarget) {
                                    CurrencySelectionTarget.BASE -> onBaseCurrencySelect(currency)
                                    CurrencySelectionTarget.TARGET -> onTargetCurrencySelect(currency)
                                }
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                            .testTag("currencyPicker:currency${currency.code}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${currency.code} · ${currency.name}",
                            style = typography.cardTitle,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Teal,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (index < filteredOptions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            thickness = 0.5.dp,
                            color = colors.borderSubtle,
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
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) Teal.copy(alpha = 0.22f) else colors.glassBgStart.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 200),
        label = "currencyTargetContainer",
    )

    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = containerColor,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = typography.caption,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currency.code,
                style = typography.cardTitle,
                color = colors.textPrimary,
            )
        }
    }
}
