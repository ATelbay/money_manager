package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import com.atelbay.money_manager.core.ui.util.LocalReduceMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.Teal
import java.text.NumberFormat
import java.util.Locale

private val DoubleToVector: TwoWayConverter<Double, AnimationVector1D> =
    TwoWayConverter(
        convertToVector = { AnimationVector1D(it.toFloat()) },
        convertFromVector = { it.value.toDouble() },
    )

@Composable
fun BalanceCard(
    accountName: String,
    balance: Double,
    modifier: Modifier = Modifier,
    currency: String = "\u20B8",
    isUnavailable: Boolean = false,
) {
    val colors = MoneyManagerTheme.colors
    val typography = MoneyManagerTheme.typography
    val reduceMotion = LocalReduceMotion.current

    val animatable = remember { Animatable(balance, DoubleToVector) }
    var displayBalance by remember { mutableStateOf(balance) }

    LaunchedEffect(balance) {
        animatable.animateTo(
            targetValue = balance,
            animationSpec = tween(
                durationMillis = MoneyManagerMotion.duration(
                    MoneyManagerMotion.DurationExtraLong,
                    reduceMotion,
                ),
            ),
        ) {
            // Update display value on each animation frame
            displayBalance = value
        }
        // Snap to exact Double value after animation to avoid Float precision loss
        displayBalance = balance
    }

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
                text = if (isUnavailable) "-" else "$currency ${formatter.format(displayBalance)}",
                style = typography.balanceDisplay,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 34.sp, stepSize = 1.sp),
                modifier = Modifier.padding(top = 8.dp),
            )
            if (isUnavailable) {
                Text(
                    text = "Недостаточно курсов для конвертации",
                    style = typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D, name = "Extreme values")
@Composable
private fun BalanceCardExtremePreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        Column {
            BalanceCard(
                accountName = "Kaspi Gold",
                balance = 9_999_999_999_999.99,
                currency = "KZT",
                modifier = Modifier.padding(16.dp),
            )
            BalanceCard(
                accountName = "Kaspi Gold",
                balance = -9_999_999_999_999.99,
                currency = "KZT",
                modifier = Modifier.padding(16.dp),
            )
            BalanceCard(
                accountName = "Kaspi Gold",
                balance = 0.00,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
