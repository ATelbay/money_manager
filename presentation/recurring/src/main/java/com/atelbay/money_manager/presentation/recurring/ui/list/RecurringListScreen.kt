package com.atelbay.money_manager.presentation.recurring.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Frequency
import com.atelbay.money_manager.core.model.RecurringTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerFAB
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.defaultMoneyNumberFormat
import com.atelbay.money_manager.core.ui.util.formatAmount
import kotlinx.collections.immutable.ImmutableList
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    state: RecurringListState,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteRecurring: (Long) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val typography = MoneyManagerTheme.typography
    val colors = MoneyManagerTheme.colors

    Scaffold(
        modifier = modifier.testTag("recurringList:screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.recurringTransactions,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("recurringList:back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
        floatingActionButton = {
            MoneyManagerFAB(
                onClick = onAddClick,
                contentDescription = s.newRecurring,
                testTag = "recurringList:fab",
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.recurrings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = s.noRecurringTransactions,
                    style = typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.testTag("recurringList:empty"),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(state.recurrings, key = { it.id }) { recurring ->
                RecurringListItem(
                    recurring = recurring,
                    onEdit = { onEditClick(recurring.id) },
                    onDelete = { onDeleteRecurring(recurring.id) },
                    onToggleActive = { isActive -> onToggleActive(recurring.id, isActive) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringListItem(
    recurring: RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val typography = MoneyManagerTheme.typography
    val colors = MoneyManagerTheme.colors

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

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.testTag("recurringList:item_${recurring.id}"),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = s.delete,
                    tint = Color(0xFFF87171),
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        GlassCard(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val iconColor = Color(recurring.categoryColor)
                    Icon(
                        imageVector = categoryIconFromName(recurring.categoryIcon),
                        contentDescription = recurring.categoryName,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val isIncome = recurring.type == TransactionType.INCOME
                    val amountPrefix = if (isIncome) "+" else "\u2212"
                    val moneyDisplay = remember(recurring.amount, recurring.accountCurrency) {
                        MoneyDisplayFormatter.resolveAndFormat(recurring.accountCurrency)
                    }
                    val formatter = remember { defaultMoneyNumberFormat() }
                    Text(
                        text = moneyDisplay.formatAmount(recurring.amount, amountPrefix, formatter),
                        style = typography.cardTitle,
                        color = if (isIncome) Color(0xFF4ADE80) else Color(0xFFF87171),
                        maxLines = 1,
                    )
                    Text(
                        text = recurring.categoryName,
                        style = typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = frequencyLabel(recurring.frequency, s),
                        style = typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                    val nextDate = remember(recurring) {
                        computeNextOccurrence(recurring)
                    }
                    if (nextDate != null) {
                        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            .format(Date(nextDate))
                        Text(
                            text = "${s.nextDate}: $dateStr",
                            style = typography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    if (!recurring.isActive) {
                        Text(
                            text = s.paused,
                            style = typography.caption,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.testTag("recurringList:paused_${recurring.id}"),
                        )
                    }
                }

                Switch(
                    checked = recurring.isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("recurringList:toggle_${recurring.id}"),
                )
            }
        }
    }
}

private fun frequencyLabel(
    frequency: Frequency,
    s: com.atelbay.money_manager.core.ui.theme.AppStrings,
): String = when (frequency) {
    Frequency.DAILY -> s.daily
    Frequency.WEEKLY -> s.weekly
    Frequency.MONTHLY -> s.monthly
    Frequency.YEARLY -> s.yearly
}

/**
 * Computes the next occurrence date (epoch millis) for a recurring transaction.
 * Returns null if the recurring has ended (endDate passed) or is inactive.
 */
private fun computeNextOccurrence(recurring: RecurringTransaction): Long? {
    if (!recurring.isActive) return null

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()

    // Determine the base date: lastGeneratedDate or startDate
    val baseMillis = recurring.lastGeneratedDate ?: recurring.startDate
    val baseDate = Instant.ofEpochMilli(baseMillis).atZone(zone).toLocalDate()

    val nextDate = if (recurring.lastGeneratedDate == null) {
        // Never generated — next is startDate itself (if today or future)
        val startDate = Instant.ofEpochMilli(recurring.startDate).atZone(zone).toLocalDate()
        if (!startDate.isBefore(today)) startDate else computeNext(recurring, baseDate)
    } else {
        computeNext(recurring, baseDate)
    }

    // Check end date
    if (nextDate == null) return null
    val endDate = recurring.endDate?.let {
        Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
    }
    if (endDate != null && nextDate.isAfter(endDate)) return null

    return nextDate.atStartOfDay(zone).toInstant().toEpochMilli()
}

private fun computeNext(recurring: RecurringTransaction, after: LocalDate): LocalDate? {
    return when (recurring.frequency) {
        Frequency.DAILY -> after.plusDays(1)

        Frequency.WEEKLY -> {
            val targetDow = recurring.dayOfWeek ?: 1
            var next = after.plusDays(1)
            while (next.dayOfWeek.value != targetDow) {
                next = next.plusDays(1)
            }
            next
        }

        Frequency.MONTHLY -> {
            val targetDay = recurring.dayOfMonth ?: 1
            val nextMonth = after.plusMonths(1)
            val yearMonth = YearMonth.of(nextMonth.year, nextMonth.month)
            val clampedDay = minOf(targetDay, yearMonth.lengthOfMonth())
            LocalDate.of(nextMonth.year, nextMonth.month, clampedDay)
        }

        Frequency.YEARLY -> after.plusYears(1)
    }
}
