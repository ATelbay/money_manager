package com.atelbay.money_manager.presentation.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerScreen(
    baseCurrency: SupportedCurrency,
    targetCurrency: SupportedCurrency,
    activeSide: CurrencyPickerSide,
    onSideChange: (CurrencyPickerSide) -> Unit,
    onSelect: (SupportedCurrency) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    var searchQuery by remember { mutableStateOf("") }
    val filteredOptions = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) SupportedCurrencies.all
        else SupportedCurrencies.all.filter { c ->
            c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
        }
    }
    val selected = if (activeSide == CurrencyPickerSide.FIRST) baseCurrency else targetCurrency

    Scaffold(
        modifier = modifier.testTag("currencyPicker:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.currencyPair,
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
                            contentDescription = s.back,
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
                    .padding(bottom = 12.dp)
                    .testTag("currencyPicker:pairSummary"),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = s.currentPair,
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "${baseCurrency.code} · ${targetCurrency.code}",
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        SideChip(
                            label = baseCurrency.code,
                            selected = activeSide == CurrencyPickerSide.FIRST,
                            onClick = { onSideChange(CurrencyPickerSide.FIRST) },
                            modifier = Modifier.testTag("currencyPicker:sideFirst"),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SideChip(
                            label = targetCurrency.code,
                            selected = activeSide == CurrencyPickerSide.SECOND,
                            onClick = { onSideChange(CurrencyPickerSide.SECOND) },
                            modifier = Modifier.testTag("currencyPicker:sideSecond"),
                        )
                    }
                }
            }

            MoneyManagerTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = s.searchCurrency,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(currency) }
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
                        if (selected.code == currency.code) {
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
private fun SideChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Teal.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Teal else colors.textSecondary,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = typography.caption,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
