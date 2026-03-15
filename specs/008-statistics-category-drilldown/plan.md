# Implementation Plan: Statistics UX Cleanup + Category Transaction Drill-Down

**Branch**: `008-statistics-category-drilldown` | **Date**: 2026-03-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-statistics-category-drilldown/spec.md`

## Summary

Refine the Statistics screen so it has one clear transaction-type selector and a dedicated category drill-down flow. The summary cards become the only expense/income selector, the duplicate top toggle is removed, category rows become the sole drill-down entry point, and drill-down preserves the exact Statistics context the user tapped from, including the resolved date range.

## Technical Context

**Language/Version**: Kotlin 2.3.0  
**Primary Dependencies**: Jetpack Compose BOM 2026.01.01, Navigation Compose 2.9.7, Material 3, Hilt, Room 2.8.4  
**Storage**: Room database (existing transactions table and DAO)  
**Testing**: JUnit 4, Compose previews, existing `app` screenshot and Compose tests  
**Target Platform**: Android  
**Project Type**: Multi-module mobile app  
**Performance Goals**: Reactive updates with no full-list in-memory filtering of all transactions  
**Constraints**: No new libraries; preserve existing Statistics charts; do not add inline expansion; do not route drill-down through a different top-level tab  
**Scale/Scope**: 8 modules affected (`app`, `presentation:statistics`, `core:ui`, `domain:statistics`, `domain:transactions`, `domain:categories`, `data:transactions`, `core:database`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | Cross-feature work stays inside existing module boundaries |
| II. Kotlin-First & Compose | PASS | Compose UI, Flow state, Kotlin domain changes |
| III. Material 3 Design System | PASS | Reuses existing card, scaffold, and top app bar patterns |
| IV. Animation & Motion | PASS | Existing motion patterns stay intact; no new complex motion system |
| V. Hilt DI | PASS | New ViewModel and use case follow current Hilt wiring |
| VI. Room Database | PASS | Reuses existing table with one additional filtered query |
| VII. Testing Architecture | PASS | Existing UI and unit verification patterns remain applicable |
| VIII. Firebase Ecosystem | N/A | No Firebase changes |
| IX. Type-Safe Navigation | PASS | Drill-down uses existing serializable route pattern |
| X. Statement Import Pipeline | N/A | No import changes |
| XI. Preferences DataStore | PASS | Flow remains local to Statistics and avoids Home-specific account behavior |

**Result**: All applicable gates PASS. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/008-statistics-category-drilldown/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── tasks.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
app/src/main/java/com/atelbay/money_manager/navigation/
├── Destinations.kt                              # MODIFY: add drill-down route with preserved stats context
└── MoneyManagerNavHost.kt                       # MODIFY: register drill-down route and callbacks

presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/
├── StatisticsRoute.kt                           # MODIFY: navigation callback wiring
├── StatisticsScreen.kt                          # MODIFY: remove duplicate toggle, make cards selectable, make rows tappable
├── StatisticsState.kt                           # MODIFY: preserve resolved statistics date range for navigation
├── StatisticsViewModel.kt                       # MODIFY: expose exact range used by the loaded summary
├── CategoryTransactionsRoute.kt                 # NEW
├── CategoryTransactionsViewModel.kt             # NEW
├── CategoryTransactionsState.kt                 # NEW
└── CategoryTransactionsScreen.kt                # NEW

core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/
└── SummaryStatCard.kt                           # MODIFY: selected and click state support

domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/
├── model/StatisticsModels.kt                    # MODIFY: shared date-range model in summary state
└── usecase/
    ├── GetPeriodSummaryUseCase.kt               # MODIFY: emit resolved date range with summary
    └── StatisticsPeriodRangeResolver.kt         # NEW

domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/
├── repository/TransactionRepository.kt          # MODIFY: filtered observation API
└── usecase/GetTransactionsByCategoryAndDateRangeUseCase.kt  # NEW

domain/categories/src/main/java/com/atelbay/money_manager/domain/categories/usecase/
└── GetCategoryByIdUseCase.kt                    # REUSE: prefer live category metadata in drill-down header

data/transactions/src/main/java/com/atelbay/money_manager/data/transactions/repository/
└── TransactionRepositoryImpl.kt                 # MODIFY: filtered observation implementation

core/database/src/main/java/com/atelbay/money_manager/core/database/dao/
└── TransactionDao.kt                            # MODIFY: category + type + date range query
```

**Structure Decision**: Keep the drill-down screen in `presentation:statistics` because it is a child flow of Statistics, not a reuse of Home. Reuse shared transaction row UI rather than moving the user into a top-level transaction list.

## Design Decisions

### D1: Summary cards are the only type selector

**Decision**: Remove the standalone `TransactionTypeToggle` row and make the existing expense and income summary cards the only selector.

**Rationale**: The screen already exposes both totals. Reusing those cards removes duplication while preserving side-by-side comparison.

### D2: Both cards stay visible, but only one is active

**Decision**: Keep both summary cards visible at all times, with explicit selected styling for the active card and subdued styling for the inactive card.

**Rationale**: The user still needs comparative context between expense and income, but the active dataset must be obvious.

### D3: Category rows are the only drill-down trigger

**Decision**: Make only category rows tappable. Keep the donut chart, slices, and legend passive.

**Rationale**: This avoids accidental secondary entry points and matches the requested scope boundary.

### D4: Dedicated Statistics detail route

**Decision**: Add a dedicated route for category drill-down inside the Statistics flow rather than reusing Home navigation or inline expansion.

**Rationale**: This keeps the overview screen focused, avoids top-level tab switching, and prevents Home-specific list behavior from leaking into Statistics.

### D5: Preserve the exact resolved date range from Statistics

**Decision**: Introduce a shared range resolver for Statistics summary generation, store the resolved range in Statistics state, and pass the exact `startMillis` and `endMillis` into drill-down route arguments.

**Rationale**: Recomputing the range from period alone in drill-down risks boundary drift. Passing the resolved range guarantees the detail screen matches the exact summary window the user tapped from.

### D6: Keep drill-down pinned to the captured range for the whole session

**Decision**: Once drill-down opens, it keeps using the captured `startMillis` and `endMillis` until the user leaves that drill-down session, even if the day changes while they remain inside drill-down or transaction edit.

**Rationale**: The detail screen represents a preserved Statistics context snapshot, not a continuously moving window.

### D7: Filter in the data layer

**Decision**: Add DAO and repository support for category + type + date range filtering.

**Rationale**: This keeps the drill-down reactive and avoids collecting all transactions in memory just to filter them in UI code.

### D8: Use a thin type mapper at the boundary

**Decision**: Keep the current statistics transaction type and core transaction type models separate for now, and map between them at the drill-down boundary.

**Rationale**: Full type unification is broader than this feature and would expand risk without improving the user-visible outcome.

### D9: Prefer live category metadata, fall back to route snapshot

**Decision**: Resolve category metadata by id on the drill-down screen and prefer the latest category identity when available, while keeping the route snapshot for fallback.

**Rationale**: This preserves a stable header in empty or deleted-category cases without leaving the header stale after category edits.

## Implementation Approach

### Step 1: Shared statistics context foundation

Add a reusable `StatisticsPeriodRangeResolver`, update `GetPeriodSummaryUseCase` to emit the resolved date range with the summary, and retain that range in `StatisticsState` so navigation can preserve the exact Statistics context.

### Step 2: Selector cleanup on Statistics

Extend `SummaryStatCard` with optional click and selected state, remove the duplicate toggle from `StatisticsScreen`, and wire both summary cards to `onTransactionTypeChange(...)` while keeping their totals visible.

### Step 3: Row-only drill-down navigation

Add a typed navigation destination that carries category presentation metadata plus the preserved Statistics context (`type`, `period`, `startMillis`, `endMillis`). Make only category rows emit the navigation callback.

### Step 4: Drill-down screen

Implement a dedicated `CategoryTransactionsViewModel` and `CategoryTransactionsScreen` that:

- observe only transactions matching the preserved category, type, and date range
- keep that preserved range fixed for the life of the drill-down session
- resolve the latest category metadata when available and fall back to route snapshot metadata otherwise
- show category name, icon, and color or accent when available
- show the active type and period context clearly
- render transactions newest first using existing shared transaction row UI
- open `TransactionEdit` on transaction tap
- show a proper empty state when no transactions match

### Step 5: Return-path consistency

Keep Statistics on the back stack so its selected period and type remain unchanged on return. Rely on reactive data updates so edits and deletions made from drill-down are reflected in both drill-down and Statistics without manual refresh entry points. When Statistics is shown again, it may resume its normal live current-period range resolution for the still-selected period.

## Complexity Tracking

No constitution violations. Complexity is moderate because the feature spans Statistics UI, navigation, state preservation, and one new filtered data path, but it does not require schema changes or new modules.
