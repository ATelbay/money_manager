package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import java.text.NumberFormat
import java.util.Locale

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
    currency: String = "\u20B8",
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
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
                    .height(72.dp),
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
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "${if (isIncome) "+" else "\u2212"}$currency ${formatter.format(amount)}",
                        style = typography.amount,
                        color = if (isIncome) colors.incomeForeground else colors.expenseForeground,
                    )
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
