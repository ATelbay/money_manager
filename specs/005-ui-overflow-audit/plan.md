# Implementation Plan: UI Overflow & Layout Audit

**Branch**: `005-ui-overflow-audit` | **Date**: 2026-03-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-ui-overflow-audit/spec.md`

## Summary

Audit and fix all UI overflow issues across the app. Primary approach: use Compose's built-in `TextAutoSize.StepBased()` for monetary amounts (never truncate amounts), and `maxLines + TextOverflow.Ellipsis` for text content (descriptions, names, emails). No new modules, no data model changes — purely UI-layer fixes in `core:ui` components and presentation screens.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose BOM 2026.01.01, Material 3, `TextAutoSize` (Foundation 1.8+, built-in)
**Storage**: N/A (no data changes)
**Testing**: Manual visual verification + Compose Preview with extreme values
**Target Platform**: Android (min 360dp width)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps — autoSize uses binary search, negligible perf impact
**Constraints**: Min font 12sp (11sp for compact contexts), max per typography scale, 360dp min screen width
**Scale/Scope**: ~15 composable files to modify across core:ui and 6 presentation modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | No new modules. Changes in core:ui (shared components) and presentation modules only. No layer boundary violations. |
| II. Kotlin-First & Jetpack Compose | PASS | All changes are Compose-only. State hoisting preserved — no ViewModel changes. testTags already present on modified components. |
| III. Material 3 Design System | PASS | Using M3 `Text` with `autoSize` parameter. Typography from `MaterialTheme`/custom theme preserved. No hardcoded colors or sizes outside theme. |
| IV. Animation & Motion | PASS | BalanceCard has animated balance — autoSize must work with `AnimatedContent`/`animateXAsState`. No new animations introduced. |
| V. Hilt DI | PASS | No DI changes. |
| VI. Room Database | PASS | No database changes. |
| VII. Testing Architecture | PASS | Existing testTags preserved. @Preview composables should be updated with extreme values for visual validation. |
| VIII. Firebase Ecosystem | PASS | No Firebase changes. |
| IX. Type-Safe Navigation | PASS | No navigation changes. |
| X. Statement Import Pipeline | PASS | Only ParsedTransactionItem UI modified. |
| XI. Preferences DataStore | PASS | No DataStore changes. |

**Gate result**: ALL PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/005-ui-overflow-audit/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: TextAutoSize API, current gaps
├── data-model.md        # Phase 1: No data changes (UI-only)
├── quickstart.md        # Phase 1: Implementation guide
├── checklists/
│   └── requirements.md  # Quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files to modify)

```text
core/ui/src/main/java/com/atelbay/money_manager/core/ui/
├── components/
│   ├── BalanceCard.kt              # autoSize on balance (34sp→14sp)
│   ├── IncomeExpenseCard.kt        # autoSize on income/expense/balance amounts
│   ├── SummaryStatCard.kt          # autoSize on stat value
│   ├── TransactionListItem.kt      # autoSize on amount (replace ellipsis)
│   ├── AccountCard.kt              # autoSize on balance (28sp→12sp)
│   ├── CategoryPicker.kt           # maxLines+ellipsis on category names
│   └── MoneyManagerTextField.kt    # autoSize on hero amount field (30sp→16sp)
└── theme/
    └── Type.kt                     # (reference only, no changes expected)

presentation/
├── transactions/ui/
│   └── list/TransactionListScreen.kt  # Verify BalanceCard/IncomeExpenseCard in context
├── accounts/ui/
│   └── list/AccountListScreen.kt      # autoSize on total balance header
├── statistics/ui/
│   └── StatisticsScreen.kt            # autoSize on donut center + category amounts
├── import/ui/components/
│   └── ParsedTransactionItem.kt       # autoSize/ellipsis on amounts + descriptions
├── settings/ui/
│   └── SettingsScreen.kt              # Verify email/name overflow in rows
├── auth/ui/
│   └── SignInScreen.kt                # Verify (already has ellipsis)
├── transactions/ui/
│   └── edit/TransactionEditScreen.kt  # Verify hero amount with large values
├── accounts/ui/
│   └── edit/AccountEditScreen.kt      # Verify name field with long text
└── categories/ui/
    └── edit/CategoryEditScreen.kt     # Verify name field with long text
```

**Structure Decision**: No new files or modules. All changes are modifications to existing composable files in `core:ui/components/` (shared) and `presentation/*/ui/` (screen-specific). This follows the existing layer-centric architecture.

## Implementation Strategy

### Approach: AutoSize for Amounts, Ellipsis for Text

**For monetary amounts** (balance, income, expense, transaction amount, stat values):
```kotlin
Text(
    text = formattedAmount,
    style = MoneyManagerTheme.typography.balanceDisplay,
    maxLines = 1,
    autoSize = TextAutoSize.StepBased(
        minFontSize = 14.sp,
        maxFontSize = 34.sp,
        stepSize = 1.sp
    )
)
```

**For text content** (descriptions, account names, category names, emails):
```kotlin
Text(
    text = description,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.weight(1f)  // take available space, don't push siblings
)
```

### Key Constraints

1. **Parent must provide bounded width** for autoSize to work — verify all parents have `fillMaxWidth()` or explicit width constraints
2. **BalanceCard** has animated balance via `AnimatedContent` — autoSize must be applied to the `Text` inside the animation scope
3. **TransactionListItem** — amount must NOT use ellipsis (change existing behavior). Description keeps ellipsis.
4. **Minimum font**: 12sp general, 11sp allowed for compact card contexts (IncomeExpenseCard, SummaryStatCard)
5. **Hero amount input** (MoneyManagerAmountField) — uses `BasicTextField`, not `Text`. AutoSize approach differs: need `TextFieldValue` with dynamic `TextStyle.fontSize` or wrapping in a container that auto-sizes

### Component-by-Component Plan

| # | Component | Change | Priority |
|---|-----------|--------|----------|
| 1 | BalanceCard | Add `autoSize(14sp..34sp)` to balance Text | P1 |
| 2 | IncomeExpenseCard | Add `autoSize(11sp..16sp)` to 3 amount Texts + balance | P1 |
| 3 | TransactionListItem | Replace ellipsis on amount with `autoSize(11sp..16sp)`. Keep ellipsis on description. | P1 |
| 4 | AccountCard | Add `autoSize(12sp..28sp)` to balance. Add ellipsis to name. | P2 |
| 5 | AccountListScreen | Add `autoSize(12sp..20sp)` to total balance header | P2 |
| 6 | SummaryStatCard | Add `autoSize(11sp..16sp)` to value Text | P2 |
| 7 | StatisticsScreen | Add `autoSize` to donut center total + category row amounts | P2 |
| 8 | ParsedTransactionItem | Add `autoSize` to amounts, ellipsis to descriptions | P2 |
| 9 | CategoryPicker | Add `maxLines=1` + ellipsis to category name labels | P2 |
| 10 | MoneyManagerAmountField | Add autoSize to hero amount display (30sp→16sp) | P2 |
| 11 | SettingsScreen | Verify/add ellipsis to email/name rows | P3 |
| 12 | SignInScreen | Verify existing ellipsis handling (likely OK) | P3 |
| 13 | TransactionEditScreen | Verify hero amount + form layout with extreme values | P2 |
| 14 | AccountEditScreen | Verify name field with 50+ chars | P2 |
| 15 | CategoryEditScreen | Verify name field with 30+ chars | P2 |

## Complexity Tracking

No constitution violations — this section is empty.
