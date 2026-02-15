package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceCard(
    accountName: String,
    balance: Double,
    modifier: Modifier = Modifier,
    currency: String = "\u20B8",
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography

    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "balance",
    )

    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        // Teal gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Teal.copy(alpha = 0.3f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = accountName,
                style = typography.caption,
                color = colors.textSecondary,
            )
            Text(
                text = "$currency ${formatter.format(animatedBalance.toDouble())}",
                style = typography.balanceDisplay,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun BalanceCardPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        BalanceCard(
            accountName = "Kaspi Gold",
            balance = 1_250_000.50,
            modifier = Modifier.padding(16.dp),
        )
    }
}
