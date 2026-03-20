# Research: Statistics Screen UI Redesign

**Date**: 2026-03-17 | **Branch**: `010-statistics-ui-redesign`

## R1: Color Token Gap — Existing vs Design System

**Decision**: Update color tokens in `core:ui/theme/Color.kt` to match the new design system palette.

**Rationale**: The existing color values diverge significantly from the design spec:

| Token | Current | Design Target |
|-------|---------|---------------|
| BackgroundLight | `0xFFF5F5F7` | `0xFFF5F4F1` |
| BackgroundDark | `0xFF0D0D0D` | `0xFF1A1918` |
| SurfaceDark | `0xFF1A1A1A` | `0xFF2A2928` |
| TextPrimaryLight | `0xFF1A1A1A` | `0xFF1A1918` |
| Muted text | `0x80000000` (50% black) | `0xFF9C9B99` |
| Expense color | `0xFFFF6B6B` | `0xFFD08068` |
| Income color | `0xFF4ADE80` (Mint) | no change specified |

**Impact**: Changing these tokens affects ALL screens, not just Statistics. This must be coordinated carefully — either:
- (a) Update globally and visually verify all screens, OR
- (b) Create Statistics-local token overrides (violates DRY, not recommended)

**Alternatives considered**:
- Local hardcoded colors in StatisticsScreen → rejected (violates Constitution III — all colors from MaterialTheme/design tokens)
- Dual token sets → rejected (complexity, maintenance burden)

**Recommendation**: Update globally. Since this is a design system alignment, all screens should benefit. Visual regression check needed.

## R2: Calendar Filter Requires Domain Change

**Decision**: The calendar filter (User Story 4) requires adding an `anchorMonth` parameter to `StatisticsPeriodRangeResolver` in the domain layer.

**Rationale**: The current `StatisticsPeriodRangeResolver.invoke(period: StatsPeriod)` always computes ranges relative to "now". There is no way to anchor to a custom month. Similarly, `GetPeriodSummaryUseCase.invoke(period: StatsPeriod)` accepts only the enum — no date range parameter.

This conflicts with FR-011 ("No changes to domain logic"). However, the change is minimal and scoped:
- Add an optional `anchorMillis: Long? = null` parameter to `StatisticsPeriodRangeResolver`
- Add an optional `anchorMillis: Long? = null` parameter to `GetPeriodSummaryUseCase`
- Default behavior (null) is identical to current behavior — backward compatible

**Alternatives considered**:
- Implement custom range logic entirely in ViewModel → rejected (duplicates range resolution, violates single-responsibility)
- Defer calendar filter to separate feature → viable but reduces this feature's value
- Create a new overload `invoke(period, anchor)` instead of modifying existing signature → cleaner but same domain change

**Recommendation**: Accept the minimal domain change (adding optional parameter). Document in plan as a constitution-check note.

## R3: Existing MoneyManagerSegmentedButton Reuse

**Decision**: Refactor the existing `MoneyManagerSegmentedButton` to support the new design tokens (white/surface active bg with shadow instead of Teal fill).

**Rationale**: The composable already has the right structure (Row + clips + animated colors) and is used elsewhere (Settings theme selector). Changes needed:
- Active bg: `Teal` → `surface` (white in light, `#3A3938` in dark) with subtle shadow
- Active text: `White` → `textPrimary` (semibold)
- Inactive text: keep `textSecondary` but update to muted color token
- Add `weight(1f)` support for equal-width tabs
- Add optional height parameter (40dp for period selector)

**Alternatives considered**:
- Create a new `StatisticsSegmentedControl` composable → rejected (code duplication)
- Use Material 3 `SegmentedButton` from compose-material3 → rejected (limited styling control, different visual language from design spec)

## R4: Vico Scroll State for Scroll Indicator

**Decision**: Use `VicoScrollState` from `rememberVicoScrollState()` to drive the custom scroll indicator.

**Rationale**: The scroll state is already created in `VicoBarChartSection` and passed to `CartesianChartHost`. Vico 2.4.3's `VicoScrollState` exposes `value` (current position) and `maxValue` (maximum scroll) as observable state. The scroll indicator can derive its thumb position as `value / maxValue`.

**Alternatives considered**:
- Wrap CartesianChartHost in a custom scrollable container → rejected (Vico manages its own scrolling)
- Use LaunchedEffect to poll scroll position → unnecessary (state is already observable)

## R5: Inline Category Expansion ("See All")

**Decision**: Implement expand/collapse as a `Boolean` state in the composable, toggling between showing top-3+Other and all categories.

**Rationale**: The existing `CategoryBreakdownCard` already renders all categories in a `Column`. The compact donut legend shows top 3 + "Other". The "See all" button toggles a local `expanded` state that switches between the two views within the same card.

**Alternatives considered**:
- Animate height change with `AnimatedVisibility` → recommended for polish
- Use `LazyColumn` for expanded list → unnecessary for typical category count (<20)

## R6: Month Picker Implementation

**Decision**: Use Material 3 `DatePickerDialog` in month-selection mode, or a custom `YearMonth` picker composable.

**Rationale**: Material 3's `DatePicker` can be configured for date selection but doesn't have a native month-only mode. Options:
- (a) Custom composable: scrollable year column + 12-month grid — simple, matches design language
- (b) M3 `DatePickerDialog` with `DatePickerDefaults.YearMonthSelectorColors` — if API available in BOM 2026.01.01
- (c) Third-party library — adds dependency, not justified for one picker

**Recommendation**: Custom month picker composable — full control over design tokens and simple to implement (year selector + 3×4 month grid).
