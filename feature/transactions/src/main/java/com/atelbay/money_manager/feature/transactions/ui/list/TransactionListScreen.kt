package com.atelbay.money_manager.feature.transactions.ui.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.ui.components.BalanceCard
import com.atelbay.money_manager.core.ui.components.ChipType
import com.atelbay.money_manager.core.ui.components.IncomeExpenseCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerChip
import com.atelbay.money_manager.core.ui.components.MoneyManagerFAB
import com.atelbay.money_manager.core.ui.components.MoneyManagerTextField
import com.atelbay.money_manager.core.ui.components.TransactionListItem
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import kotlinx.collections.immutable.persistentListOf
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
    val colors = MoneyManagerTheme.colors
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("transactionList:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Money Manager",
                        style = MoneyManagerTheme.typography.sectionHeader,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onImportClick,
                        modifier = Modifier.testTag("transactionList:importButton"),
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = "Импорт выписки",
                            tint = colors.textSecondary,
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
                onClick = onAddClick,
                modifier = Modifier.testTag("transactionList:fab"),
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
                CircularProgressIndicator(color = colors.chart1)
            }
            return@Scaffold
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
                    accountName = state.selectedAccountName ?: "Все счета",
                    balance = state.balance,
                    currency = state.currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("transactionList:balance"),
                )
            }

            // Income/Expense summary
            item(key = "summary") {
                IncomeExpenseCard(
                    income = state.periodIncome,
                    expense = state.periodExpense,
                    currency = state.currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            }

            // Search bar
            item(key = "search") {
                MoneyManagerTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "Поиск",
                    maxLines = 1,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = colors.textSecondary,
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .testTag("transactionList:searchBar"),
                    tag = "transactionList:searchField",
                )
            }

            // Type filter chips
            item(key = "typeFilter") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp)
                        .testTag("transactionList:tabRow"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MoneyManagerChip(
                        label = "Все",
                        selected = state.selectedTab == null,
                        onClick = { onTabSelected(null) },
                    )
                    MoneyManagerChip(
                        label = "Расходы",
                        selected = state.selectedTab == TransactionType.EXPENSE,
                        onClick = { onTabSelected(TransactionType.EXPENSE) },
                        type = ChipType.EXPENSE,
                    )
                    MoneyManagerChip(
                        label = "Доходы",
                        selected = state.selectedTab == TransactionType.INCOME,
                        onClick = { onTabSelected(TransactionType.INCOME) },
                        type = ChipType.INCOME,
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
                        Period.ALL to "Все",
                        Period.TODAY to "Сегодня",
                        Period.WEEK to "Неделя",
                        Period.MONTH to "Месяц",
                        Period.YEAR to "Год",
                    )
                    periods.forEach { (period, label) ->
                        MoneyManagerChip(
                            label = label,
                            selected = state.selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                        )
                    }
                    MoneyManagerChip(
                        label = "Период",
                        selected = state.selectedPeriod == Period.CUSTOM,
                        onClick = { showDateRangePicker = true },
                    )
                }
            }

            // Transaction list or empty state
            if (state.transactions.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        isSearchActive = state.searchQuery.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    )
                }
            } else {
                // Group transactions by date
                val grouped = state.transactions.groupBy { formatDateHeader(it.date) }

                grouped.forEach { (dateHeader, transactions) ->
                    // Date section header
                    item(key = "header_$dateHeader") {
                        Text(
                            text = dateHeader,
                            style = MoneyManagerTheme.typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                        )
                    }

                    items(
                        items = transactions,
                        key = { it.id },
                    ) { transaction ->
                        val isExpense = transaction.type == TransactionType.EXPENSE

                        TransactionListItem(
                            description = transaction.note?.ifBlank { transaction.categoryName }
                                ?: transaction.categoryName,
                            category = transaction.categoryName,
                            categoryIcon = categoryIconFromName(transaction.categoryIcon),
                            categoryColor = Color(transaction.categoryColor),
                            amount = transaction.amount,
                            date = formatTime(transaction.date),
                            isIncome = !isExpense,
                            currency = state.currency,
                            onClick = { onTransactionClick(transaction.id) },
                            modifier = Modifier.testTag("transactionList:item_${transaction.id}"),
                        )
                    }
                }
            }

            // Bottom spacing for FAB
            item(key = "spacer") {
                Spacer(modifier = Modifier.height(80.dp))
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
private fun EmptyState(
    isSearchActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isSearchActive) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colors.textTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ничего не найдено",
                    style = MoneyManagerTheme.typography.cardTitle,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Попробуйте изменить запрос",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            } else {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colors.textTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Нет транзакций",
                    style = MoneyManagerTheme.typography.cardTitle,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Добавьте первую транзакцию",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
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
            title = {
                Text(
                    "Выберите период",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            modifier = Modifier.height(500.dp),
        )
    }
}

private val dateHeaderFormat = SimpleDateFormat("dd MMMM", Locale.forLanguageTag("ru"))
private val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("ru"))

private fun formatDateHeader(timestamp: Long): String {
    val txDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    return when (txDate) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> dateHeaderFormat.format(Date(timestamp))
    }
}

private fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

@Preview(showBackground = true)
@Composable
private fun TransactionListScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        TransactionListScreen(
            state = TransactionListState(
                balance = 1_250_000.50,
                currency = "KZT",
                isLoading = false,
                selectedAccountName = "Kaspi Gold",
                periodIncome = 450_000.0,
                periodExpense = 358_400.0,
                transactions = persistentListOf(
                    Transaction(
                        id = 1,
                        amount = 15_480.0,
                        type = TransactionType.EXPENSE,
                        categoryId = 1,
                        categoryName = "Еда",
                        categoryIcon = "restaurant",
                        categoryColor = 0xFF4ECDC4,
                        accountId = 1,
                        note = "Магнум супермаркет",
                        date = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                    ),
                    Transaction(
                        id = 2,
                        amount = 2_450.0,
                        type = TransactionType.EXPENSE,
                        categoryId = 2,
                        categoryName = "Транспорт",
                        categoryIcon = "directions_car",
                        categoryColor = 0xFF45B7D1,
                        accountId = 1,
                        note = "Bolt",
                        date = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                    ),
                    Transaction(
                        id = 3,
                        amount = 450_000.0,
                        type = TransactionType.INCOME,
                        categoryId = 11,
                        categoryName = "Зарплата",
                        categoryIcon = "payments",
                        categoryColor = 0xFF00C9A7,
                        accountId = 1,
                        note = null,
                        date = System.currentTimeMillis() - 86_400_000,
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

@Preview(showBackground = true)
@Composable
private fun TransactionListScreenEmptyPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        TransactionListScreen(
            state = TransactionListState(
                balance = 0.0,
                currency = "KZT",
                isLoading = false,
                selectedAccountName = "Kaspi Gold",
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
