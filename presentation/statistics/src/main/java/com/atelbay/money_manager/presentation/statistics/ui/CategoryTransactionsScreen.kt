package com.atelbay.money_manager.presentation.statistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.TransactionListItem
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.domain.statistics.model.StatsPeriod
import com.atelbay.money_manager.domain.statistics.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    state: CategoryTransactionsState,
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings
    val timestampFormatter = remember(s.locale) { SimpleDateFormat("dd MMM yyyy, HH:mm", s.locale) }
    val rangeFormatter = remember(s.locale) { SimpleDateFormat("dd MMM yyyy", s.locale) }

    Scaffold(
        modifier = modifier.testTag("statistics:categoryTransactions:screen"),
        contentWindowInsets = contentWindowInsets,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.categoryName.ifBlank { s.byCategory },
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.chart1)
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("statistics:categoryTransactions:error"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error,
                        style = typography.cardTitle,
                        color = colors.textSecondary,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("statistics:categoryTransactions:list"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        CategoryTransactionsHeader(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            rangeLabel = formatDateRange(
                                startMillis = state.startMillis,
                                endMillis = state.endMillis,
                                formatter = rangeFormatter,
                            ),
                        )
                    }

                    if (state.isEmpty) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(240.dp)
                                    .testTag("statistics:categoryTransactions:empty"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = s.noDataForPeriod,
                                    style = typography.cardTitle,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    } else {
                        items(
                            items = state.transactions,
                            key = { it.transactionId },
                        ) { transaction ->
                            TransactionListItem(
                                description = transaction.description,
                                category = transaction.categoryName,
                                categoryIcon = categoryIconFromName(transaction.categoryIcon),
                                categoryColor = Color(transaction.categoryColor),
                                amount = transaction.amount,
                                date = timestampFormatter.format(Date(transaction.date)),
                                isIncome = transaction.isIncome,
                                currency = transaction.currency,
                                onClick = { onTransactionClick(transaction.transactionId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("statistics:categoryTransactions:item_${transaction.transactionId}"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTransactionsHeader(
    state: CategoryTransactionsState,
    rangeLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings
    val contextColor = when (state.transactionType) {
        TransactionType.EXPENSE -> colors.expenseForeground
        TransactionType.INCOME -> colors.incomeForeground
    }
    val contextBackground = contextColor.copy(alpha = 0.12f)

    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(state.categoryColor).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIconFromName(state.categoryIcon),
                        contentDescription = state.categoryName,
                        tint = Color(state.categoryColor),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.categoryName,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = rangeLabel,
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ContextBadge(
                    text = when (state.transactionType) {
                        TransactionType.EXPENSE -> s.filterExpenses
                        TransactionType.INCOME -> s.filterIncome
                    },
                    containerColor = contextBackground,
                    contentColor = contextColor,
                )
                ContextBadge(
                    text = when (state.period) {
                        StatsPeriod.WEEK -> s.periodWeek
                        StatsPeriod.MONTH -> s.periodMonth
                        StatsPeriod.YEAR -> s.periodYear
                    },
                    containerColor = colors.chart1.copy(alpha = 0.12f),
                    contentColor = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun ContextBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = contentColor,
        style = MoneyManagerTheme.typography.caption,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun formatDateRange(
    startMillis: Long,
    endMillis: Long,
    formatter: SimpleDateFormat,
): String {
    val start = formatter.format(Date(startMillis))
    val end = formatter.format(Date(endMillis))
    return if (start == end) start else "$start - $end"
}
