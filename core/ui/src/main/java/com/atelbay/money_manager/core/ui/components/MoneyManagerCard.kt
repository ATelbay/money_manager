package com.atelbay.money_manager.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@Composable
fun MoneyManagerCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MoneyManagerTheme.colors

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, colors.surfaceBorder),
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, colors.surfaceBorder),
            content = content,
        )
    }
}

/**
 * Glassmorphism card with gradient background, border, and top glow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MoneyManagerTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    val glassBrush = Brush.linearGradient(
        colors = listOf(colors.glassBgStart, colors.glassBgEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
    val glowColor = colors.glassGlow

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(brush = glassBrush)
                // Top edge glow
                drawRect(
                    color = glowColor,
                    size = size.copy(height = 1.dp.toPx()),
                )
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun MoneyManagerCardPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        MoneyManagerCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Баланс: 150 000 KZT",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun GlassCardPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Glass Card",
                modifier = Modifier.padding(16.dp),
                color = Color.White,
            )
        }
    }
}
