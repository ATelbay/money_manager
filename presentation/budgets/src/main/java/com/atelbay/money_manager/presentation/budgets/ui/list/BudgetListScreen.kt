package com.atelbay.money_manager.presentation.budgets.ui.list

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.atelbay.money_manager.core.model.Budget
import com.atelbay.money_manager.core.ui.components.GlassCard
import com.atelbay.money_manager.core.ui.components.MoneyManagerFAB
import com.atelbay.money_manager.core.ui.components.categoryIconFromName
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    state: BudgetListState,
    onAddClick: () -> Unit,
    onBudgetClick: (Long) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = MoneyManagerTheme.strings
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    Scaffold(
        modifier = modifier.testTag("budgetList:screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.budgets,
                        style = typography.sectionHeader,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("budgetList:backButton"),
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
                onClick = onAddClick,
                icon = Icons.Default.Add,
                contentDescription = s.newBudget,
                testTag = "budgetList:fab",
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

        if (state.budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = s.noBudgets,
                    style = typography.cardTitle,
                    color = colors.textSecondary,
                    modifier = Modifier.testTag("budgetList:emptyState"),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("budgetList:list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(
                items = state.budgets,
                key = { it.id },
            ) { budget ->
                BudgetSwipeToDeleteItem(
                    onDelete = { onDeleteBudget(budget.id) },
                ) {
                    BudgetListItem(
                        budget = budget,
                        onClick = { onBudgetClick(budget.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budgetList:item_${budget.id}"),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun BudgetListItem(
    budget: Budget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val s = MoneyManagerTheme.strings

    val progressColor = when {
        budget.percentage > 0.9f -> Color(0xFFF87171)
        budget.percentage > 0.7f -> Color(0xFFFBBF24)
        else -> Color(0xFF4ADE80)
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(budget.categoryColor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIconFromName(budget.categoryIcon),
                        contentDescription = null,
                        tint = Color(budget.categoryColor),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.categoryName,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${s.spent}: ${formatAmount(budget.spent)} / ${formatAmount(budget.monthlyLimit)}",
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                if (budget.percentage > 1.0f) {
                    Text(
                        text = s.overBudget,
                        style = typography.caption,
                        color = Color(0xFFF87171),
                        modifier = Modifier.testTag("budgetList:overBudget_${budget.id}"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { budget.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("budgetList:progressBar_${budget.id}"),
                color = progressColor,
                trackColor = colors.borderSubtle,
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(budget.percentage * 100).toInt()}%",
                    style = typography.caption,
                    color = progressColor,
                )
                Text(
                    text = "${s.remaining}: ${formatAmount(budget.remaining)}",
                    style = typography.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount >= 1_000_000) {
        "${(amount / 1_000_000).toBigDecimal().stripTrailingZeros().toPlainString()}M"
    } else if (amount >= 1_000) {
        "${(amount / 1_000).toBigDecimal().stripTrailingZeros().toPlainString()}K"
    } else {
        amount.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSwipeToDeleteItem(
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
                    contentDescription = MoneyManagerTheme.strings.delete,
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        content()
    }
}
