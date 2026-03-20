# Research: UI Overflow & Layout Audit

## R-001: Auto-Sizing Text API in Compose

**Decision**: Use built-in `TextAutoSize.StepBased()` on M3 `Text` composable

**Rationale**: Compose Foundation 1.8+ (April 2025) introduced `TextAutoSize` — a first-party API. M3 `Text` gained the `autoSize` parameter in compose-material3 ~1.4+ (BOM 2025.05.01+). Since the project uses **BOM 2026.01.01**, both `BasicText` and M3 `Text` support it natively. No third-party library needed.

**API**:
```kotlin
Text(
    text = "1,500,000,000.00",
    autoSize = TextAutoSize.StepBased(
        minFontSize = 12.sp,
        maxFontSize = 34.sp,
        stepSize = 1.sp
    )
)
```

The algorithm performs a binary search within `[minFontSize, maxFontSize]` to find the largest font size that fits without overflow.

**Limitations**:
- Multiline: line spacing does NOT scale proportionally — avoid `maxLines > 1` with autoSize for amounts
- Requires bounded constraints from parent (finite width/height) — not usable inside horizontal scroll without explicit width
- `stepSize` affects precision vs performance — 1.sp is fine for our use case

**Alternatives considered**:
- Custom iterative measurement approach — obsolete now that official API exists
- Compact number formatting ("1.5 млрд") — rejected per clarification (always show full amount)

## R-002: Current Overflow Handling Gaps

**Decision**: Fix gaps incrementally per component, prioritizing P1 screens

**Findings from codebase audit**:

| Component | Current Font | Has Overflow Handling | Gap |
|-----------|-------------|----------------------|-----|
| BalanceCard | 34sp (balanceDisplay) | NO | Needs autoSize (min 14sp → max 34sp) |
| IncomeExpenseCard | 16sp (amount) | NO | Needs autoSize (min 12sp → max 16sp) |
| SummaryStatCard | 16sp (amount) | NO | Needs autoSize (min 12sp → max 16sp) |
| TransactionListItem | 16sp (amount) | YES (ellipsis) | Change amount from ellipsis to autoSize |
| AccountCard | 28sp (balance) | NO | Needs autoSize (min 12sp → max 28sp) |
| AccountListScreen | varies | NO (total balance) | Needs autoSize for total balance header |
| CategoryPicker | caption | NO | Needs maxLines=1 + ellipsis |
| ParsedTransactionItem | varies | NO (amounts) | Needs autoSize or ellipsis |
| MoneyManagerAmountField | 30sp | NO | Needs autoSize for hero input display |
| StatisticsScreen (donut center) | varies | YES (ellipsis) | Change to autoSize |
| StatisticsScreen (category row) | 16sp (amount) | YES (ellipsis on name) | Amount needs autoSize |
| SignInScreen | body | YES (ellipsis) | OK, already handled |
| SettingsScreen | body | Partial | Verify email/name rows |

**Amount formatting**: All 6 files use duplicated `NumberFormat.getNumberInstance(Locale.US)` — opportunity to extract shared utility, but not in scope (avoid refactoring beyond what's needed).

## R-003: Typography Scale for AutoSize

**Decision**: Define per-component min/max ranges based on hierarchy importance

**Rationale**: Different components have different space budgets and visual hierarchy importance. The balance is the most prominent element (34sp), so it gets the widest auto-size range. List items are compact, so they get a narrower range.

| Component | maxFontSize | minFontSize | stepSize |
|-----------|------------|------------|---------|
| BalanceCard (balance) | 34.sp | 14.sp | 1.sp |
| AccountCard (balance) | 28.sp | 12.sp | 1.sp |
| IncomeExpenseCard (amounts) | 16.sp | 11.sp | 1.sp |
| SummaryStatCard (value) | 16.sp | 11.sp | 1.sp |
| TransactionListItem (amount) | 16.sp | 11.sp | 1.sp |
| StatisticsScreen (donut total) | 20.sp | 12.sp | 1.sp |
| StatisticsScreen (category amount) | 16.sp | 11.sp | 1.sp |
| MoneyManagerAmountField (hero) | 30.sp | 16.sp | 1.sp |
| AccountListScreen (total) | 20.sp | 12.sp | 1.sp |

Note: 12sp is the absolute minimum per spec (SC-005). Some components use 11sp min because at 16sp max the visual difference is small and 11sp keeps amounts readable on 360dp screens.
