# Quickstart: Statistics Screen Audit

## What this feature changes

Bug fixes and UX improvements for the existing Statistics screen. No new modules, no new libraries, no architecture changes.

## Files to modify

| File | Changes |
|------|---------|
| `domain/statistics/.../model/StatisticsModels.kt` | Add `MonthlyTotal`, `TransactionType`. Modify `PeriodSummary` (add income fields, monthly fields). Change `CategorySummary.percentage` from `Float` to `Int`. |
| `domain/statistics/.../usecase/GetPeriodSummaryUseCase.kt` | Fix `periodRange()` to use day-count subtraction. Add zero-day fill. Add monthly aggregation for YEAR. Add income-by-category computation. Implement largest-remainder percentage rounding. |
| `presentation/statistics/.../ui/StatisticsState.kt` | Add `error`, `transactionType`, income/monthly fields. |
| `presentation/statistics/.../ui/StatisticsViewModel.kt` | Add `.catch {}` error handling. Add `setTransactionType()`, `retry()`. Remove `takeLast()` hack. Add `distinctUntilChanged()`. |
| `presentation/statistics/.../ui/StatisticsScreen.kt` | Add error state UI. Add expense/income toggle. Fix donut animation key. Fix bar chart label overlap (skip labels). Add locale-aware date formatting. Fix donut center text overflow. |
| `presentation/statistics/.../ui/StatisticsRoute.kt` | Pass `onTransactionTypeChange` and `onRetry` callbacks. |

## How to verify

```bash
# Build
./gradlew :presentation:statistics:assembleDebug

# Unit tests
./gradlew :domain:statistics:test
./gradlew :presentation:statistics:test

# Full build + lint
./gradlew assembleDebug test lint detekt
```

## Key decisions

1. **YEAR bar chart**: 12 monthly bars (not 365 daily)
2. **Periods**: Rolling windows (last 7/30/365 days)
3. **Toggle**: Material 3 `SingleChoiceSegmentedButtonRow`
4. **Percentages**: Largest-remainder method, sum always = 100%
5. **Animation stability**: `distinctUntilChanged()` + content-derived LaunchedEffect key
