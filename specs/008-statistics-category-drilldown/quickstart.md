# Quickstart: Statistics UX Cleanup + Category Transaction Drill-Down

**Branch**: `008-statistics-category-drilldown` | **Date**: 2026-03-15

## Source Code Impact

### `app`

| File | Change |
|------|--------|
| `app/src/main/java/com/atelbay/money_manager/navigation/Destinations.kt` | Add typed drill-down route with preserved Statistics context |
| `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt` | Register drill-down route and wire navigation from Statistics to TransactionEdit |

### `presentation/statistics`

| File | Change |
|------|--------|
| `presentation/statistics/build.gradle.kts` | Add dependency on `domain:transactions` and `domain:categories` if required |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsRoute.kt` | Add category-click navigation callback |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` | Remove duplicate top toggle, make summary cards selectable, make category rows tappable |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsState.kt` | Preserve the resolved Statistics date range for navigation |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsViewModel.kt` | Expose exact range used by the current Statistics summary |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/CategoryTransactionsRoute.kt` | New route composable for drill-down |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/CategoryTransactionsViewModel.kt` | New ViewModel for filtered transaction list |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/CategoryTransactionsState.kt` | New UI state |
| `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/CategoryTransactionsScreen.kt` | New detail screen UI |

### `core/ui`

| File | Change |
|------|--------|
| `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/SummaryStatCard.kt` | Add optional click and selected-state support |

### `domain/statistics`

| File | Change |
|------|--------|
| `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/model/StatisticsModels.kt` | Add shared Statistics date-range model |
| `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/StatisticsPeriodRangeResolver.kt` | New shared Statistics range resolver |
| `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/GetPeriodSummaryUseCase.kt` | Replace private range math with shared resolver and expose resolved range |

### `domain/categories`

| File | Change |
|------|--------|
| `domain/categories/src/main/java/com/atelbay/money_manager/domain/categories/usecase/GetCategoryByIdUseCase.kt` | Reuse to prefer live category metadata in drill-down header with snapshot fallback |

### `domain:transactions`, `data:transactions`, `core:database`

| File | Change |
|------|--------|
| `domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/repository/TransactionRepository.kt` | Add filtered observation method |
| `domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/usecase/GetTransactionsByCategoryAndDateRangeUseCase.kt` | New use case for drill-down |
| `data/transactions/src/main/java/com/atelbay/money_manager/data/transactions/repository/TransactionRepositoryImpl.kt` | Implement filtered observation |
| `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/TransactionDao.kt` | Add category + type + date range query |

## Build & Verify

```bash
./gradlew :presentation:statistics:assembleDebug
./gradlew :app:assembleDebug
./gradlew test
```

## Manual Verification

1. Open Statistics and confirm the standalone top expense/income toggle is gone.
2. Confirm both summary cards remain visible and exactly one is visually active at a time.
3. Tap the expense and income summary cards and confirm the donut chart, bar chart, category breakdown, totals, center values, and empty state all switch with the selected card.
4. Tap the donut chart, donut slices, and legend and confirm they do not open drill-down.
5. Tap a category row from Statistics and confirm a dedicated detail screen opens inside the Statistics flow.
6. Verify the drill-down header reflects category identity plus the active expense/income and period context.
7. Verify every transaction in the drill-down belongs to the tapped category and is filtered to the exact same Statistics date range used at tap time.
8. If the current day changes while drill-down or transaction edit remains open, verify the drill-down list stays pinned to the original captured range until the user leaves that drill-down session.
9. Verify drill-down transactions are ordered newest first.
10. Tap a transaction from drill-down and confirm the existing `TransactionEdit` flow opens.
11. Rename or recolor the selected category through the existing flow if feasible, then verify the drill-down header prefers the updated category metadata and falls back to the tapped snapshot only when the category cannot be resolved.
12. Edit or delete a matching transaction and verify drill-down refreshes, the empty state appears when the filtered list becomes empty, and returning to Statistics keeps the same period and type selected with updated aggregates.
