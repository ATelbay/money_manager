package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun IncomeExpenseCard(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier,
    currency: String = "\u20B8",
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val net = income - expense
    val isPositive = net >= 0

    // Savings rate: what % of income was kept
    val savingsRate = when {
        income <= 0 && expense <= 0 -> 0f
        income <= 0 -> 0f // no income but has expenses
        else -> ((income - expense) / income).toFloat().coerceIn(0f, 1f)
    }
    val expenseRate = 1f - savingsRate

    // remember(savingsRate) ensures animation only fires when data changes, not on scroll recomposition
    val animatedSavings by animateFloatAsState(
        targetValue = savingsRate,
        animationSpec = tween(1000),
        label = "savingsBar",
    )
    val animatedExpense by animateFloatAsState(
        targetValue = expenseRate,
        animationSpec = tween(1000),
        label = "expenseBar",
    )

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Income
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.income),
                        )
                        Text(
                            text = "ДОХОД",
                            style = typography.caption,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        text = "+$currency ${formatter.format(income)}",
                        style = typography.amount,
                        color = colors.incomeForeground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Expense
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.expense),
                        )
                        Text(
                            text = "РАСХОД",
                            style = typography.caption,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        text = "\u2212$currency ${formatter.format(expense)}",
                        style = typography.amount,
                        color = colors.expenseForeground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = colors.borderSubtle,
            )

            // Net savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "БАЛАНС",
                    style = typography.caption,
                    color = colors.textSecondary,
                )
                Text(
                    text = "${if (isPositive) "+" else "\u2212"}$currency ${formatter.format(abs(net))}",
                    style = typography.cardTitle,
                    color = if (isPositive) colors.incomeForeground else colors.expenseForeground,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Savings rate bar: green (saved) from left, red (spent) from right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.surfaceBorder),
            ) {
                if (animatedSavings > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(animatedSavings.coerceAtLeast(0.001f))
                            .height(8.dp)
                            .background(colors.income),
                    )
                }
                if (animatedExpense > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(animatedExpense.coerceAtLeast(0.001f))
                            .height(8.dp)
                            .background(colors.expense),
                    )
                }
            }

            // Savings rate label
            if (income > 0) {
                Text(
                    text = "Сохранено ${(savingsRate * 100).toInt()}%",
                    style = typography.caption,
                    color = if (isPositive) colors.incomeForeground else colors.expenseForeground,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun IncomeExpenseCardPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        IncomeExpenseCard(
            income = 450_000.00,
            expense = 358_400.00,
            modifier = Modifier.padding(16.dp),
        )
    }
}
