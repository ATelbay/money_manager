# Tasks: Replace Bar Chart with Vico Library

**Input**: Design documents from `/specs/009-statistics-chart-vico/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: ViewModel unit tests and basic UI render test per Constitution VII.

**Organization**: Tasks grouped by user story. US1 and US2 (both P1) share the foundational chart creation but have distinct deliverables.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: Add Vico dependency to the presentation:statistics module

- [ ] T001 Add `implementation(libs.vico.compose.m3)` to `presentation/statistics/build.gradle.kts` and sync Gradle. The catalog alias `vico-compose-m3` (version 2.4.3) already exists in `gradle/libs.versions.toml` — no catalog changes needed.

---

## Phase 2: Foundational (State Model + ViewModel Producer)

**Purpose**: Update state model and wire CartesianChartModelProducer in ViewModel. MUST complete before any user story work.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 [P] Remove `yAxisLabels: ImmutableList<String>` field from `StatisticsChartState` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsState.kt`. Update the default value and all call sites that construct `StatisticsChartState` (in `StatisticsViewModel.kt` — the `withChartContract()` method). Remove the `buildYAxisLabels()` method from `StatisticsViewModel.kt` and all its references.

- [ ] T003 [P] Define ExtraStore keys as top-level `val` declarations in a new section at the top of `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` (or a companion/file-level block). Keys to define per data-model.md: `xToLabelMapKey` (Map<Double, String>), `xToDateStringKey` (Map<Double, String>), `todayIndexKey` (Int), `currencySymbolKey` (String), `currencyPrefixKey` (Boolean). All are `ExtraStore.Key<T>()`.

- [ ] T004 Add `val chartModelProducer = CartesianChartModelProducer()` as a property in `StatisticsViewModel` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsViewModel.kt`. Create a new `private suspend fun updateChartModel()` method that: (1) reads `state.value` to get current `points`, `currencyUiState.moneyDisplay`, and period; (2) calls `chartModelProducer.runTransaction { columnSeries { series(x = indices, y = amounts) }; extras { store -> ... } }` populating all ExtraStore keys from data-model.md; (3) handles null amounts by using 0.0 for Vico (the unavailable overlay at section level already prevents rendering). Wire `updateChartModel()` to be called everywhere `withChartContract()` is currently called (inside `_state.update` blocks after chart data changes). Import ExtraStore keys from StatisticsScreen.kt file.

**Checkpoint**: State model updated, ViewModel produces Vico chart data. Old chart UI still renders (will be replaced in Phase 3).

---

## Phase 3: User Story 1 + 2 — Currency Y-Axis Labels & Scrollable Month (Priority: P1) 🎯 MVP

**Goal**: Replace custom `StatisticsBarChartSection`, `ChartBars`, and `YAxisLabels` composables with a Vico `CartesianChartHost` that has currency-formatted Y-axis, contextual X-axis date labels, dashed grid lines, and period-conditional scroll behavior.

**Independent Test**: Open Statistics screen with transaction data → verify Y-axis shows currency-formatted values (US1), select Month → verify horizontal scroll starting at right edge (US2), select Week/Year → verify all bars fit without scroll (US2).

### Implementation

- [ ] T005 [US1] [US2] Create a new `@Composable private fun VicoBarChartSection(...)` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` that replaces `StatisticsBarChartSection`. This composable should:
  - Accept parameters: `chart: StatisticsChartState`, `modelProducer: CartesianChartModelProducer`, `moneyDisplay: StatisticsMoneyDisplay`, `barColor: Color`, `isUnavailable: Boolean`, `unavailableText: String`, `period: StatsPeriod`, `modifier: Modifier`
  - Render the same outer structure: title caption (`chart.title`), date range caption (`chart.dateRangeLabel`), then a `GlassCard` containing either `StatisticsUnavailableCard` (when `isUnavailable`) or the Vico `CartesianChartHost`
  - Keep the same `testTag` values on the outer container for UI test compatibility
  - Inside the `GlassCard`, set up `CartesianChartHost` with:
    - `rememberCartesianChart(rememberColumnCartesianLayer(...))` with `ColumnProvider.series(rememberLineComponent(fill = Fill(barColor), thickness = 16.dp, shape = CorneredShape.rounded(topLeftPercent = 40, topRightPercent = 40)))` for rounded-top bars
    - `startAxis = VerticalAxis.rememberStart(valueFormatter = ...)` — the `CartesianValueFormatter` lambda reads `currencySymbolKey` and `currencyPrefixKey` from `context.model.extraStore` and formats y-values with K/M abbreviation (FR-002). Use existing `defaultMoneyNumberFormat()` pattern where applicable.
    - `bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = ...)` — reads `xToLabelMapKey` from ExtraStore (FR-003)
    - `guideline` on VerticalAxis: `LineComponent` with `DashedShape(shape = RectangleShape, dashLengthDp = 8f, gapLengthDp = 4f)` using `MaterialTheme.colorScheme.outlineVariant` for dashed horizontal grid lines (FR-004). Pass `guideline = null` on HorizontalAxis to suppress vertical grid lines.
    - Conditional scroll/zoom state based on `period`:
      - `StatsPeriod.MONTH`: `rememberVicoScrollState(scrollEnabled = true, initialScroll = Scroll.Absolute.End)` (FR-005)
      - `StatsPeriod.WEEK`, `StatsPeriod.YEAR`: `rememberVicoScrollState(scrollEnabled = false)` (FR-006)
      - All periods: `rememberVicoZoomState(zoomEnabled = false)`
    - Chart height: `Modifier.height(220.dp)` to match existing card height
    - Colors from `MaterialTheme.colorScheme` for axis text, grid lines (FR-011)
  - Reference design screenshots at `specs/009-statistics-chart-vico/design/statistics-light.png` and `statistics-dark.png` for visual matching

- [ ] T006 [US1] [US2] Update the LazyColumn `"bar"` item in `StatisticsScreen` composable (inside `StatisticsContent` or the main screen composable) in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` to call `VicoBarChartSection` instead of `StatisticsBarChartSection`. Pass `viewModel.chartModelProducer` (or `state.chartModelProducer` if exposed via state) and the current `period` as additional parameters. Ensure `moneyDisplay`, `barColor`, `isUnavailable`, and `unavailableText` continue to be passed correctly. Verify the total row below the chart (FR-013) and the Expenses/Income toggle (FR-012) remain unchanged.

- [ ] T007 [US1] [US2] Delete the old composables that are no longer needed from `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`: remove `StatisticsBarChartSection`, `ChartBars`, and `YAxisLabels` composable functions entirely. Verify no remaining references. Keep all other composables (`DonutChartCard`, `DonutChart`, `CategoryBreakdownCard`, `PeriodSelector`, `StatisticsTypeCards`, etc.) untouched (FR-015).

**Checkpoint**: MVP complete. Y-axis shows currency labels, Month view scrolls, Week/Year fit-to-width. Old custom chart code removed.

---

## Phase 4: User Story 3 — Tap Tooltip (Priority: P2)

**Goal**: Add a floating marker/tooltip that shows exact formatted amount and date when a bar is tapped.

**Independent Test**: Tap any bar → tooltip appears with amount and date. Tap elsewhere → tooltip dismisses. Test in both light and dark themes for inverted tooltip colors.

### Implementation

- [ ] T008 [US3] Add a `CartesianMarker` to the Vico chart in `VicoBarChartSection` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`. Use `rememberDefaultCartesianMarker(valueFormatter = ...)` with a custom `DefaultCartesianMarker.ValueFormatter` that: (1) reads `xToDateStringKey` and `currencySymbolKey` from ExtraStore to format the tooltip text as "amount\ndate" (e.g., "153,200 ₸\nMar 23"); (2) uses theme-aware colors — in light theme: dark background (`MaterialTheme.colorScheme.inverseSurface`) with white text (`MaterialTheme.colorScheme.inverseOnSurface`); in dark theme: light background with dark text (FR-008). Pass `label = rememberTextComponent(...)` with appropriate text style and colors. Attach the marker to `rememberCartesianChart(..., marker = marker)` (FR-007).

**Checkpoint**: Tap interaction works. Tooltip shows exact amount + date with correct theme colors.

---

## Phase 5: User Story 4 — "Today" Bar Highlight (Priority: P3)

**Goal**: Today's bar renders at full opacity with an accent dot above it; other bars are dimmed to ~0.7 opacity.

**Independent Test**: View chart for current period containing today → one bar is visibly brighter with a colored dot above it. View a past period → all bars at uniform opacity, no dot.

### Implementation

- [ ] T009 [P] [US4] Create a custom `TodayColumnProvider` class (private to the file) in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` implementing `ColumnCartesianLayer.ColumnProvider`. The `getColumn(entry, seriesIndex, extraStore)` method reads `todayIndexKey` from `extraStore`: if `entry.x.toInt() == todayIndex`, return the full-opacity `LineComponent`; otherwise return the 0.7-opacity `LineComponent`. Both components should use the same `barColor` with `CorneredShape.rounded(topLeftPercent = 40, topRightPercent = 40)`. The `getWidestSeriesColumn()` returns the full-opacity component. Per research Decision 2.

- [ ] T010 [P] [US4] Create a custom `TodayDotDecoration` class (private to the file) in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` implementing Vico's `Decoration` interface. In `onDrawAboveChart(context, bounds)`: read `todayIndexKey` from `context.extraStore`; if index >= 0, compute the pixel x-position of that bar using Vico's coordinate mapping, then draw a small filled circle (radius ~4dp) in the accent color (`MaterialTheme.colorScheme.primary` — captured at composition time and passed to the decoration) above the bar's top. Per research Decision 3.

- [ ] T011 [US4] Wire `TodayColumnProvider` and `TodayDotDecoration` into `VicoBarChartSection` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`. Replace the `ColumnProvider.series(...)` with `TodayColumnProvider(...)` in the `rememberColumnCartesianLayer` call. Add `TodayDotDecoration` to `rememberCartesianChart(..., decorations = listOf(todayDotDecoration))`. Ensure the accent dot color and bar colors are read from `MaterialTheme.colorScheme` so they adapt to light/dark theme (FR-011). When `todayIndexKey` is -1 (today outside period), all bars render at 0.7 opacity and no dot appears (FR-009, FR-010).

**Checkpoint**: Today's bar is visually distinct. All acceptance scenarios for US4 met.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Edge case handling, cleanup, and final verification

- [ ] T012 [P] Handle edge cases in `updateChartModel()` in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsViewModel.kt`: (1) all amounts are zero — ensure `runTransaction` still produces a valid model with zero-height bars; (2) single data point — ensure a single bar renders without crash; (3) extremely large amounts — verify K/M/B abbreviation in the Y-axis formatter handles values up to 999,999,999+; (4) screen rotation / configuration change — verify `CartesianChartModelProducer` survives ViewModel retention (it's a plain property, not `SavedStateHandle`), and Month scroll position is restored via `rememberVicoScrollState` surviving recomposition (Vico's `rememberSaveable` integration). Manually test on device by rotating in Month view mid-scroll.

- [ ] T013 [P] Verify that the Expenses/Income toggle in `StatisticsScreen` triggers `updateChartModel()` and that `CartesianChartModelProducer` animates the data transition smoothly in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`. Confirm scroll position is preserved in Month view when toggling (SC-006). If Vico resets scroll on model update, investigate using `scrollState.value` preservation. Additionally, stress-test rapid toggling (tap Expenses→Income→Expenses quickly 5+ times) — verify no flickering, no crash from concurrent `runTransaction` calls, and the final chart state matches the last selected type. If `runTransaction` is not safe under rapid calls, add a debounce or cancel-previous pattern in the ViewModel collector.

- [ ] T014 Build and lint check: run `./gradlew :presentation:statistics:assembleDebug :presentation:statistics:test :presentation:statistics:lint` to verify no compilation errors, no test regressions, and no lint warnings from the new Vico integration.

- [ ] T015 Run quickstart.md manual verification checklist from `specs/009-statistics-chart-vico/quickstart.md`: test all 8 items (Y-axis labels, Month scroll, Week fit, Year fit, tooltip, toggle, dark theme, unavailable state).

---

## Phase 7: Tests (Constitution VII)

**Purpose**: Mandatory ViewModel unit tests and basic UI compose test per Constitution VII.

- [ ] T016 [P] Add unit tests for `updateChartModel()` in `presentation/statistics/src/test/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsViewModelChartTest.kt`. Tests MUST use `MainDispatcherRule`, MockK, Turbine, and `runTest {}`. Minimum 4 tests: (1) initial state produces empty/valid model; (2) week period with 7 data points populates correct ExtraStore keys (xToLabelMapKey has 7 entries, todayIndexKey matches expected index); (3) month period with 30 points sets todayIndexKey to -1 when viewing a past month; (4) toggling Expenses→Income triggers a new `runTransaction` with updated amounts. Verify `chartModelProducer` is updated by asserting on ExtraStore contents after `runTransaction` completes.

- [ ] T017 [P] Add a basic Compose UI render test for the Vico chart in `presentation/statistics/src/androidTest/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsChartRenderTest.kt`. Use `ComposeTestRule` to render `VicoBarChartSection` with a pre-built `CartesianChartModelProducer` containing 7 sample data points. Assert: (1) the chart host node with `testTag` is displayed; (2) the unavailable overlay is NOT shown when data is available; (3) the unavailable overlay IS shown when `isUnavailable = true`.

**Checkpoint**: Constitution VII compliance met. ViewModel logic and basic UI rendering verified.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (Vico dependency must be available)
- **US1+US2 (Phase 3)**: Depends on Phase 2 (state model + ModelProducer must exist)
- **US3 (Phase 4)**: Depends on Phase 3 (chart must exist to add marker to)
- **US4 (Phase 5)**: Depends on Phase 3 (chart must exist to customize ColumnProvider)
- **Polish (Phase 6)**: Depends on Phases 3–5
- **Tests (Phase 7)**: Depends on Phase 3 (chart must exist to test). Can run in parallel with Phases 4–6.

### User Story Dependencies

- **US1 + US2 (P1)**: Combined — both delivered by the core chart replacement. Can start after Phase 2.
- **US3 (P2)**: Depends on Phase 3 chart existing. Independent of US4.
- **US4 (P3)**: Depends on Phase 3 chart existing. Independent of US3.
- **US3 and US4 can run in parallel** after Phase 3 completes.

### Parallel Opportunities

Within Phase 2:
```
T002 (state model) ∥ T003 (ExtraStore keys)  →  T004 (ViewModel producer, needs both)
```

Within Phase 3:
```
T005 (create VicoBarChartSection) → T006 (wire into screen) → T007 (delete old code)
```

Phase 4 ∥ Phase 5 (after Phase 3):
```
T008 (marker/tooltip)  ∥  T009 + T010 (TodayColumnProvider + TodayDotDecoration)
                           → T011 (wire today highlight)
```

Within Phase 5:
```
T009 (TodayColumnProvider) ∥ T010 (TodayDotDecoration)  →  T011 (wire both in)
```

Within Phase 6:
```
T012 (edge cases) ∥ T013 (toggle/scroll preservation)  →  T014 (build) → T015 (manual QA)
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundational (T002–T004)
3. Complete Phase 3: US1+US2 chart replacement (T005–T007)
4. **STOP and VALIDATE**: Y-axis labels + scroll work correctly
5. This delivers the two core UX improvements that motivated the feature

### Incremental Delivery

1. Phases 1–3 → MVP with currency labels + scroll ✅
2. Phase 4 → Add tap tooltip (US3) ✅
3. Phase 5 → Add today highlight (US4) ✅
4. Phase 6 → Polish, edge cases, final QA ✅
5. Phase 7 → Tests (can run in parallel with Phases 4–6) ✅

### Parallel Execution (after Phase 3)

- Agent A: Phase 4 (US3 — tooltip) — single task T008
- Agent B: Phase 5 (US4 — today highlight) — T009 ∥ T010, then T011

---

## Notes

- All changes are in `presentation/statistics/` module — 4 files modified (3 `.kt` + `build.gradle.kts`), 2 test files created
- Vico `compose-m3` 2.4.3 is already declared in version catalog — only needs `implementation` line
- `CartesianChartModelProducer` is a plain object (not Hilt-injectable) — created directly in ViewModel
- ExtraStore keys are file-level `val` declarations — shared between ViewModel and composables
- Design reference: `specs/009-statistics-chart-vico/design/statistics-light.png` and `statistics-dark.png`
