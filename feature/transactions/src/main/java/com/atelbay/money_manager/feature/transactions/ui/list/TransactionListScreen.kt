package com.atelbay.money_manager.feature.transactions.ui.list

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.components.MoneyManagerCard
import com.atelbay.money_manager.core.ui.theme.ExpenseColor
import com.atelbay.money_manager.core.ui.theme.IncomeColor
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.model.Transaction
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.collections.immutable.persistentListOf
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    state: TransactionListState,
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onCategoriesClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onAccountsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("transactionList:screen"),
        topBar = {
            TopAppBar(
                title = { Text("Money Manager") },
                actions = {
                    IconButton(
                        onClick = onAccountsClick,
                        modifier = Modifier.testTag("transactionList:accountsButton"),
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = "Счета")
                    }
                    IconButton(
                        onClick = onStatisticsClick,
                        modifier = Modifier.testTag("transactionList:statisticsButton"),
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "Статистика")
                    }
                    IconButton(
                        onClick = onCategoriesClick,
                        modifier = Modifier.testTag("transactionList:categoriesButton"),
                    ) {
                        Icon(Icons.Default.Category, contentDescription = "Категории")
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("transactionList:list"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
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

            if (state.transactions.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Пока нет транзакций",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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
            onDeleteTransaction = {},
            onCategoriesClick = {},
            onStatisticsClick = {},
            onAccountsClick = {},
        )
    }
}
