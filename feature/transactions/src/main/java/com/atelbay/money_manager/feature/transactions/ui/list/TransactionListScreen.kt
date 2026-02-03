package com.atelbay.money_manager.feature.transactions.ui.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.components.MoneyManagerCard
import com.atelbay.money_manager.core.ui.theme.ExpenseColor
import com.atelbay.money_manager.core.ui.theme.IncomeColor
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import kotlinx.collections.immutable.persistentListOf
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onTabSelected: (TransactionType?) -> Unit,
    onPeriodSelected: (Period) -> Unit,
    onCustomDateRange: (LocalDate, LocalDate) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("transactionList:screen"),
        topBar = {
            TopAppBar(
                title = { Text("Money Manager") },
                actions = {
                    IconButton(
                        onClick = onImportClick,
                        modifier = Modifier.testTag("transactionList:importButton"),
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = "Импорт выписки",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.testTag("transactionList:fab"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить транзакцию")
            }
        },
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

        val tabIndex = when (state.selectedTab) {
            null -> 0
            TransactionType.EXPENSE -> 1
            TransactionType.INCOME -> 2
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("transactionList:list"),
        ) {
            // Balance card
            item(key = "balance") {
                BalanceCard(
                    balance = state.balance,
                    currency = state.currency,
                    accountName = state.selectedAccountName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Search bar
            item(key = "search") {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск по заметке или категории") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .testTag("transactionList:searchBar"),
                )
            }

            // Tab row
            stickyHeader(key = "tabs") {
                PrimaryTabRow(
                    selectedTabIndex = tabIndex,
                    modifier = Modifier.testTag("transactionList:tabRow"),
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { onTabSelected(null) },
                        text = { Text("Все") },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { onTabSelected(TransactionType.EXPENSE) },
                        text = { Text("Расходы") },
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { onTabSelected(TransactionType.INCOME) },
                        text = { Text("Доходы") },
                    )
                }
            }

            // Period filter chips
            item(key = "period") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("transactionList:periodFilter"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val periods = listOf(
                        Period.TODAY to "Сегодня",
                        Period.WEEK to "Неделя",
                        Period.MONTH to "Месяц",
                        Period.YEAR to "Год",
                    )
                    periods.forEach { (period, label) ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                            label = { Text(label) },
                        )
                    }
                    FilterChip(
                        selected = state.selectedPeriod == Period.CUSTOM,
                        onClick = { showDateRangePicker = true },
                        label = { Text("Период") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            // Summary card
            item(key = "summary") {
                SummaryCard(
                    income = state.periodIncome,
                    expense = state.periodExpense,
                    currency = state.currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            }

            // Transaction list or empty state
            if (state.transactions.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.searchQuery.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Попробуйте изменить запрос",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Text(
                                text = "Нет транзакций за период",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = state.transactions,
                    key = { it.id },
                ) { transaction ->
                    SwipeToDeleteItem(
                        onDelete = { onDeleteTransaction(transaction.id) },
                    ) {
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("transactionList:item_${transaction.id}"),
                        )
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { start, end ->
                onCustomDateRange(start, end)
                showDateRangePicker = false
            },
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    currency: String,
    accountName: String?,
    modifier: Modifier = Modifier,
) {
    MoneyManagerCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = accountName ?: "Все счета",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatAmount(balance)} $currency",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("transactionList:balance"),
            )
        }
    }
}

@Composable
private fun SummaryCard(
    income: Double,
    expense: Double,
    currency: String,
    modifier: Modifier = Modifier,
) {
    MoneyManagerCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Доходы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "+${formatAmount(income)} $currency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IncomeColor,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Расходы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "-${formatAmount(expense)} $currency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExpenseColor,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val total = income - expense
            val totalColor = when {
                total > 0 -> IncomeColor
                total < 0 -> ExpenseColor
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Итого",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${if (total >= 0) "+" else ""}${formatAmount(total)} $currency",
                    style = MaterialTheme.typography.titleSmall,
                    color = totalColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        val start = Instant.ofEpochMilli(startMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val end = Instant.ofEpochMilli(endMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onConfirm(start, end)
                    }
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { Text("Выберите период", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            modifier = Modifier.height(500.dp),
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) ExpenseColor else IncomeColor
    val sign = if (isExpense) "-" else "+"

    MoneyManagerCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(transaction.categoryColor).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = transaction.categoryName.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(transaction.categoryColor),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val note = transaction.note
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatDate(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        content()
    }
}

private val amountFormat = DecimalFormat("#,##0.##")
private val dateFormat = SimpleDateFormat("dd MMM", Locale("ru"))

private fun formatAmount(amount: Double): String = amountFormat.format(amount)
private fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

@Preview(showBackground = true)
@Composable
private fun TransactionListScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        TransactionListScreen(
            state = TransactionListState(
                balance = 150_000.0,
                currency = "KZT",
                isLoading = false,
                periodIncome = 50_000.0,
                periodExpense = 30_000.0,
                transactions = persistentListOf(
                    Transaction(
                        id = 1,
                        amount = 5000.0,
                        type = TransactionType.EXPENSE,
                        categoryId = 1,
                        categoryName = "Еда",
                        categoryIcon = "restaurant",
                        categoryColor = 0xFFE57373,
                        accountId = 1,
                        note = "Обед в кафе",
                        date = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                    ),
                    Transaction(
                        id = 2,
                        amount = 200_000.0,
                        type = TransactionType.INCOME,
                        categoryId = 2,
                        categoryName = "Зарплата",
                        categoryIcon = "payments",
                        categoryColor = 0xFF81C784,
                        accountId = 1,
                        note = null,
                        date = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                    ),
                ),
            ),
            onTransactionClick = {},
            onAddClick = {},
            onImportClick = {},
            onDeleteTransaction = {},
            onTabSelected = {},
            onPeriodSelected = {},
            onCustomDateRange = { _, _ -> },
            onSearchQueryChange = {},
        )
    }
}
