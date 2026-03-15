# Tasks: Statistics UX Cleanup + Category Transaction Drill-Down

**Input**: Design documents from `/specs/008-statistics-category-drilldown/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not explicitly requested. Include build verification and targeted manual verification.

**Organization**: Tasks grouped by user story and blocking infrastructure.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel
- **[Story]**: User story reference from `spec.md`
- Exact file paths are included

## Path Conventions

```text
app/src/main/java/com/atelbay/money_manager/navigation/
presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/
core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/
domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/
domain/categories/src/main/java/com/atelbay/money_manager/domain/categories/
domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/
data/transactions/src/main/java/com/atelbay/money_manager/data/transactions/
core/database/src/main/java/com/atelbay/money_manager/core/database/dao/
```

---

## Phase 1: Shared Statistics Context Foundation

**Purpose**: Create the exact-range preservation and filtered data primitives that both Statistics and drill-down depend on.

- [ ] T001 Add `StatisticsDateRange` to `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/model/StatisticsModels.kt` and add `StatisticsPeriodRangeResolver.kt` in `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/`.

- [ ] T002 Update `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/GetPeriodSummaryUseCase.kt` to use the shared resolver and emit the resolved range with `PeriodSummary`.

- [ ] T003 Update `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsState.kt` and `StatisticsViewModel.kt` to retain the exact resolved range currently visible on Statistics.

- [ ] T004 Add `observeByCategoryTypeAndDateRange(categoryId, type, startDate, endDate)` to `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/TransactionDao.kt`.

- [ ] T005 Update `domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/repository/TransactionRepository.kt` and `data/transactions/src/main/java/com/atelbay/money_manager/data/transactions/repository/TransactionRepositoryImpl.kt` to expose and implement category + type + date range observation.

- [ ] T006 Add `GetTransactionsByCategoryAndDateRangeUseCase.kt` to `domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/usecase/`.

**Checkpoint**: Statistics can preserve the exact date window currently on screen, and a ViewModel can observe only transactions that match one category, one type, and that exact range.

---

## Phase 2: User Story 1 - One clear selector in Statistics (Priority: P1) 🎯 MVP

**Goal**: Remove the duplicate type selector and make the existing summary cards the only selector.

**Independent Test**: Open Statistics and verify the standalone top toggle is gone, both summary cards remain visible, and only the summary cards control expense/income selection.

- [ ] T007 [US1] Extend `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/SummaryStatCard.kt` with optional `selected` and `onClick` support while preserving current passive-card behavior for other screens.

- [ ] T008 [US1] Remove the standalone transaction-type toggle row from `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`, including loading, empty, and error-state variants.

- [ ] T009 [US1] Update the summary card row in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` so the expense and income cards call `onTransactionTypeChange(...)`, remain simultaneously visible, expose clear selected/inactive styling, and include explicit test tags for both selectable cards.

**Checkpoint**: Statistics has one clear type selector, no duplicate top control, and both cards remain visible with explicit active state.

---

## Phase 3: User Story 2 - Statistics visuals stay in sync (Priority: P1)

**Goal**: Ensure every Statistics visualization responds consistently to the selected card.

**Independent Test**: Switch between expense and income using the summary cards and verify the donut chart, bar chart, category breakdown, totals, center values, and empty state all change together.

- [ ] T010 [US2] Review `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` and ensure all visible charts, totals, labels, and empty-state branching derive from the same active `transactionType`.

- [ ] T011 [US2] Add or update test tags and previews in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` so card-driven switching can be manually or UI-tested without ambiguity.

**Checkpoint**: No stale expense/income content remains visible after a selector change.

---

## Phase 4: User Story 3 - Category drill-down entry from Statistics (Priority: P1)

**Goal**: Make category rows the sole drill-down entry point and wire a dedicated route into the app.

**Independent Test**: Tap a category row from Statistics and verify navigation to a dedicated detail screen inside the Statistics flow. Confirm the donut chart and legend do not navigate.

- [ ] T012 [US3] Add a typed drill-down destination to `app/src/main/java/com/atelbay/money_manager/navigation/Destinations.kt` using primitive route arguments for category metadata plus preserved Statistics context (`transactionType`, `period`, `startMillis`, `endMillis`).

- [ ] T013 [US3] Update `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt` to register the new route, navigate from `StatisticsRoute`, and forward transaction clicks from the drill-down screen to the existing `TransactionEdit` route.

- [ ] T014 [US3] Update `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsRoute.kt` and `StatisticsScreen.kt` to add `onCategoryClick` wiring from the currently visible category breakdown.

- [ ] T015 [US3] Refactor the category breakdown section in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` so each row is visibly tappable and emits category metadata plus the preserved Statistics context, while the donut chart and legend remain passive.

**Checkpoint**: Category rows are the only drill-down trigger and they navigate with the exact Statistics context.

---

## Phase 5: User Story 4 - Dedicated drill-down screen and filtered list (Priority: P2)

**Goal**: Implement the new detail screen that shows filtered transactions with clear category and statistics context.

**Independent Test**: Open drill-down from Statistics and verify the screen shows category identity, expense/income + period context, a newest-first filtered list, and a proper empty state when needed.

- [ ] T016 [US4] Add `implementation(projects.domain.transactions)` and `implementation(projects.domain.categories)` to `presentation/statistics/build.gradle.kts` if required by the new ViewModel or route.

- [ ] T017 [US4] Create `CategoryTransactionsState.kt`, `CategoryTransactionsViewModel.kt`, `CategoryTransactionsRoute.kt`, and `CategoryTransactionsScreen.kt` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/`.

- [ ] T018 [US4] Implement `CategoryTransactionsViewModel.kt` to read route args, map statistics type strings to the transactions-domain type, and observe filtered transactions via `GetTransactionsByCategoryAndDateRangeUseCase` using the preserved `startMillis` and `endMillis`, keeping that range fixed for the lifetime of the drill-down session.

- [ ] T019 [US4] Use `domain/categories/src/main/java/com/atelbay/money_manager/domain/categories/usecase/GetCategoryByIdUseCase.kt` from `CategoryTransactionsViewModel.kt` so the drill-down header prefers live category metadata and falls back to the tapped route snapshot when the category cannot be resolved.

- [ ] T020 [US4] Implement `CategoryTransactionsScreen.kt` to show:
  - a top app bar with back navigation
  - category name plus icon and color or accent when available
  - visible context for active type and selected period
  - transaction list ordered newest-first using shared transaction row UI
  - test tags for screen, list, and empty state

- [ ] T021 [US4] Wire transaction row taps from `CategoryTransactionsScreen.kt` through `CategoryTransactionsRoute.kt` and `MoneyManagerNavHost.kt` into the existing `TransactionEdit(id)` navigation flow.

**Checkpoint**: Drill-down screen loads only matching transactions, shows the correct context, and behaves as a Statistics detail screen.

---

## Phase 6: User Story 4 - Sync after edits and return-path consistency

**Goal**: Ensure the filtered list and originating Statistics screen stay correct after edits or deletions.

**Independent Test**: Edit or delete a transaction from drill-down, return to Statistics, and verify both list and aggregates refresh while the same period and type remain selected.

- [ ] T022 [US4] Ensure `CategoryTransactionsViewModel.kt` derives empty state and filtered totals reactively from the filtered Flow.

- [ ] T023 [US4] Verify `StatisticsRoute`, `StatisticsViewModel`, and navigation back-stack behavior preserve the active period and transaction type on return without manual reset logic, while allowing Statistics to resume its normal live current-period range when shown again.

- [ ] T024 [US4] Verify edits and deletions triggered through `TransactionEdit` propagate back to both drill-down and Statistics via existing reactive data flows.

**Checkpoint**: The last-item-deleted case is handled cleanly, and Statistics returns in the same context with updated data.

---

## Phase 7: Verification and Cleanup

- [ ] T025 [P] Clean up imports, previews, and test tags in `SummaryStatCard.kt`, `StatisticsScreen.kt`, and the new drill-down files.

- [ ] T026 Build verification: run `./gradlew :presentation:statistics:assembleDebug :app:assembleDebug` and fix compile issues.

- [ ] T027 Run `./gradlew test` and fix regressions in touched modules.

- [ ] T028 Manual verification per `specs/008-statistics-category-drilldown/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1**: No dependencies
- **Phase 2**: Depends on Phase 1 for preserved date-range state if navigation wiring is added early
- **Phase 3**: Depends on Phase 2 because all visible Statistics content must already pivot cleanly on the selected type
- **Phase 4**: Depends on Phase 1 and Phase 2
- **Phase 5**: Depends on Phase 4
- **Phase 6**: Depends on Phase 5
- **Phase 7**: Depends on all previous phases

### Parallel Opportunities

- T001 and T004 can start in parallel
- T005 and T007 can run in parallel after T004 is underway because they touch different layers
- T012 and T015 can run in parallel after the route shape is agreed
- T017 screen scaffolding and T018 ViewModel data flow can run in parallel

## Implementation Strategy

### MVP First

1. Finish Phase 1
2. Finish Phase 2
3. Finish Phase 3
4. Validate that Statistics now has one selector with synchronized visuals
5. Finish Phase 4 and verify category-row navigation with exact preserved context

### Incremental Delivery

1. Exact-range foundation
2. Selector cleanup and synchronized visuals
3. Row-only drill-down navigation
4. Drill-down screen implementation
5. Edit/delete consistency and final verification
