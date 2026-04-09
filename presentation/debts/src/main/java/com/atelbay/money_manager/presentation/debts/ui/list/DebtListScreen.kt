package com.atelbay.money_manager.presentation.debts.ui.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Account
import com.atelbay.money_manager.core.model.Debt
import com.atelbay.money_manager.core.model.DebtDirection
import com.atelbay.money_manager.core.model.DebtStatus
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerFAB
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    state: DebtListState,
    accounts: ImmutableList<Account>,
    onDebtClick: (Long) -> Unit,
    onDeleteDebt: (Long) -> Unit,
    onSaveDebt: (Debt) -> Unit,
    onFilterChange: (DebtFilter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    var showEditSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("debtList:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.debtsTitle,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("debtList:backButton"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
        floatingActionButton = {
            MoneyManagerFAB(
                onClick = { showEditSheet = true },
                icon = Icons.Default.Add,
                contentDescription = s.addDebt,
                testTag = "debtList:fab",
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Teal)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("debtList:list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Summary Card
            if (state.totalLent > 0 || state.totalBorrowed > 0) {
                item {
                    DebtSummaryCard(
                        totalLent = state.totalLent,
                        totalBorrowed = state.totalBorrowed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debtList:summaryCard"),
                    )
                }
            }

            // Filter Chips
            item {
                DebtFilterRow(
                    selectedFilter = state.selectedFilter,
                    onFilterChange = onFilterChange,
                    modifier = Modifier.testTag("debtList:filterRow"),
                )
            }

            if (state.debts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = s.noDebts,
                            style = typography.cardTitle,
                            color = colors.textSecondary,
                            modifier = Modifier.testTag("debtList:emptyState"),
                        )
                    }
                }
            }

            items(
                items = state.debts,
                key = { it.id },
            ) { debt ->
                DebtSwipeToDeleteItem(
                    onDelete = { onDeleteDebt(debt.id) },
                ) {
                    DebtListItem(
                        debt = debt,
                        onClick = { onDebtClick(debt.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debtList:item_${debt.id}"),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showEditSheet) {
        DebtEditBottomSheet(
            debt = null,
            accounts = accounts,
            onSave = { debt ->
                onSaveDebt(debt)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false },
        )
    }
}

@Composable
private fun DebtSummaryCard(
    totalLent: Double,
    totalBorrowed: Double,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = s.totalLent,
                    style = typography.caption,
                    color = colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDebtAmount(totalLent),
                    style = typography.cardTitle,
                    color = colors.incomeForeground,
                    modifier = Modifier.testTag("debtList:totalLent"),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = s.totalBorrowed,
                    style = typography.caption,
                    color = colors.textSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDebtAmount(totalBorrowed),
                    style = typography.cardTitle,
                    color = colors.expenseForeground,
                    modifier = Modifier.testTag("debtList:totalBorrowed"),
                )
            }
        }
    }
}

@Composable
private fun DebtFilterRow(
    selectedFilter: DebtFilter,
    onFilterChange: (DebtFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val filters = listOf(
        DebtFilter.ALL to s.all,
        DebtFilter.LENT to s.iLent,
        DebtFilter.BORROWED to s.iBorrowed,
        DebtFilter.PAID_OFF to s.paidOff,
    )

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters, key = { it.first }) { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Teal.copy(alpha = 0.15f),
                    selectedLabelColor = Teal,
                ),
                modifier = Modifier.testTag("debtList:filter_${filter.name}"),
            )
        }
    }
}

@Composable
private fun DebtListItem(
    debt: Debt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    val directionColor = if (debt.direction == DebtDirection.LENT) {
        colors.incomeForeground
    } else {
        colors.expenseForeground
    }
    val directionLabel = if (debt.direction == DebtDirection.LENT) s.iLent else s.iBorrowed
    val progress = if (debt.totalAmount > 0) {
        (debt.paidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    GlassCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.contactName,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = directionLabel,
                        style = typography.caption,
                        color = directionColor,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDebtAmount(debt.totalAmount),
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                    )
                    if (debt.status == DebtStatus.PAID_OFF) {
                        Text(
                            text = s.debtPaidOff,
                            style = typography.caption,
                            color = Teal,
                        )
                    }
                }
            }

            if (debt.status == DebtStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .testTag("debtList:progressBar_${debt.id}"),
                    color = directionColor,
                    trackColor = colors.borderSubtle,
                    strokeCap = StrokeCap.Round,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = typography.caption,
                        color = directionColor,
                    )
                    Text(
                        text = "${s.remainingAmount}: ${formatDebtAmount(debt.remainingAmount)}",
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtSwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dismissed by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                dismissed = true
                true
            } else {
                false
            }
        },
    )

    LaunchedEffect(dismissed) {
        if (dismissed) onDelete()
    }

    val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Transparent
                },
                label = "swipe_bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (isSwiping) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = MoneyManagerTheme.strings.delete,
                        tint = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        content()
    }
}

private fun formatDebtAmount(amount: Double): String {
    return if (amount >= 1_000_000) {
        "${(amount / 1_000_000).toBigDecimal().stripTrailingZeros().toPlainString()}M"
    } else if (amount >= 1_000) {
        "${(amount / 1_000).toBigDecimal().stripTrailingZeros().toPlainString()}K"
    } else {
        amount.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}
