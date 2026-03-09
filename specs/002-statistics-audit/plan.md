# Implementation Plan: Statistics Screen Audit & Bug Fixes

**Branch**: `002-statistics-audit` | **Date**: 2026-03-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-statistics-audit/spec.md`

## Summary

Audit and fix all bugs on the Statistics screen: crash on YEAR period (negative bar width), period range off-by-one, overlapping labels, missing zero-transaction days, flickering donut animation, no income visualization, incorrect percentage rounding, and missing error state. Add expense/income toggle. No architecture changes, no new libraries.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Material 3, Hilt 2.58, Room 2.8.4, kotlinx-collections-immutable
**Storage**: Room (TransactionDao, CategoryDao)
**Testing**: JUnit 4, MockK 1.14.9, Turbine 1.2.1, kotlinx-coroutines-test
**Target Platform**: Android (minSdk per project)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps chart rendering, no jank on period switches
**Constraints**: No new libraries, no architecture/navigation changes, existing module boundaries
**Scale/Scope**: 3 modules affected (domain:statistics, presentation:statistics, domain:statistics models)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | ✅ PASS | No new modules. Changes within existing `domain:statistics` and `presentation:statistics`. Layer boundaries preserved. |
| II. Kotlin-First & Jetpack Compose | ✅ PASS | All code is Kotlin + Compose. State hoisting maintained. New `testTag` modifiers will be added for error/toggle states. |
| III. Material 3 Design System | ✅ PASS | Expense/income toggle will use `SingleChoiceSegmentedButtonRow` (M3 component). Colors from `MaterialTheme`. |
| IV. Animation & Motion | ✅ PASS | Donut animation fix uses `MoneyManagerMotion` tokens. Bar chart animation already uses Motion tokens. No new custom easings. |
| V. Hilt DI | ✅ PASS | No DI changes needed. ViewModel already `@HiltViewModel`, UseCase already constructor-injected. |
| VI. Room Database | ✅ PASS | No schema changes. Using existing `observeByDateRange` DAO query. |
| VII. Testing Architecture | ✅ PASS | Will add ViewModel tests (initial, success, error), UseCase tests (period range, percentage rounding, income filtering). |
| VIII. Firebase Ecosystem | ✅ N/A | No Firebase changes. |
| IX. Type-Safe Navigation | ✅ N/A | No navigation changes. |
| X. Statement Import Pipeline | ✅ N/A | No import changes. |
| XI. Preferences DataStore | ✅ N/A | No preference changes. |

**Gate result: PASS** — No violations.

## Project Structure

### Documentation (this feature)

```text
specs/002-statistics-audit/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research output
├── data-model.md        # Phase 1 data model changes
├── contracts/           # Phase 1 UI contracts
│   └── statistics-ui.md # Screen states and composable contracts
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/
├── model/
│   └── StatisticsModels.kt    # MODIFY: add MonthlyTotal, incomesByCategory to PeriodSummary
└── usecase/
    └── GetPeriodSummaryUseCase.kt  # MODIFY: fix periodRange, add zero-day fill, monthly aggregation, income categories, percentage rounding

presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/
├── StatisticsState.kt      # MODIFY: add error, transactionType fields; add incomesByCategory, dailyIncome
├── StatisticsViewModel.kt  # MODIFY: add error handling (.catch), expense/income toggle, remove takeLast hack
├── StatisticsScreen.kt     # MODIFY: fix bar chart labels, donut animation key, add error state, toggle UI, locale dates
└── StatisticsRoute.kt      # MODIFY: pass new callbacks (onToggleType, onRetry)
```

**Structure Decision**: All changes within existing `domain:statistics` and `presentation:statistics` modules. No new modules or directories.

## Complexity Tracking

No constitution violations — this section intentionally empty.

---

## Detailed Bug Analysis & Fix Strategy

### BUG 1: Bar chart crash on YEAR (CRITICAL)

**Root cause**: `ExpenseBarChart` computes `naturalBarWidth = (size.width - barSpacing * (n-1)) / n`. With 365 bars and 6dp spacing: `size.width - 6*364 = negative` → negative bar width → crash in `drawRoundRect`.

**Fix**: For YEAR period, aggregate to 12 monthly bars in `GetPeriodSummaryUseCase`. For MONTH, aggregate to 30 daily bars (already daily). For WEEK, 7 daily bars. The use case returns the correct granularity; the chart just renders whatever it receives.

### BUG 2: periodRange off-by-one

**Root cause**: `periodRange()` sets end to 23:59:59.999, then calls `cal.add(DAY, -6)` which subtracts from 23:59:59 — so start is at 23:59:59 minus 6 days, then reset to 00:00. Actually after re-reading: it resets HOUR/MIN/SEC/MS to 0 after the subtraction. The issue is the `MONTH` period: `cal.add(Calendar.MONTH, -1)` from today 23:59:59 goes back exactly 1 calendar month, which may be 28-31 days, not the 30 days the VM expects.

**Fix**: Make periodRange return rolling periods consistently:
- WEEK: subtract 6 days → 7 days total (today inclusive)
- MONTH: subtract 29 days → 30 days total
- YEAR: subtract 364 days → 365 days total
Then reset start to 00:00:00.000. Remove the `takeLast()` hack from ViewModel since the use case will return the correct count.

### BUG 3: Label overlap at MONTH/YEAR

**Fix**: For MONTH (30 bars), show labels every 5th bar. For YEAR (12 monthly bars), show all labels (month names). For WEEK (7 bars), show all labels. Label rendering skips intermediate indices.

### BUG 4: Missing zero-transaction days

**Root cause**: `dailyExpenses` only includes days WITH transactions (groupBy on transaction dates).

**Fix**: In `GetPeriodSummaryUseCase`, after grouping by day, fill in missing days with `DailyTotal(date, 0.0)` for the entire period. For YEAR, fill missing months with `MonthlyTotal(year, month, 0.0)`.

### BUG 5: Donut animation flicker

**Root cause**: `LaunchedEffect(categories)` — `categories` is an `ImmutableList` recreated on every Flow emission, triggering animation restart.

**Fix**: Use `LaunchedEffect(categories.map { it.categoryId to it.totalAmount })` or a stable key derived from the data content (e.g., categories hashCode based on amounts). Or use `key = categories.size` + amounts hash.

### BUG 6: No income visualization

**Fix**: Add `TransactionType` enum (EXPENSE/INCOME) to state. Add toggle in UI (Material 3 `SingleChoiceSegmentedButtonRow`). UseCase already computes `totalIncome` and has access to income transactions — extend it to also compute `incomesByCategory` and `dailyIncome`. ViewModel selects which to display based on toggle.

### BUG 7: Percentage rounding ≠ 100%

**Root cause**: `percentage = (total / totalExpenses * 100).toFloat()` then displayed as `.toInt()%` (truncation).

**Fix**: Implement largest-remainder method in the use case:
1. Compute raw percentages
2. Floor each to integer
3. Distribute remaining points (100 - sum of floors) to categories with largest fractional remainders
4. Guarantee minimum 1% for non-zero categories

### BUG 8: No error state

**Root cause**: No `.catch {}` on the Flow in ViewModel. No error field in `StatisticsState`.

**Fix**: Add `error: String? = null` to state. Add `.catch { _state.update { it.copy(error = e.message, isLoading = false) } }` in ViewModel. Add error UI composable with retry button. Add `onRetry` callback.

### BUG 9: No expense/income toggle (already covered in BUG 6)

### Additional fixes identified during audit:
- **Locale date format**: Replace hardcoded `"dd.MM"` with locale-aware format
- **Donut center text overflow**: Add `maxLines = 1` and `TextOverflow.Ellipsis`, or use `autoSizeText` pattern
- **Empty state logic**: Fix condition to also check transaction type context
- **Bar chart animation**: Also use stable key to prevent unnecessary re-animation
