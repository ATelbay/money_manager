# Quickstart: UI Overflow & Layout Audit

## Prerequisites

- Android Studio with Compose BOM 2026.01.01 (already configured)
- No new dependencies needed — `TextAutoSize` is built into Compose Foundation 1.8+

## Key API: TextAutoSize.StepBased

```kotlin
import androidx.compose.foundation.text.TextAutoSize

// For monetary amounts — always show full number, shrink font if needed
Text(
    text = "KZT 1,500,000,000.00",
    style = MoneyManagerTheme.typography.balanceDisplay,
    maxLines = 1,
    autoSize = TextAutoSize.StepBased(
        minFontSize = 14.sp,
        maxFontSize = 34.sp,
        stepSize = 1.sp
    )
)

// For text content — truncate with ellipsis
Text(
    text = "Very long transaction description that won't fit",
    style = MoneyManagerTheme.typography.caption,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.weight(1f)
)
```

## Important Rules

1. **Never truncate amounts** — use `autoSize` instead of `TextOverflow.Ellipsis` for money
2. **Parent must have bounded width** — `autoSize` needs finite constraints to calculate sizing
3. **Min font size**: 12sp general, 11sp for compact card contexts
4. **Max font size**: match the existing typography style (e.g., 34sp for balanceDisplay)
5. **stepSize = 1.sp** for all components (good precision, negligible perf cost)

## Verification

For each modified component, verify with these test values:
- Small: `0.00`
- Medium: `1,234,567.89`
- Large: `999,999,999,999.99` (12 digits)
- Max: `9,999,999,999,999.99` (13 digits)
- Negative: `−9,999,999,999,999.99`

Use `@Preview` with these values to visually verify.

## Files to Modify (in order)

### P1 — Main screen components
1. `core/ui/.../components/BalanceCard.kt`
2. `core/ui/.../components/IncomeExpenseCard.kt`
3. `core/ui/.../components/TransactionListItem.kt`

### P2 — Secondary screens
4. `core/ui/.../components/AccountCard.kt`
5. `core/ui/.../components/SummaryStatCard.kt`
6. `presentation/accounts/.../AccountListScreen.kt`
7. `presentation/statistics/.../StatisticsScreen.kt`
8. `presentation/import/.../ParsedTransactionItem.kt`
9. `core/ui/.../components/CategoryPicker.kt`
10. `core/ui/.../components/MoneyManagerTextField.kt` (hero amount)

### P3 — Verification only
11. `presentation/settings/.../SettingsScreen.kt`
12. `presentation/auth/.../SignInScreen.kt`
13. Edit screens (Transaction, Account, Category)
