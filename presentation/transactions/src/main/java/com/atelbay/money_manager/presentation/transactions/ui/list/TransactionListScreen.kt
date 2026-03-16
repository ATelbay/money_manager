package com.atelbay.money_manager.presentation.transactions.ui.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import com.atelbay.money_manager.core.ui.components.LocalAnimatedVisibilityScope
import com.atelbay.money_manager.core.ui.components.LocalSharedTransitionScope
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
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import kotlinx.collections.immutable.persistentListOf
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val TransactionListBottomGutter = 16.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
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
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
) {
    val colors = MoneyManagerTheme.colors
    val s = MoneyManagerTheme.strings
    val locale = s.locale
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val dateHeaderFormat = remember(locale) { SimpleDateFormat("dd MMMM", locale) }
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        modifier = modifier.testTag("transactionList:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = contentWindowInsets,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.appName,
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
                            contentDescription = s.importStatement,
                            tint = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                ),
            )
        },
        floatingActionButton = {
            MoneyManagerFAB(
                
                onClick = onAddClick,
                testTag = "transactionList:fab",
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
                .testTag("transactionList:list"),
            contentPadding = PaddingValues(
                start = padding.calculateLeftPadding(layoutDirection),
                end = padding.calculateRightPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + TransactionListBottomGutter,
            ),
        ) {
            // Balance card
            item(key = "balance") {
                BalanceCard(
                    accountName = state.selectedAccountName ?: s.allAccounts,
                    balance = state.balance ?: 0.0,
                    moneyDisplay = state.summaryMoneyDisplay,
                    unavailableSupportingText = s.mixedCurrencyUnavailable
                        .takeIf { state.summaryDisplayMode == SummaryDisplayMode.UNAVAILABLE },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("transactionList:balance"),
                )
            }

            // Income/Expense summary
            item(key = "summary") {
                IncomeExpenseCard(
                    income = state.periodIncome ?: 0.0,
                    expense = state.periodExpense ?: 0.0,
                    moneyDisplay = state.summaryMoneyDisplay,
                    unavailableSupportingText = s.mixedCurrencyUnavailable
                        .takeIf { state.summaryDisplayMode == SummaryDisplayMode.UNAVAILABLE },
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
                    placeholder = s.search,
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
                                    contentDescription = s.clearSearch,
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
                        label = s.filterAll,
                        selected = state.selectedTab == null,
                        onClick = { onTabSelected(null) },
                    )
                    MoneyManagerChip(
                        label = s.filterExpenses,
                        selected = state.selectedTab == TransactionType.EXPENSE,
                        onClick = { onTabSelected(TransactionType.EXPENSE) },
                        type = ChipType.EXPENSE,
                    )
                    MoneyManagerChip(
                        label = s.filterIncome,
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
                        Period.ALL to s.filterAll,
                        Period.TODAY to s.periodToday,
                        Period.WEEK to s.periodWeek,
                        Period.MONTH to s.periodMonth,
                        Period.YEAR to s.periodYear,
                    )
                    periods.forEach { (period, label) ->
                        MoneyManagerChip(
                            label = label,
                            selected = state.selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                        )
                    }
                    MoneyManagerChip(
                        label = s.periodCustom,
                        selected = state.selectedPeriod == Period.CUSTOM,
                        onClick = { showDateRangePicker = true },
                    )
                }
            }

            // Transaction list or empty state
            if (state.transactionRows.isEmpty()) {
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
                val grouped = state.transactionRows.groupBy { formatDateHeader(it.transaction.date, s.periodToday, s.periodYesterday, dateHeaderFormat) }

                grouped.forEach { (dateHeader, transactionRows) ->
                    // Date section header
                    item(key = "header_$dateHeader") {
                        Text(
                            text = dateHeader,
                            style = MoneyManagerTheme.typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                        )
                    }

                    items(
                        items = transactionRows,
                        key = { it.transaction.id },
                    ) { row ->
                        val transaction = row.transaction
                        val isExpense = transaction.type == TransactionType.EXPENSE
                        val isShowingConvertedAmount =
                            row.conversionStatus == ConversionStatus.AVAILABLE &&
                                row.convertedAmount != null &&
                                row.convertedCurrency != null

                        val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState("tx_${transaction.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                        } else Modifier

                        TransactionListItem(
                            description = transaction.note?.ifBlank { transaction.categoryName }
                                ?: transaction.categoryName,
                            category = transaction.categoryName,
                            categoryIcon = categoryIconFromName(transaction.categoryIcon),
                            categoryColor = Color(transaction.categoryColor),
                            amount = row.displayAmount,
                            date = formatTime(transaction.date, timeFormat),
                            isIncome = !isExpense,
                            moneyDisplay = row.displayMoneyDisplay,
                            secondaryAmount = row.originalAmount.takeIf { isShowingConvertedAmount },
                            secondaryMoneyDisplay = row.secondaryMoneyDisplay.takeIf { isShowingConvertedAmount },
                            secondaryAmountLabel = s.originalAmount.takeIf {
                                isShowingConvertedAmount
                            },
                            onClick = { onTransactionClick(transaction.id) },
                            modifier = sharedModifier
                                .animateItem(
                                    fadeInSpec = MoneyManagerMotion.ItemFadeInSpec,
                                    placementSpec = MoneyManagerMotion.ItemPlacementSpec,
                                    fadeOutSpec = MoneyManagerMotion.ItemFadeOutSpec,
                                )
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
            val s = MoneyManagerTheme.strings
            if (isSearchActive) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colors.textTertiary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = s.nothingFound,
                    style = MoneyManagerTheme.typography.cardTitle,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.tryChangingQuery,
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
                    text = s.noTransactions,
                    style = MoneyManagerTheme.typography.cardTitle,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.addFirstTransaction,
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
    val s = MoneyManagerTheme.strings

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
                Text(s.ok)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel)
            }
        },
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    s.choosePeriod,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            modifier = Modifier.height(500.dp),
        )
    }
}

private fun formatDateHeader(timestamp: Long, todayStr: String, yesterdayStr: String, formatter: SimpleDateFormat): String {
    val txDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    return when (txDate) {
        today -> todayStr
        today.minusDays(1) -> yesterdayStr
        else -> formatter.format(Date(timestamp))
    }
}

private fun formatTime(timestamp: Long, formatter: SimpleDateFormat): String = formatter.format(Date(timestamp))

@Preview(showBackground = true)
@Composable
private fun TransactionListScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        TransactionListScreen(
            state = TransactionListState(
                balance = 1_250_000.50,
                displayCurrency = "KZT",
                summaryMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                summaryDisplayMode = SummaryDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                isLoading = false,
                selectedAccountName = "Kaspi Gold",
                periodIncome = 450_000.0,
                periodExpense = 358_400.0,
                transactionRows = persistentListOf(
                    TransactionRowState(
                        transaction = Transaction(
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
                        originalAmount = 15_480.0,
                        originalCurrency = "KZT",
                        displayMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                    ),
                    TransactionRowState(
                        transaction = Transaction(
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
                        originalAmount = 2_450.0,
                        originalCurrency = "USD",
                        convertedAmount = 1_220_875.0,
                        convertedCurrency = "KZT",
                        conversionStatus = ConversionStatus.AVAILABLE,
                        displayMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                        secondaryMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("USD"),
                    ),
                    TransactionRowState(
                        transaction = Transaction(
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
                        originalAmount = 450_000.0,
                        originalCurrency = "KZT",
                        displayMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
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
                displayCurrency = "KZT",
                summaryMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                summaryDisplayMode = SummaryDisplayMode.ORIGINAL_SINGLE_CURRENCY,
                isLoading = false,
                selectedAccountName = "Kaspi Gold",
                periodIncome = 0.0,
                periodExpense = 0.0,
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
