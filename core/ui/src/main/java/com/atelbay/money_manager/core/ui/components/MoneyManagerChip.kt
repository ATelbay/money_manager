package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

enum class ChipType { INCOME, EXPENSE, DEFAULT }

@Composable
fun MoneyManagerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ChipType = ChipType.DEFAULT,
) {
    val colors = MoneyManagerTheme.colors

    val containerColor by animateColorAsState(
        targetValue = when {
            selected && type == ChipType.INCOME -> colors.income
            selected && type == ChipType.EXPENSE -> colors.expense
            selected -> colors.chart1
            type == ChipType.INCOME -> colors.incomeBg
            type == ChipType.EXPENSE -> colors.expenseBg
            else -> Color.Transparent
        },
        animationSpec = tween(250),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            type == ChipType.INCOME -> colors.incomeForeground
            type == ChipType.EXPENSE -> colors.expenseForeground
            else -> colors.textSecondary
        },
        animationSpec = tween(250),
        label = "chipContent",
    )
    val borderColor = when {
        selected -> Color.Transparent
        type == ChipType.INCOME -> colors.incomeForeground.copy(alpha = 0.3f)
        type == ChipType.EXPENSE -> colors.expenseForeground.copy(alpha = 0.3f)
        else -> colors.surfaceBorder
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            selectedContainerColor = containerColor,
            selectedLabelColor = contentColor,
        ),
        border = BorderStroke(1.dp, borderColor),
    )
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerChipPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MoneyManagerChip(
                label = "Расход",
                selected = true,
                onClick = {},
                type = ChipType.EXPENSE,
            )
            MoneyManagerChip(
                label = "Доход",
                selected = false,
                onClick = {},
                type = ChipType.INCOME,
            )
        }
    }
}
