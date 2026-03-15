package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.style.TextOverflow
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import java.text.NumberFormat
import java.util.Locale

enum class StatType { DEFAULT, INCOME, EXPENSE }

@Composable
fun SummaryStatCard(
    title: String,
    value: Double,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    currency: String = "\u20B8",
    change: Float? = null,
    type: StatType = StatType.DEFAULT,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val selectionColor = when (type) {
        StatType.INCOME -> colors.incomeForeground
        StatType.EXPENSE -> colors.expenseForeground
        StatType.DEFAULT -> colors.chart1
    }
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> selectionColor.copy(alpha = 0.9f)
            onClick != null -> colors.surfaceBorder.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "summaryCardBorder",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (onClick != null && !selected) 0.72f else 1f,
        label = "summaryCardAlpha",
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) colors.textPrimary else colors.textSecondary,
        label = "summaryCardTitle",
    )
    val amountColor by animateColorAsState(
        targetValue = when {
            selected && type == StatType.INCOME -> colors.incomeForeground
            selected && type == StatType.EXPENSE -> colors.expenseForeground
            else -> colors.textPrimary
        },
        label = "summaryCardAmount",
    )

    val iconBg = when (type) {
        StatType.INCOME -> colors.incomeBg
        StatType.EXPENSE -> colors.expenseBg
        StatType.DEFAULT -> colors.chart1.copy(alpha = 0.1f)
    }
    val iconColor = when (type) {
        StatType.INCOME -> colors.incomeForeground
        StatType.EXPENSE -> colors.expenseForeground
        StatType.DEFAULT -> colors.chart1
    }

    GlassCard(
        modifier = modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(contentAlpha),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (change != null) {
                    val isPositive = change > 0
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPositive) colors.incomeBg else colors.expenseBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) colors.incomeForeground else colors.expenseForeground,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "${kotlin.math.abs(change).toInt()}%",
                            style = typography.caption,
                            color = if (isPositive) colors.incomeForeground else colors.expenseForeground,
                        )
                    }
                }
            }

            Text(
                text = title,
                style = typography.caption,
                color = titleColor,
            )
            Text(
                text = "$currency ${formatter.format(value)}",
                style = typography.amount,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 1.sp),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun SummaryStatCardPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        SummaryStatCard(
            title = "TOTAL EXPENSES",
            value = 358_400.00,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            type = StatType.EXPENSE,
            change = -8f,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun SummaryStatCardExtremePreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        SummaryStatCard(
            title = "TOTAL EXPENSES",
            value = 9_999_999_999_999.99,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            type = StatType.EXPENSE,
            currency = "KZT",
            modifier = Modifier.padding(16.dp),
        )
    }
}
