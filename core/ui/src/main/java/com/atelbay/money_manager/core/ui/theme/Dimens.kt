package com.atelbay.money_manager.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale for Money Manager. Use these instead of hardcoded dp literals so
 * padding/gaps stay consistent across the design system.
 */
object MoneyManagerSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/**
 * Corner-radius / shape tokens beyond the Material 3 [androidx.compose.material3.Shapes] set.
 */
object MoneyManagerShapes {
    /** Standard card / surface corner radius. */
    val card = RoundedCornerShape(20.dp)
    /** Primary action button corner radius. */
    val button = RoundedCornerShape(14.dp)
    /** Minimum accessible touch-target size. */
    val minTouchTarget: Dp = 48.dp
    /** Default control height (buttons, fields). */
    val controlHeight: Dp = 56.dp
}
