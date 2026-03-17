package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import com.atelbay.money_manager.core.ui.util.defaultMoneyNumberFormat
import com.atelbay.money_manager.core.ui.util.formatAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListItem(
    description: String,
    category: String,
    categoryIcon: ImageVector,
    categoryColor: Color,
    amount: Double,
    date: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    moneyDisplay: MoneyDisplayPresentation = MoneyDisplayFormatter.resolveAndFormat("KZT"),
    secondaryAmount: Double? = null,
    secondaryMoneyDisplay: MoneyDisplayPresentation? = null,
    secondaryAmountLabel: String? = null,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = remember { defaultMoneyNumberFormat() }
    val sign = if (isIncome) "+" else "\u2212"
    val primaryAmountText = moneyDisplay.formatAmount(
        amount = amount,
        sign = sign,
        formatter = formatter,
    )
    val secondaryAmountText = secondaryAmount?.let {
        val resolvedDisplay = secondaryMoneyDisplay ?: moneyDisplay
        val formattedAmount = resolvedDisplay.formatAmount(
            amount = it,
            sign = sign,
            formatter = formatter,
        )
        secondaryAmountLabel?.let { label -> "$label $formattedAmount" } ?: formattedAmount
    }

    val content = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Description and category
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = description,
                        style = typography.cardTitle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = category,
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                }

                // Amount and date
                Column(
                    modifier = Modifier.widthIn(max = 140.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = primaryAmountText,
                        style = typography.amount,
                        color = if (isIncome) colors.incomeForeground else colors.expenseForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp),
                    )
                    if (secondaryAmountText != null) {
                        Text(
                            text = secondaryAmountText,
                            style = typography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp),
                        )
                    }
                    Text(
                        text = date,
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                }
            }

            // Inset divider
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = 0.5.dp,
                color = colors.border,
            )
        }
    }

    if (onDelete != null) {
        val dismissState = rememberSwipeToDismissBoxState()
        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
        }
        val bgColor by animateColorAsState(
            targetValue = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                else -> Color.Transparent
            },
            animationSpec = MoneyManagerMotion.ColorTransition,
            label = "deleteBg",
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.White,
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            content = { content() },
        )
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionListItemPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        TransactionListItem(
            description = "Магнум супермаркет",
            category = "Продукты",
            categoryIcon = Icons.Default.ShoppingCart,
            categoryColor = Color(0xFF4ECDC4),
            amount = 15_480.00,
            date = "Сегодня",
            isIncome = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionListItemExtremePreview() {
    MoneyManagerTheme(dynamicColor = false) {
        TransactionListItem(
            description = "Оплата за долгосрочную аренду офисного помещения в бизнес-центре",
            category = "Аренда",
            categoryIcon = Icons.Default.ShoppingCart,
            categoryColor = Color(0xFF4ECDC4),
            amount = 9_999_999_999_999.99,
            date = "Сегодня",
            isIncome = false,
            moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
            secondaryAmount = 9_999_999_999_999.99,
            secondaryMoneyDisplay = MoneyDisplayFormatter.resolveAndFormat("USD"),
            secondaryAmountLabel = "≈",
        )
    }
}
