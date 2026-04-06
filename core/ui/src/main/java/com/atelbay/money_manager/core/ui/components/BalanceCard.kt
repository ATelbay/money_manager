package com.atelbay.money_manager.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import com.atelbay.money_manager.core.ui.theme.MoneyManagerMotion
import com.atelbay.money_manager.core.ui.util.LocalReduceMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.util.MoneyDisplayFormatter
import com.atelbay.money_manager.core.ui.util.MoneyDisplayPresentation
import com.atelbay.money_manager.core.ui.util.formatAmount
import com.atelbay.money_manager.core.ui.util.supportingText
import java.text.NumberFormat
import java.util.Locale

private val DoubleToVector: TwoWayConverter<Double, AnimationVector1D> =
    TwoWayConverter(
        convertToVector = { AnimationVector1D(it.toFloat()) },
        convertFromVector = { it.value.toDouble() },
    )

private val BalanceCardGreenLight = Color(0xFF3D8A5A)
private val BalanceCardGreenEnd = Color(0xFF2D6B44)

@Composable
fun BalanceCard(
    accountName: String,
    balance: Double,
    modifier: Modifier = Modifier,
    moneyDisplay: MoneyDisplayPresentation = MoneyDisplayFormatter.resolveAndFormat("KZT"),
    unavailableSupportingText: String? = null,
    onAccountPickerClick: (() -> Unit)? = null,
) {
    val typography = MoneyManagerTheme.typography
    val reduceMotion = LocalReduceMotion.current

    val animatable = remember { Animatable(balance, DoubleToVector) }
    var displayBalance by remember { mutableStateOf(balance) }
    var isFirstEmission by remember { mutableStateOf(true) }

    LaunchedEffect(balance) {
        if (isFirstEmission) {
            animatable.snapTo(balance)
            displayBalance = balance
            isFirstEmission = false
        } else {
            animatable.animateTo(
                targetValue = balance,
                animationSpec = tween(
                    durationMillis = MoneyManagerMotion.duration(
                        MoneyManagerMotion.DurationExtraLong,
                        reduceMotion,
                    ),
                ),
            ) {
                displayBalance = value
            }
            displayBalance = balance
        }
    }

    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = BalanceCardGreenLight.copy(alpha = 0.3f),
                spotColor = BalanceCardGreenLight.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(BalanceCardGreenLight, BalanceCardGreenEnd),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            // Account name row with chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onAccountPickerClick != null) {
                    Modifier.clickable(onClick = onAccountPickerClick)
                } else {
                    Modifier
                },
            ) {
                Text(
                    text = accountName,
                    style = typography.caption,
                    color = Color.White.copy(alpha = 0.8f),
                )
                if (onAccountPickerClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Balance amount
            Text(
                text = moneyDisplay.formatAmount(
                    amount = displayBalance,
                    formatter = formatter,
                ),
                style = typography.balanceDisplay,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 34.sp, stepSize = 1.sp),
                modifier = Modifier.padding(top = 8.dp),
            )

            moneyDisplay.supportingText(unavailableSupportingText)?.let { supportingText ->
                Text(
                    text = supportingText,
                    style = typography.caption,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCardPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        BalanceCard(
            accountName = "Kaspi Gold",
            balance = 1_250_000.50,
            modifier = Modifier.padding(16.dp),
            onAccountPickerClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun BalanceCardDarkPreview() {
    MoneyManagerTheme(themeMode = "dark", dynamicColor = false) {
        BalanceCard(
            accountName = "Kaspi Gold",
            balance = 1_250_000.50,
            modifier = Modifier.padding(16.dp),
            onAccountPickerClick = {},
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
                moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
                modifier = Modifier.padding(16.dp),
                onAccountPickerClick = {},
            )
            BalanceCard(
                accountName = "Kaspi Gold",
                balance = -9_999_999_999_999.99,
                moneyDisplay = MoneyDisplayFormatter.resolveAndFormat("KZT"),
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
