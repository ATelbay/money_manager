# Tasks: Statistics Screen UI Redesign

**Input**: Design documents from `/specs/010-statistics-ui-redesign/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not explicitly requested — test tasks omitted. Only ViewModel/domain unit tests included where new logic is added.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **core:ui**: `core/ui/src/main/java/com/atelbay/money_manager/core/ui/`
- **domain:statistics**: `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/`
- **presentation:statistics**: `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/`
- **presentation:statistics test**: `presentation/statistics/src/test/java/com/atelbay/money_manager/presentation/statistics/`
- **domain:statistics test**: `domain/statistics/src/test/java/com/atelbay/money_manager/domain/statistics/`

---

## Phase 1: Setup

**Purpose**: No new project setup needed — existing multi-module project. This phase ensures the branch is ready.

- [ ] T001 Verify clean build on branch `010-statistics-ui-redesign` by running `./gradlew assembleDebug`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Update shared design tokens and components that ALL user stories depend on. These changes affect `core:ui` which is a dependency of every presentation module.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T002 Update design token color values in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/Color.kt`: change `BackgroundLight` from `0xFFF5F5F7` → `0xFFF5F4F1`, `BackgroundDark` from `0xFF0D0D0D` → `0xFF1A1918`, `SurfaceDark` from `0xFF1A1A1A` → `0xFF2A2928`, `TextPrimaryLight` from `0xFF1A1A1A` → `0xFF1A1918`, `TextSecondaryLight` from `0x80000000` → `0xFF9C9B99`, add `TextSecondaryDark = 0xFF6D6C6A`, `ExpenseCoral` from `0xFFFF6B6B` → `0xFFD08068`, update/add `DividerLight = 0xFFEDECEA` and `DividerDark = 0xFF3A3938`, add `GreenAccentLight = 0xFF3D8A5A` and `GreenAccentDark = 0xFF4DB87A` to `MoneyManagerColors`

- [ ] T003 Refactor `MoneyManagerSegmentedButton` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/MoneyManagerSegmentedButton.kt`: change active bg from `Teal` to `colors.surface` (white in light, dark surface in dark) with subtle shadow (`elevation 2.dp`), active text to `textPrimary` with `FontWeight.SemiBold`, inactive text to muted token, add `Modifier.weight(1f)` to each segment for equal-width tabs, add optional `height: Dp = Dp.Unspecified` parameter, update the Preview to reflect new styling

- [ ] T004 Verify build still passes after foundational changes by running `./gradlew assembleDebug test`

**Checkpoint**: Foundation ready — design tokens and shared components updated. User story implementation can now begin.

---

## Phase 3: User Story 1 — Unified Chart Card with Expense/Income Toggle (Priority: P1) 🎯 MVP

**Goal**: Consolidate bar chart, expense/income toggle, and total amount into a single elevated card. Replace the two `SummaryStatCard` composables with small toggle pills in the card header. Add scroll indicator below the chart.

**Independent Test**: Switch between Expense/Income pills and verify chart, title, subtitle, and total row update correctly for each type.

### Implementation for User Story 1

- [ ] T005 [US1] Create a new private composable `ExpenseIncomeToggle` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` — a small rounded-full segmented control with two pills ("Expenses" / "Income"), using animated background/text colors from design tokens (use `MoneyManagerMotion` duration/easing constants for color transitions), `12.sp` text, `padding(4.dp)`, `gap(4.dp)`. Active pill: surface fill + shadow (1.dp elevation). Inactive: transparent + muted text. Takes `selected: TransactionType` and `onSelect: (TransactionType) -> Unit`. Add `testTag("statistics:expenseIncomeToggle")`

- [ ] T006 [US1] Create a new private composable `ChartCardHeader` in `StatisticsScreen.kt` — a Row with: left side showing chart title (`16.sp`, semibold) + date range subtitle (`12.sp`, muted) in a Column, right side showing `ExpenseIncomeToggle`. Takes `title: String`, `dateRange: String`, `selectedType: TransactionType`, `onTypeChange: (TransactionType) -> Unit`

- [ ] T007 [US1] Create a new private composable `ChartScrollIndicator` in `StatisticsScreen.kt` — centered horizontal indicator: track (60dp wide, 3dp tall, rounded, divider color fill) + thumb (24dp wide, proportional position based on `scrollFraction: Float`, muted color fill). Only renders when `isVisible: Boolean` is true. Add `testTag("statistics:scrollIndicator")`

- [ ] T008 [US1] Create a new private composable `ChartTotalRow` in `StatisticsScreen.kt` — a Row with top padding (12dp) acting as divider area: left side "Total expenses" or "Total income" label (`14.sp`, muted), right side formatted amount (`22.sp`, bold, `-0.3.sp` letterSpacing). Takes `label: String`, `amount: Double?`, `moneyDisplay: MoneyDisplayPresentation`

- [ ] T009 [US1] Create a new private composable `UnifiedChartCard` in `StatisticsScreen.kt` that combines: `ChartCardHeader` + existing `VicoBarChartSection` (chart area, height 180dp) + `ChartScrollIndicator` (driven by Vico's `scrollState.value / scrollState.maxValue`, visible only when `chart.isScrollable`) + `ChartTotalRow`. Card styling: `RoundedCornerShape(16.dp)`, surface fill, `padding(20.dp)`, `gap(16.dp)`, shadow (2.dp elevation). Takes all parameters needed by its children plus `chartModelProducer`, `barColor`, `period`. Preserve existing testTags (`statistics:chartTitle`, `statistics:chartDateRange`, `statistics:barChart`, `statistics:monthChartContainer`, `statistics:barChartUnavailable`)

- [ ] T010 [US1] Refactor the main `StatisticsScreen` composable's `LazyColumn` in `StatisticsScreen.kt`: remove the `"totals"` item (which renders `StatisticsTypeCards`), remove the separate `"bar"` item, and replace them with a single `"chart_card"` item that renders `UnifiedChartCard`. Pass `onTransactionTypeChange` to the toggle pills inside the card. Keep the `"period"` item above. Preserve all existing testTags

- [ ] T011 [US1] Extract `scrollState` from inside `VicoBarChartSection` so it can be observed by the parent `UnifiedChartCard` for the scroll indicator. Either lift `rememberVicoScrollState()` to `UnifiedChartCard` and pass it down, or expose scroll fraction via a callback parameter on `VicoBarChartSection`

**Checkpoint**: Chart card with toggle pills, scroll indicator, and total row is functional. The two SummaryStatCards are no longer rendered.

---

## Phase 4: User Story 2 — Segmented Period Selector (Priority: P1)

**Goal**: Replace the `PeriodSelector` chip-based Row with the refactored `MoneyManagerSegmentedButton` as a polished segmented control.

**Independent Test**: Tap each period tab and verify visual state changes (active highlight, text weight) and data reload.

### Implementation for User Story 2

- [ ] T012 [US2] Rewrite the `PeriodSelector` composable in `StatisticsScreen.kt`: replace the `Row` of `MoneyManagerChip` composables with `MoneyManagerSegmentedButton(options = listOf("Week", "Month", "Year"), selectedOption = selected.displayName, onOptionSelected = { ... })`. Map option strings back to `StatsPeriod` enum values. Set container height to `40.dp` via the new height parameter. Preserve testTag `"statistics:periodSelector"` on the outer modifier. Preserve individual period testTags (`statistics:period_WEEK`, etc.) by adding testTag to each segment — this may require adding a `testTagPrefix: String?` parameter to `MoneyManagerSegmentedButton`

- [ ] T013 [US2] Add `testTagPrefix: String? = null` parameter to `MoneyManagerSegmentedButton` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/MoneyManagerSegmentedButton.kt` so each segment Box gets `testTag("${prefix}_${option}")` when prefix is non-null. This preserves the existing `statistics:period_WEEK` / `statistics:period_MONTH` / `statistics:period_YEAR` tags

**Checkpoint**: Period selector renders as a segmented control. All period-related testTags preserved.

---

## Phase 5: User Story 3 — Compact Donut Chart with Side Legend (Priority: P2)

**Goal**: Restructure the "By Category" section: donut chart on the left, vertical legend (top 3 + "Other") on the right, "See all" / "Show less" toggle for inline expansion.

**Independent Test**: Verify donut + legend layout renders correctly with categories. Tap "See all" to expand, "Show less" to collapse.

### Implementation for User Story 3

- [ ] T014 [P] [US3] Create a new private composable `CategoryLegendRow` in `StatisticsScreen.kt` — a single legend row: colored dot (`8.dp` circle, `Canvas` or `Box` with `CircleShape`), category name (`13.sp`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`), percentage text (`13.sp`, muted). Takes `color: Color`, `name: String`, `percentage: String`

- [ ] T015 [P] [US3] Create a new private composable `CategoryLegend` in `StatisticsScreen.kt` — a vertical Column with `gap(8.dp)`, centered vertically. Accepts `categories: ImmutableList<StatisticsCategoryDisplayItem>`. Shows top 3 categories as `CategoryLegendRow`. If more than 3 exist, aggregates the rest into an "Other" row with a neutral gray color and summed percentage. Add `testTag("statistics:categoryLegend")`

- [ ] T016 [US3] Create a new private composable `CompactDonutCard` in `StatisticsScreen.kt` — a card (`RoundedCornerShape(16.dp)`, surface fill, `padding(20.dp)`, shadow (2.dp elevation)) containing a horizontal Row (`height = 120.dp`, `gap = 16.dp`): left side is the existing `DonutChart` composable (`120x120.dp`, fixed size), right side is `CategoryLegend` (fill remaining width). Keep existing Canvas `drawArc()` implementation. Center text in donut: total amount (auto-sized 12–20sp) + label. Preserve testTag `"statistics:pieChart"`

- [ ] T017 [US3] Create a new private composable `ByCategorySection` in `StatisticsScreen.kt` — wraps the section header Row (space-between: "By Category" text `18.sp` semibold + "See all"/"Show less" clickable text `13.sp` green accent color) and `CompactDonutCard`. Manages a local `var expanded by remember { mutableStateOf(false) }` state. When expanded: use `AnimatedVisibility` (with `MoneyManagerMotion.enterTransition`/`exitTransition` specs) to show the full category list below the donut card (reuse existing `CategoryBreakdownCard` content — the clickable category rows with amounts, icons, and percentages). "See all" toggles to "Show less" when expanded. Add `testTag("statistics:byCategorySection")` and `testTag("statistics:seeAllButton")`

- [ ] T018 [US3] Update the `StatisticsScreen` `LazyColumn` in `StatisticsScreen.kt`: replace the `"donut"` and `"breakdown"` items with a single `"categories"` item rendering `ByCategorySection`. Pass existing `onCategoryClick` for drill-down on individual category tap. Preserve existing `statistics:category_{id}` testTags on individual category rows in the expanded view

**Checkpoint**: Category section shows compact donut + legend. "See all" expands inline. All category-related testTags preserved.

---

## Phase 6: User Story 4 — Calendar Filter Button in Header (Priority: P2)

**Goal**: Add a calendar filter pill in the header that opens a month picker, allowing the user to override the default period range with a custom month anchor.

**Independent Test**: Tap calendar pill, select a month, verify statistics refresh for the custom range. Switch period tab to verify custom range clears.

**⚠️ Note**: T025 (header update) depends on T010 (US1) completing first — the header Row created in T010 is what T025 modifies.

### Implementation for User Story 4

- [ ] T019 [P] [US4] Add optional `anchorMillis: Long? = null` parameter to `StatisticsPeriodRangeResolver.invoke()` in `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/StatisticsPeriodRangeResolver.kt`. When non-null, use `anchorMillis` as the base timestamp instead of `Calendar.getInstance()`. Default (null) preserves current behavior (relative to "now")

- [ ] T020 [P] [US4] Add optional `anchorMillis: Long? = null` parameter to `GetPeriodSummaryUseCase.invoke()` in `domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/GetPeriodSummaryUseCase.kt`. Pass it through to `rangeResolver(period, anchorMillis)`

- [ ] T021 [P] [US4] Add `selectedMonth: YearMonth? = null` field to `StatisticsState` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsState.kt` (use `java.time.YearMonth`). This represents the custom month anchor from the calendar picker

- [ ] T022 [US4] Add `setMonth(yearMonth: YearMonth?)` function to `StatisticsViewModel` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsViewModel.kt`. When called with a non-null value: update `_state` with `selectedMonth`, compute `anchorMillis` from the YearMonth (1st day of month, midnight), and call `loadSummary(currentPeriod, anchorMillis)`. When called with null: clear `selectedMonth` and reload with default. Update `setPeriod()` to also reset `selectedMonth` to null (switching tabs clears custom range). Update `loadSummary()` to accept and pass `anchorMillis` to `getPeriodSummaryUseCase`

- [ ] T023 [US4] Create `MonthPickerDialog` composable in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/MonthPickerDialog.kt` — a custom dialog with: year selector (left/right arrows + year text), 3×4 month grid (Jan–Dec), current month highlighted, selected month with accent color. Takes `initialYearMonth: YearMonth`, `onMonthSelected: (YearMonth) -> Unit`, `onDismiss: () -> Unit`. Style with design tokens. Add `testTag("statistics:monthPicker")`

- [ ] T024 [US4] Create a private composable `CalendarFilterPill` in `StatisticsScreen.kt` — rounded-full surface fill, `padding(horizontal = 12.dp, vertical = 8.dp)`, `gap(6.dp)`: calendar icon (`16.dp`, `Icons.Outlined.CalendarMonth`) + period label text (e.g., "Mar 2026", `14.sp`). Shadow (1.dp elevation). Dark mode: fill `#2A2928`, border `#3A3938`, text/icon muted. Takes `label: String`, `onClick: () -> Unit`. Add `testTag("statistics:calendarPill")`

- [ ] T025 [US4] Update the `StatisticsScreen` header in `StatisticsScreen.kt`: replace the simple "Statistics" title `TopAppBar` with a custom header Row containing "Statistics" title (`26.sp`, semibold, `-0.5.sp` letterSpacing) on the left and `CalendarFilterPill` on the right. The pill label shows the formatted month from `state.selectedMonth` (or current month if null). Tapping opens `MonthPickerDialog`. On month selection, call `viewModel::setMonth`. Manage `showMonthPicker: Boolean` dialog state locally. Add `testTag("statistics:header")`

- [ ] T026 [US4] Add unit tests for the anchor parameter in `domain/statistics/src/test/java/.../StatisticsPeriodRangeResolverTest.kt`: test that `invoke(WEEK, anchorMillis)` computes range relative to the anchor date, test that `invoke(MONTH, null)` preserves current behavior, test that `invoke(YEAR, anchorMillis)` anchors 12 months from the given timestamp

- [ ] T027 [US4] Add unit tests for selectedMonth in `presentation/statistics/src/test/java/.../StatisticsViewModelTest.kt`: test that `setMonth(YearMonth.of(2026, 1))` updates state and reloads data, test that `setPeriod()` clears selectedMonth, test that `setMonth(null)` reverts to default behavior

**Checkpoint**: Calendar pill visible in header. Month picker opens on tap. Selecting a month reloads statistics for that month. Switching period tabs clears the custom month.

---

## Phase 7: User Story 5 — Dark Mode Design Token Alignment (Priority: P3)

**Goal**: Ensure all Statistics screen elements use the correct design tokens for both light and dark modes. No hardcoded color values.

**Independent Test**: Switch to dark mode and verify each element's color matches the design token spec.

### Implementation for User Story 5

- [ ] T028 [US5] Audit all composables in `StatisticsScreen.kt` for hardcoded `Color(0x...)` values in production code (not @Preview). Replace any found with `MoneyManagerTheme.colors` tokens. Check: card backgrounds, text colors, divider colors, shadow colors, border colors, icon tints. Specifically verify: chart bar colors use `colors.expense`/`colors.income`, "See all" text uses `colors.greenAccent`, muted text uses `colors.textSecondary`

- [ ] T029 [US5] Audit `MonthPickerDialog.kt` for hardcoded colors — ensure all dialog elements (background, text, selected state, grid cells) use `MoneyManagerTheme.colors` tokens

- [ ] T030 [US5] Verify `MoneyManagerSegmentedButton.kt` and `CalendarFilterPill` use the correct dark mode tokens: container `surfaceBorder` → divider token, active bg → surface token, shadow color appropriate per light/dark

**Checkpoint**: All Statistics screen elements render with correct design tokens in both light and dark modes.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, cleanup, and cross-story integration checks

- [ ] T031 Verify all existing testTags are preserved by searching `StatisticsScreen.kt` for each tag listed in the spec: `statistics:screen`, `statistics:error`, `statistics:retryButton`, `statistics:emptyState`, `statistics:content`, `statistics:periodSelector`, `statistics:period_WEEK`, `statistics:period_MONTH`, `statistics:period_YEAR`, `statistics:typeSelector` (now on toggle pills), `statistics:totalExpenses`, `statistics:totalIncome`, `statistics:pieChart`, `statistics:pieChartUnavailable`, `statistics:chartTitle`, `statistics:chartDateRange`, `statistics:barChartUnavailable`, `statistics:monthChartContainer`, `statistics:barChart`, `statistics:category_{id}`

- [ ] T032 Remove dead code: delete the old `StatisticsTypeCards` composable and old `PeriodSelector` (chip-based) composable from `StatisticsScreen.kt` if not already removed during refactoring. Remove unused imports

- [ ] T033 Run `./gradlew assembleDebug test lint detekt` and fix any issues

- [ ] T034 Verify the screen renders correctly on 360dp width (smallest target) — check for layout overflow in: chart card header (title + toggle pills), donut + legend row, calendar pill label truncation

- [ ] T035 Verify all 6 edge cases defined in the spec: (1) zero categories — donut card and category section do not render; (2) single category — donut shows one full-circle segment, legend shows only that category with no "Other"; (3) no data for selected type — chart area shows "unavailable" placeholder inside the new card structure; (4) custom date range survives config change — ViewModel `selectedMonth` is preserved through rotation; (5) long category names — legend rows truncate with ellipsis and do not overflow the card width; (6) non-scrollable chart — scroll indicator does not appear when chart content fits the visible area

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — verify build
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — chart card restructure
- **US2 (Phase 4)**: Depends on Phase 2 — can run in parallel with US1
- **US3 (Phase 5)**: Depends on Phase 2 — can run in parallel with US1 and US2
- **US4 (Phase 6)**: Depends on Phase 2 — domain changes are independent, UI integrates after US1 header exists
- **US5 (Phase 7)**: Depends on Phase 2 + all other stories complete (audit what was built)
- **Polish (Phase 8)**: Depends on all stories complete

### User Story Dependencies

- **US1 (P1)**: After Phase 2. No dependencies on other stories.
- **US2 (P1)**: After Phase 2. No dependencies on other stories. Can parallel with US1.
- **US3 (P2)**: After Phase 2. No dependencies on other stories. Can parallel with US1/US2.
- **US4 (P2)**: After Phase 2. Domain tasks (T019–T021) can parallel with anything. UI tasks (T024–T025) should follow US1's header change (T010).
- **US5 (P3)**: After US1–US4 complete (audits the final code).

### Within Each User Story

- Composable building blocks before composition (e.g., T005–T008 before T009)
- Screen integration (T010, T012, T018, T025) after composables are ready
- Domain changes (T019–T020) independent of UI

### Parallel Opportunities

- **Phase 2**: T002 (tokens) and T003 (segmented button) can run sequentially but are both in core:ui — recommend sequential to avoid merge conflicts in the same module
- **US1**: T005, T006, T007, T008 (individual composables) can be built in parallel, then composed in T009
- **US3**: T014 and T015 can run in parallel (separate composables)
- **US4**: T019, T020, T021 can all run in parallel (different files in different modules)
- **Cross-story**: US1, US2, US3 can all run in parallel after Phase 2 (different sections of StatisticsScreen.kt — but since they modify the same file, recommend sequential to avoid conflicts)

---

## Parallel Example: User Story 1

```bash
# Launch all building-block composables together:
Task: "T005 - Create ExpenseIncomeToggle composable"
Task: "T006 - Create ChartCardHeader composable"
Task: "T007 - Create ChartScrollIndicator composable"
Task: "T008 - Create ChartTotalRow composable"

# Then compose them:
Task: "T009 - Create UnifiedChartCard"
Task: "T010 - Update StatisticsScreen LazyColumn"
Task: "T011 - Extract scrollState for indicator"
```

## Parallel Example: User Story 4

```bash
# Launch all independent changes together:
Task: "T019 - Add anchorMillis to StatisticsPeriodRangeResolver"
Task: "T020 - Add anchorMillis to GetPeriodSummaryUseCase"
Task: "T021 - Add selectedMonth to StatisticsState"

# Then sequential integration:
Task: "T022 - Add setMonth() to ViewModel"
Task: "T023 - Create MonthPickerDialog"
Task: "T024 - Create CalendarFilterPill"
Task: "T025 - Update header with pill + picker"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundational (T002–T004) — color tokens + segmented button
3. Complete Phase 3: US1 — Unified Chart Card (T005–T011)
4. Complete Phase 4: US2 — Segmented Period Selector (T012–T013)
5. **STOP and VALIDATE**: The core layout restructuring is done. Screen is usable with new chart card and period selector.

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 (Chart Card) → Test independently → The biggest visual change is done
3. Add US2 (Period Selector) → Test independently → Core P1 stories complete
4. Add US3 (Donut + Legend) → Test independently → Category section redesigned
5. Add US4 (Calendar Filter) → Test independently → New functionality added
6. Add US5 (Dark Mode Audit) → Test independently → Visual polish complete
7. Polish phase → Final cleanup and verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All composables in `StatisticsScreen.kt` are `private` — they're internal to the screen
- The biggest risk is the chart card restructure (US1, T009–T011) — it touches the most existing code
- Color token changes (T002) affect ALL screens globally — visual regression check recommended
- The domain change (T019–T020) is backward-compatible — existing tests should still pass
- `MonthPickerDialog.kt` is the only new file; everything else is refactoring existing files
