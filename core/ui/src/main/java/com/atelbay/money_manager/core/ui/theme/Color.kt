package com.atelbay.money_manager.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Primary Accent — Teal ──
val Teal = Color(0xFF00C9A7)
val TealHoverLight = Color(0xFF00B396)
val TealHoverDark = Color(0xFF00E0BA)

// ── Income — Mint Green ──
val IncomeMint = Color(0xFF4ADE80)
val IncomeBgLight = Color(0x1F4ADE80) // 12% opacity
val IncomeBgDark = Color(0x1F4ADE80)
val IncomeForegroundLight = Color(0xFF16A34A)

// ── Expense — Coral ──
val ExpenseCoral = Color(0xFFFF6B6B)
val ExpenseBgLight = Color(0x1FFF6B6B)
val ExpenseBgDark = Color(0x1FFF6B6B)
val ExpenseForegroundLight = Color(0xFFDC2626)

// ── Warning — Amber ──
val WarningAmber = Color(0xFFFBBF24)
val WarningBg = Color(0x1FFBBF24)
val WarningForegroundLight = Color(0xFFD97706)

// ── Chart Palette ──
val Chart1 = Color(0xFF00C9A7)
val Chart2 = Color(0xFF4ADE80)
val Chart3 = Color(0xFFFF6B6B)
val Chart4 = Color(0xFFFBBF24)
val Chart5 = Color(0xFFA78BFA)

// ── Category Colors ──
val CategoryShopping = Color(0xFFFF6B6B)
val CategoryFood = Color(0xFF4ECDC4)
val CategoryTransport = Color(0xFF45B7D1)
val CategoryHome = Color(0xFFF7B801)
val CategoryHealth = Color(0xFFE74C3C)
val CategoryWork = Color(0xFF00C9A7)
val CategoryTravel = Color(0xFFA78BFA)
val CategoryGifts = Color(0xFFF472B6)
val CategoryTech = Color(0xFF3B82F6)
val CategoryEducation = Color(0xFF10B981)
val CategoryEntertainment = Color(0xFFEF4444)
val CategoryDining = Color(0xFFF59E0B)

// ── Light Theme ──
val BackgroundLight = Color(0xFFF5F5F7)
val ForegroundLight = Color(0xFF1A1A1A)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceBorderLight = Color(0x0F000000) // 6%
val GlassBgStartLight = Color(0xF2FFFFFF) // 95%
val GlassBgEndLight = Color(0xD9FFFFFF) // 85%
val GlassBorderLight = Color(0x14000000) // 8%
val GlassGlowLight = Color(0x05000000) // 2%
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0x80000000) // 50%
val TextTertiaryLight = Color(0x59000000) // 35%
val BorderLight = Color(0x0F000000) // 6%
val BorderSubtleLight = Color(0x0A000000) // 4%

// ── Dark Theme ──
val BackgroundDark = Color(0xFF0D0D0D)
val ForegroundDark = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceBorderDark = Color(0x0FFFFFFF) // 6%
val GlassBgStartDark = Color(0x0DFFFFFF) // 5%
val GlassBgEndDark = Color(0x05FFFFFF) // 2%
val GlassBorderDark = Color(0x14FFFFFF) // 8%
val GlassGlowDark = Color(0x0AFFFFFF) // 4%
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0x8CFFFFFF) // 55%
val TextTertiaryDark = Color(0x59FFFFFF) // 35%
val BorderDark = Color(0x14FFFFFF) // 8%
val BorderSubtleDark = Color(0x0AFFFFFF) // 4%

// ── M3 Color Scheme Mapping ──
// Light
val PrimaryLight = Teal
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFB8F5E5)
val OnPrimaryContainerLight = Color(0xFF002114)
val SecondaryLight = Color(0xFF4D6357)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCFE9D9)
val OnSecondaryContainerLight = Color(0xFF0A1F16)
val TertiaryLight = Color(0xFF3D6373)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFC1E8FB)
val OnTertiaryContainerLight = Color(0xFF001F29)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val OnBackgroundLight = ForegroundLight
val OnSurfaceLight = ForegroundLight
val SurfaceVariantLight = Color(0xFFDBE5DD)
val OnSurfaceVariantLight = Color(0xFF404943)
val OutlineLight = Color(0xFF707973)

// Dark
val PrimaryDark = Teal
val OnPrimaryDark = Color(0xFF003824)
val PrimaryContainerDark = Color(0xFF005138)
val OnPrimaryContainerDark = Color(0xFFB8F5E5)
val SecondaryDark = Color(0xFFB3CCBD)
val OnSecondaryDark = Color(0xFF1F352A)
val SecondaryContainerDark = Color(0xFF354B40)
val OnSecondaryContainerDark = Color(0xFFCFE9D9)
val TertiaryDark = Color(0xFFA5CCDF)
val OnTertiaryDark = Color(0xFF073543)
val TertiaryContainerDark = Color(0xFF244C5B)
val OnTertiaryContainerDark = Color(0xFFC1E8FB)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
val OnBackgroundDark = ForegroundDark
val OnSurfaceDark = ForegroundDark
val SurfaceVariantDark = Color(0xFF404943)
val OnSurfaceVariantDark = Color(0xFFBFC9C1)
val OutlineDark = Color(0xFF8A938C)

/**
 * Semantic colors beyond Material 3 for Money Manager's financial UI.
 */
@Immutable
data class MoneyManagerColors(
    val income: Color,
    val incomeBg: Color,
    val incomeForeground: Color,
    val expense: Color,
    val expenseBg: Color,
    val expenseForeground: Color,
    val warning: Color,
    val warningBg: Color,
    val warningForeground: Color,
    val glassBgStart: Color,
    val glassBgEnd: Color,
    val glassBorder: Color,
    val glassGlow: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val borderSubtle: Color,
    val primaryHover: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
)

val LightMoneyManagerColors = MoneyManagerColors(
    income = IncomeMint,
    incomeBg = IncomeBgLight,
    incomeForeground = IncomeForegroundLight,
    expense = ExpenseCoral,
    expenseBg = ExpenseBgLight,
    expenseForeground = ExpenseForegroundLight,
    warning = WarningAmber,
    warningBg = WarningBg,
    warningForeground = WarningForegroundLight,
    glassBgStart = GlassBgStartLight,
    glassBgEnd = GlassBgEndLight,
    glassBorder = GlassBorderLight,
    glassGlow = GlassGlowLight,
    surfaceBorder = SurfaceBorderLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    border = BorderLight,
    borderSubtle = BorderSubtleLight,
    primaryHover = TealHoverLight,
    chart1 = Chart1,
    chart2 = Chart2,
    chart3 = Chart3,
    chart4 = Chart4,
    chart5 = Chart5,
)

val DarkMoneyManagerColors = MoneyManagerColors(
    income = IncomeMint,
    incomeBg = IncomeBgDark,
    incomeForeground = IncomeMint,
    expense = ExpenseCoral,
    expenseBg = ExpenseBgDark,
    expenseForeground = ExpenseCoral,
    warning = WarningAmber,
    warningBg = WarningBg,
    warningForeground = WarningAmber,
    glassBgStart = GlassBgStartDark,
    glassBgEnd = GlassBgEndDark,
    glassBorder = GlassBorderDark,
    glassGlow = GlassGlowDark,
    surfaceBorder = SurfaceBorderDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    border = BorderDark,
    borderSubtle = BorderSubtleDark,
    primaryHover = TealHoverDark,
    chart1 = Chart1,
    chart2 = Chart2,
    chart3 = Chart3,
    chart4 = Chart4,
    chart5 = Chart5,
)

val LocalMoneyManagerColors = staticCompositionLocalOf { LightMoneyManagerColors }

// Backward-compatible aliases
val IncomeColor = IncomeForegroundLight
val ExpenseColor = ExpenseForegroundLight
