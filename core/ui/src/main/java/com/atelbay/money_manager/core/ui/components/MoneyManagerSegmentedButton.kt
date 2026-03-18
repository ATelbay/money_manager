package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.animateColorAsState
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun MoneyManagerSegmentedButton(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified,
    testTagPrefix: String? = null,
) {
    val colors = MoneyManagerTheme.colors

    Row(
        modifier = modifier
            .let { if (height != Dp.Unspecified) it.height(height) else it }
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceBorder)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val textColor by animateColorAsState(
                targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
                animationSpec = MoneyManagerMotion.ColorTransition,
                label = "segmentText",
            )

            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOptionSelected(option) }
                        .let { if (testTagPrefix != null) it.testTag("${testTagPrefix}_${option}") else it },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .clickable { onOptionSelected(option) }
                        .let { if (testTagPrefix != null) it.testTag("${testTagPrefix}_${option}") else it }
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
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerSegmentedButtonPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerSegmentedButton(
            options = listOf("Week", "Month", "Year"),
            selectedOption = "Month",
            onOptionSelected = {},
            modifier = Modifier.padding(16.dp),
            height = 40.dp,
        )
    }
}
