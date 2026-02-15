package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal

@Composable
fun MoneyManagerSegmentedButton(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MoneyManagerTheme.colors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceBorder)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Teal else Color.Transparent,
                animationSpec = tween(200),
                label = "segmentBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else colors.textSecondary,
                animationSpec = tween(200),
                label = "segmentText",
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerSegmentedButtonPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerSegmentedButton(
            options = listOf("Система", "Светлая", "Тёмная"),
            selectedOption = "Система",
            onOptionSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
