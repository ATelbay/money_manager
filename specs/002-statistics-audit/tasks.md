# Tasks: Statistics Screen Audit & Bug Fixes

**Input**: Design documents from `/specs/002-statistics-audit/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/statistics-ui.md

**Tests**: Not explicitly requested in spec. Test tasks omitted.

**Organization**: Tasks grouped by user story. US1+US2 combined (same UseCase fix). All file paths relative to repo root.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

```
domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/
presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/
```

---

## Phase 1: Foundational (Domain Model Changes)

**Purpose**: Update domain models and state that ALL user stories depend on. MUST complete before any story work.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T001 Add `TransactionType` enum (EXPENSE, INCOME) and `MonthlyTotal` data class to `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/model/StatisticsModels.kt`. Change `CategorySummary.percentage` type from `Float` to `Int`. Add `incomesByCategory: List<CategorySummary>`, `dailyIncome: List<DailyTotal>`, `monthlyExpenses: List<MonthlyTotal>`, `monthlyIncome: List<MonthlyTotal>` fields to `PeriodSummary`.

- [ ] T002 Update `StatisticsState` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsState.kt`: add `transactionType: TransactionType = TransactionType.EXPENSE`, `error: String? = null`, `incomesByCategory: ImmutableList<CategorySummary>`, `dailyIncome: ImmutableList<DailyTotal>`, `monthlyExpenses: ImmutableList<MonthlyTotal>`, `monthlyIncome: ImmutableList<MonthlyTotal>` fields.

**Checkpoint**: Models compile. All downstream code will have compile errors (expected — fixed in subsequent phases).

---

## Phase 2: US1+US2 — No Crash + Correct Period Range (Priority: P1) 🎯 MVP

**Goal**: Fix the bar chart crash on YEAR period (negative bar width from 365 bars) and fix the period date range off-by-one. Return zero-filled daily/monthly entries so the bar chart always has the correct count.

**Independent Test**: Switch between WEEK/MONTH/YEAR with varying transaction counts (0, 1, 100, 365+). Verify no crash and that transactions at 00:01 on the first day of the period are included.

### Implementation

- [ ] T003 [US1] [US2] Fix `periodRange()` in `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/GetPeriodSummaryUseCase.kt`: change MONTH to subtract 29 days (not `Calendar.MONTH`), change YEAR to subtract 364 days (not `Calendar.YEAR`). Set start time to 00:00:00.000 BEFORE subtracting days (current code subtracts from 23:59:59 then resets — set to 00:00 first, then subtract, to avoid any edge case). End remains at 23:59:59.999 of today.

- [ ] T004 [US1] [US2] Add zero-day fill logic in `GetPeriodSummaryUseCase`: after grouping expenses by day (`dayStart()`), iterate from period start to period end day-by-day, lookup the grouped map, default to `DailyTotal(dayTimestamp, 0.0)`. Result: exactly 7 entries for WEEK, 30 for MONTH. Apply same for income daily totals.

- [ ] T005 [US1] Add monthly aggregation logic in `GetPeriodSummaryUseCase` for YEAR period: group transactions by `Calendar.YEAR + Calendar.MONTH`, produce `MonthlyTotal(year, month, amount, label)` for each of the 12 months in the range. Fill missing months with zero. Use `SimpleDateFormat("MMM", Locale.getDefault())` for the `label` field.

- [ ] T006 [US1] Remove `takeLast()` hack from `StatisticsViewModel.kt` lines 47-52. The UseCase now returns the correct count of entries per period. Map all `PeriodSummary` fields directly to state including new `monthlyExpenses`, `monthlyIncome`, `dailyIncome`, `incomesByCategory` fields (convert each to `.toImmutableList()`).

**Checkpoint**: YEAR period no longer crashes. Period ranges are correct rolling windows. Bar chart data includes zero-transaction days/months.

---

## Phase 3: US3 — Error State (Priority: P1)

**Goal**: Display error message with retry button when data loading fails, instead of infinite loading spinner.

**Independent Test**: Simulate a DAO error (e.g., close database). Verify error state appears with "Retry" button. Tap retry → data reloads.

### Implementation

- [ ] T007 [US3] Add `.catch { e -> _state.update { it.copy(error = e.message ?: "Unknown error", isLoading = false) } }` to the Flow pipeline in `StatisticsViewModel.kt` `loadSummary()`, between `.onEach` and `.launchIn`. Add `fun retry()` method that sets `isLoading = true, error = null` and re-calls `loadSummary(current period)`.

- [ ] T008 [US3] Add error state UI in `StatisticsScreen.kt`: after the `isLoading` check and before the empty state check, add a condition for `state.error != null`. Show a Column with error icon (`Icons.Outlined.ErrorOutline`), error message text, and a "Повторить" (Retry) `OutlinedButton`. Keep PeriodSelector visible above the error. Add `testTag("statistics:error")` and `testTag("statistics:retryButton")`.

- [ ] T009 [US3] Update `StatisticsScreen` composable signature to accept `onRetry: () -> Unit` parameter. Update `StatisticsRoute.kt` to pass `viewModel::retry` as the `onRetry` callback.

**Checkpoint**: Error state renders on failure with working retry. Loading/success paths unchanged.

---

## Phase 4: US4 — Readable Bar Chart (Priority: P2)

**Goal**: Bar chart labels don't overlap at any period. WEEK shows day-of-week, MONTH shows every 5th day number, YEAR shows month abbreviations.

**Independent Test**: View bar chart at WEEK (7 bars), MONTH (30 bars), YEAR (12 bars). Verify all visible labels are legible and don't overlap.

### Implementation

- [ ] T010 [US4] Refactor `ExpenseBarChart` in `StatisticsScreen.kt` to accept a generic `entries: ImmutableList<BarEntry>` parameter instead of `dailyExpenses: ImmutableList<DailyTotal>`. Create a simple `BarEntry(label: String, amount: Double)` data class (private to the file or in StatisticsState). The caller maps `DailyTotal`/`MonthlyTotal` to `BarEntry` with pre-formatted labels.

- [ ] T011 [US4] Add label formatting logic at the call site in `StatisticsScreen.kt` where `ExpenseBarChart` is used. Based on `state.period`: WEEK → `SimpleDateFormat("EEE", Locale.getDefault())` from `DailyTotal.date`; MONTH → day number `SimpleDateFormat("d", Locale.getDefault())` from `DailyTotal.date`; YEAR → use `MonthlyTotal.label` directly. Pass the appropriate list (daily or monthly) mapped to `BarEntry`.

- [ ] T012 [US4] Add label-skip logic inside `ExpenseBarChart` Canvas drawing: accept a `labelInterval: Int` parameter (default 1). For MONTH set `labelInterval = 5`. Only draw the text label when `index % labelInterval == 0`. For WEEK and YEAR, use `labelInterval = 1` (show all labels).

- [ ] T013 [US4] Fix negative `labelX` in bar chart: after computing `labelX = x + (barWidth - measuredText.size.width) / 2f`, clamp it with `.coerceAtLeast(0f)` and `.coerceAtMost(size.width - measuredText.size.width)` to prevent text rendering outside canvas bounds.

**Checkpoint**: Bar chart renders readable labels at all periods. No overlap, no clipping.

---

## Phase 5: US5 — Expense/Income Toggle (Priority: P2)

**Goal**: Users can switch between expense and income views. All visualizations update accordingly.

**Independent Test**: Toggle between Расходы/Доходы. Verify donut chart, bar chart, category breakdown, and totals all switch to the selected type.

### Implementation

- [ ] T014 [US5] Extend `GetPeriodSummaryUseCase` to compute `incomesByCategory` (same logic as `expensesByCategory` but filtering `type == "income"` and using `totalIncome` as denominator for percentages). Also compute `dailyIncome` with zero-day fill and `monthlyIncome` with zero-month fill. Return all in `PeriodSummary`.

- [ ] T015 [US5] Add `fun setTransactionType(type: TransactionType)` to `StatisticsViewModel.kt`. Update state with the new type. No need to re-query — the `PeriodSummary` already contains both expense and income data. The Screen reads the appropriate fields based on `state.transactionType`.

- [ ] T016 [US5] Add `onTransactionTypeChange: (TransactionType) -> Unit` parameter to `StatisticsScreen` composable. Update `StatisticsRoute.kt` to pass `viewModel::setTransactionType`.

- [ ] T017 [US5] Add Material 3 `SingleChoiceSegmentedButtonRow` in `StatisticsScreen.kt` below the `PeriodSelector` item in the LazyColumn. Two segments: "Расходы" and "Доходы". Selected state from `state.transactionType`. Add `testTag("statistics:typeToggle")`. Use `MoneyManagerTheme.strings` for labels (add string constants if needed).

- [ ] T018 [US5] Update all chart/breakdown sections in `StatisticsScreen.kt` to read from the correct fields based on `state.transactionType`: when EXPENSE → use `expensesByCategory`, `dailyExpenses`, `monthlyExpenses`, `totalExpenses`; when INCOME → use `incomesByCategory`, `dailyIncome`, `monthlyIncome`, `totalIncome`. Update donut chart center label to show "Расходы"/"Доходы" accordingly. Update bar chart section title from hardcoded `s.expensesByDays` to a dynamic label.

- [ ] T019 [US5] Fix empty state condition in `StatisticsScreen.kt`: check based on current `transactionType`. When EXPENSE: empty if `expensesByCategory.isEmpty()`. When INCOME: empty if `incomesByCategory.isEmpty()`. Add `testTag("statistics:emptyState")` to empty state container.

**Checkpoint**: Expense/Income toggle works. All visualizations switch correctly. Empty state respects current type.

---

## Phase 6: US6 — Smooth Donut Animation (Priority: P2)

**Goal**: Donut chart animates once per data change, not on every Flow re-emission.

**Independent Test**: Switch periods and observe donut — animates once. Wait without interaction — no re-animation.

### Implementation

- [ ] T020 [US6] Add `distinctUntilChanged()` operator to the Flow pipeline in `StatisticsViewModel.kt` `loadSummary()`, after the `combine` in the UseCase returns. This prevents redundant state updates when the DAO re-emits identical data.

- [ ] T021 [US6] Change `LaunchedEffect(categories)` key in `DonutChart` composable (`StatisticsScreen.kt`) to use a stable content-derived key: `LaunchedEffect(categories.map { it.categoryId to it.totalAmount })`. This ensures animation only re-triggers when actual category data changes, not on list reference changes.

- [ ] T022 [US6] Apply same stable-key fix to `ExpenseBarChart` animation: change `LaunchedEffect(dailyExpenses)` to `LaunchedEffect(entries.map { it.amount })` (or equivalent for the refactored `BarEntry` list). Prevents bar chart re-animation on identical data.

**Checkpoint**: Donut and bar chart animate once per real data change. No flickering.

---

## Phase 7: US7 — Accurate Percentages (Priority: P3)

**Goal**: Category percentages sum to exactly 100% using largest-remainder rounding.

**Independent Test**: Create transactions in 3+ categories with amounts that cause fractional percentages. Verify displayed percentages sum to 100%.

### Implementation

- [ ] T023 [US7] Implement `largestRemainderRound(items: List<Pair<Long, Double>>, total: Double): Map<Long, Int>` private function in `GetPeriodSummaryUseCase.kt`. Algorithm: compute raw % for each item, floor to Int, compute deficit (100 - sum of floors), distribute +1 to items with largest fractional remainders. Ensure minimum 1% for any non-zero item (steal from the largest category if needed). Return map of categoryId → rounded percentage.

- [ ] T024 [US7] Replace inline percentage computation in `GetPeriodSummaryUseCase` `expensesByCategory` mapping (line ~44): call `largestRemainderRound()` and assign the result to each `CategorySummary.percentage`. Apply same for `incomesByCategory` computation (added in T014).

- [ ] T025 [US7] Update `CategoryBreakdownCard` in `StatisticsScreen.kt`: change `${summary.percentage.toInt()}%` to `${summary.percentage}%` (percentage is now already an `Int`, no `.toInt()` needed).

**Checkpoint**: Percentages always sum to 100%. No truncation artifacts.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Minor fixes that improve quality across multiple stories.

- [ ] T026 [P] Fix donut chart center text overflow in `StatisticsScreen.kt` `DonutChart` composable: add `maxLines = 1` and `overflow = TextOverflow.Ellipsis` to the amount `Text`. Optionally use `AutoSizeText` pattern or reduce font size for amounts > 10 characters.

- [ ] T027 [P] Update `StatisticsScreen` previews at the bottom of `StatisticsScreen.kt`: add preview for error state, income view, and YEAR period with monthly data. Ensure all previews compile with the new `StatisticsState` fields.

- [ ] T028 Verify full build and lint pass: run `./gradlew assembleDebug test lint detekt` and fix any issues.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Foundational)**: No dependencies — start immediately
- **Phase 2 (US1+US2)**: Depends on Phase 1 — model changes required
- **Phase 3 (US3)**: Depends on Phase 1 — needs `error` field in state
- **Phase 4 (US4)**: Depends on Phase 2 — needs zero-filled data and monthly aggregation
- **Phase 5 (US5)**: Depends on Phase 2 — needs income fields in PeriodSummary
- **Phase 6 (US6)**: Depends on Phase 2 — needs correct data flow to fix animation
- **Phase 7 (US7)**: Depends on Phase 1 — needs `Int` percentage type; also depends on T014 (income percentages)
- **Phase 8 (Polish)**: Depends on all previous phases

### User Story Dependencies

- **US1+US2 (P1)**: Start after Phase 1. No dependencies on other stories.
- **US3 (P1)**: Start after Phase 1. Independent of US1/US2.
- **US4 (P2)**: Depends on US1+US2 (needs zero-filled data and MonthlyTotal).
- **US5 (P2)**: Depends on US1+US2 (needs updated PeriodSummary structure).
- **US6 (P2)**: Can start after Phase 1, but best after US1+US2 (data flow stabilized).
- **US7 (P3)**: Can start after Phase 1. Independent, but T024 should follow T014.

### Parallel Opportunities

After Phase 1 completes:
- **US1+US2** and **US3** can run in parallel (different files: UseCase vs ViewModel error handling)
- **US6** and **US7** can run in parallel (different code sections)

After Phase 2 completes:
- **US4** and **US5** can run in parallel (bar chart vs toggle, mostly different code)

Within stories:
- T010 and T012 in US4 are parallelizable (different aspects of bar chart)
- T014 and T017 in US5 are parallelizable (UseCase vs UI)
- T026 and T027 in Polish are parallelizable

---

## Parallel Example: Phase 2 + Phase 3

```
# After Phase 1 completes, launch in parallel:

# Stream A: US1+US2 (UseCase fixes)
Task T003: Fix periodRange() in GetPeriodSummaryUseCase.kt
Task T004: Add zero-day fill in GetPeriodSummaryUseCase.kt
Task T005: Add monthly aggregation in GetPeriodSummaryUseCase.kt
Task T006: Remove takeLast hack in StatisticsViewModel.kt

# Stream B: US3 (Error handling)
Task T007: Add .catch and retry() in StatisticsViewModel.kt
Task T008: Add error state UI in StatisticsScreen.kt
Task T009: Update StatisticsRoute.kt with onRetry callback
```

---

## Implementation Strategy

### MVP First (US1+US2+US3 = P1 stories)

1. Complete Phase 1: Model changes
2. Complete Phase 2: US1+US2 — crash fix + period range fix
3. Complete Phase 3: US3 — error state
4. **STOP and VALIDATE**: All P1 bugs fixed, app doesn't crash, errors are handled
5. Build and run: `./gradlew assembleDebug`

### Incremental Delivery

1. Phase 1 → Models ready
2. Phase 2 (US1+US2) → Critical crash and data bugs fixed (MVP!)
3. Phase 3 (US3) → Error resilience added
4. Phase 4 (US4) → Bar chart polished
5. Phase 5 (US5) → Income visualization added
6. Phase 6 (US6) → Animations stabilized
7. Phase 7 (US7) → Percentages accurate
8. Phase 8 → Final polish and verification

Each phase adds value without breaking previous fixes.

---

## Notes

- All changes are within 2 existing modules: `domain:statistics` and `presentation:statistics`
- No new Gradle modules, no new libraries
- No navigation changes, no architecture changes
- `StatisticsScreen.kt` is the most-modified file — phases touching it should be sequential
- Commit after each phase for clean history
