package com.atelbay.money_manager.presentation.transactions.ui.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import com.atelbay.money_manager.core.ui.components.LocalAnimatedVisibilityScope
import com.atelbay.money_manager.core.ui.components.LocalSharedTransitionScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.model.Account
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
import com.atelbay.money_manager.core.ui.util.formatAmount
import com.atelbay.money_manager.core.ui.util.isUnavailable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val TransactionListBottomGutter = 16.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onTabSelected: (TransactionType?) -> Unit,
    onPeriodSelected: (Period) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAccountPickerClick: () -> Unit,
    onAccountSelected: (Long?) -> Unit,
    onDismissAccountPicker: () -> Unit,
    onCalendarClick: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    onRangeSelected: (startMillis: Long, endMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
) {
    val colors = MoneyManagerTheme.colors
    val s = MoneyManagerTheme.strings
    val locale = s.locale
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val dateHeaderFormat = remember(locale) {
        SimpleDateFormat("dd MMMM", locale).also { it.timeZone = java.util.TimeZone.getDefault() }
    }
    val timeFormat = remember(locale) {
        SimpleDateFormat("HH:mm", locale).also { it.timeZone = java.util.TimeZone.getDefault() }
    }
    val layoutDirection = LocalLayoutDirection.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.testTag("transactionList:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = contentWindowInsets,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                onClick = {
                    if (state.accounts.isEmpty()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(s.noAccountsWarning)
                        }
                    } else {
                        onAddClick()
                    }
                },
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
            // 1. Balance card
            item(key = "balance") {
                BalanceCard(
                    accountName = state.selectedAccountName ?: s.allAccounts,
                    balance = state.balance ?: 0.0,
                    moneyDisplay = state.summaryMoneyDisplay,
                    unavailableSupportingText = s.mixedCurrencyUnavailable
                        .takeIf { state.summaryMoneyDisplay.isUnavailable },
                    onAccountPickerClick = onAccountPickerClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("transactionList:balance"),
                )
            }

            // 2. Date period chips (This Month | Day | Week | 30 days | Year) + calendar button
            item(key = "period") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("transactionList:periodFilter"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val periods = listOf(
                        Period.MONTH to s.currentMonth,
                        Period.TODAY to s.periodDay,
                        Period.WEEK to s.periodWeek,
                        Period.YEAR to s.periodYear,
                    )
                    periods.forEach { (period, label) ->
                        MoneyManagerChip(
                            label = label,
                            selected = state.customDateRangeStart == null && state.selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onCalendarClick,
                        modifier = Modifier.testTag("transactionList:calendarButton"),
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = s.selectRange,
                            tint = if (state.customDateRangeStart != null) {
                                colors.chart1
                            } else {
                                colors.textSecondary
                            },
                        )
                    }
                }
            }

            // 2b. Custom date range label (shown when a custom range is active)
            if (state.customDateRangeStart != null && state.customDateRangeEnd != null) {
                item(key = "customRangeLabel") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                    ) {
                        val customLabel = remember(state.customDateRangeStart, state.customDateRangeEnd) {
                            val sdf = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault())
                            "${sdf.format(java.util.Date(state.customDateRangeStart))} – ${sdf.format(java.util.Date(state.customDateRangeEnd))}"
                        }
                        MoneyManagerChip(
                            label = customLabel,
                            selected = true,
                            onClick = onCalendarClick,
                            modifier = Modifier.testTag("transactionList:customRangeLabel"),
                        )
                    }
                }
            }

            // 3. Income/Expense summary
            item(key = "summary") {
                IncomeExpenseCard(
                    income = state.periodIncome ?: 0.0,
                    expense = state.periodExpense ?: 0.0,
                    moneyDisplay = state.summaryMoneyDisplay,
                    unavailableSupportingText = s.mixedCurrencyUnavailable
                        .takeIf { state.summaryMoneyDisplay.isUnavailable },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            }

            // 4. Search bar
            item(key = "search") {
                MoneyManagerTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = s.searchTransactions,
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

            // 5. Transaction type filter chips (All | Expenses | Income)
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

            // 6. Transaction list or empty state
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
                val grouped = state.transactionRows.groupBy {
                    formatDateHeader(
                        it.transaction.date,
                        s.periodToday,
                        s.periodYesterday,
                        dateHeaderFormat,
                    )
                }

                grouped.forEach { (dateHeader, transactionRows) ->
                    // Compute daily net sum for this header
                    val dateKey = transactionRows.firstOrNull()?.let { row ->
                        Instant.ofEpochMilli(row.transaction.date)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                    }
                    val dailyNet = dateKey?.let { state.dailyNetSums[it] }

                    // Date section header with daily net sum
                    item(key = "header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = dateHeader,
                                style = MoneyManagerTheme.typography.caption,
                                color = colors.textSecondary,
                            )
                            if (dailyNet != null) {
                                val isPositive = dailyNet >= 0
                                val sign = if (isPositive) "+" else "\u2212"
                                val netColor = if (isPositive) {
                                    colors.incomeForeground
                                } else {
                                    colors.expenseForeground
                                }
                                Text(
                                    text = state.summaryMoneyDisplay.formatAmount(
                                        kotlin.math.abs(dailyNet),
                                        sign = sign,
                                    ),
                                    style = MoneyManagerTheme.typography.caption,
                                    color = netColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
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
                            accountName = null,
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

    // Account picker bottom sheet
    if (state.showAccountPicker) {
        AccountPickerBottomSheet(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = onAccountSelected,
            onDismiss = onDismissAccountPicker,
        )
    }

    // Date picker dialog
    if (state.showDatePickerDialog) {
        TransactionDatePickerDialog(
            onDismiss = onCalendarClick,
            onMonthSelected = onMonthSelected,
            onRangeSelected = onRangeSelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerBottomSheet(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MoneyManagerTheme.colors
    val s = MoneyManagerTheme.strings

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // "All accounts" option
            ListItem(
                headlineContent = {
                    Text(
                        text = s.allAccounts,
                        style = MoneyManagerTheme.typography.cardTitle,
                        fontWeight = if (selectedAccountId == null) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                trailingContent = {
                    if (selectedAccountId == null) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.chart1,
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    onAccountSelected(null)
                    onDismiss()
                },
            )
            HorizontalDivider(color = colors.borderSubtle)

            accounts.forEach { account ->
                val isSelected = account.id == selectedAccountId
                ListItem(
                    headlineContent = {
                        Text(
                            text = account.name,
                            style = MoneyManagerTheme.typography.cardTitle,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = account.currency,
                            style = MoneyManagerTheme.typography.caption,
                            color = colors.textSecondary,
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.chart1,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onAccountSelected(account.id)
                        onDismiss()
                    },
                )
            }
        }
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
                summaryMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                isLoading = false,
                selectedAccountName = "Kaspi Gold",
                periodIncome = 450_000.0,
                periodExpense = 358_400.0,
                selectedPeriod = Period.MONTH,
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
                dailyNetSums = mapOf(
                    LocalDate.now().toString() to -17_930.0,
                    LocalDate.now().minusDays(1).toString() to 450_000.0,
                ),
            ),
            onTransactionClick = {},
            onAddClick = {},
            onImportClick = {},
            onDeleteTransaction = {},
            onTabSelected = {},
            onPeriodSelected = {},
            onSearchQueryChange = {},
            onAccountPickerClick = {},
            onAccountSelected = {},
            onDismissAccountPicker = {},
            onCalendarClick = {},
            onMonthSelected = { _, _ -> },
            onRangeSelected = { _, _ -> },
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
                summaryMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                isLoading = false,
                selectedAccountName = "Kaspi Gold",
                periodIncome = 0.0,
                periodExpense = 0.0,
                selectedPeriod = Period.MONTH,
            ),
            onTransactionClick = {},
            onAddClick = {},
            onImportClick = {},
            onDeleteTransaction = {},
            onTabSelected = {},
            onPeriodSelected = {},
            onSearchQueryChange = {},
            onAccountPickerClick = {},
            onAccountSelected = {},
            onDismissAccountPicker = {},
            onCalendarClick = {},
            onMonthSelected = { _, _ -> },
            onRangeSelected = { _, _ -> },
        )
    }
}
