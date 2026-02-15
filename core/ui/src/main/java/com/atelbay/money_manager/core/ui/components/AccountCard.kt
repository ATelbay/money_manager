package com.atelbay.money_manager.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AccountCard(
    accountName: String,
    currency: String,
    balance: Double,
    modifier: Modifier = Modifier,
    gradientColor: Color = Teal,
    onClick: (() -> Unit)? = null,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        // Gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            gradientColor.copy(alpha = 0.2f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = accountName,
                    style = typography.cardTitle,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = currency,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceBorder)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = typography.caption,
                    color = colors.textSecondary,
                )
            }

            Text(
                text = "$currency ${formatter.format(balance)}",
                style = typography.sectionHeader,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun AccountCardPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        AccountCard(
            accountName = "Kaspi Gold",
            currency = "KZT",
            balance = 1_250_000.50,
            modifier = Modifier.padding(16.dp),
        )
    }
}
