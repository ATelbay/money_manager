# Data Model: Statistics Screen UI Redesign

**Date**: 2026-03-17 | **Branch**: `010-statistics-ui-redesign`

## Existing Entities (Unchanged)

### StatisticsChartState
Chart metadata produced by ViewModel. No changes.
- `title: String` — "Expenses" or "Income"
- `dateRangeLabel: String` — e.g., "Mar 1 – Mar 17, 2026"
- `points: ImmutableList<StatisticsChartPoint>` — bar chart data
- `isScrollable: Boolean` — true for MONTH period

### StatisticsCategoryDisplayItem
Category data for display. No changes.
- `category: CategorySummary` — domain model (id, name, icon, color, amount)
- `displayAmount: String` — formatted with currency
- `displayPercentage: String` — e.g., "32%"

### StatisticsChartPoint
Individual chart bar data. No changes.
- `bucketStartMillis: Long`
- `displayLabel: String`
- `amount: Double`
- `isToday: Boolean`

## Modified Entities

### StatisticsState (presentation layer)

**Added fields:**
- `selectedMonth: YearMonth? = null` — custom month anchor from calendar picker. `null` means "current" (default behavior). `YearMonth` is `java.time.YearMonth` or a simple `Pair<Int, Int>(year, month)`.

**State transitions:**
- User taps calendar pill → month picker opens
- User selects month → `selectedMonth` set to chosen value, data reloads with anchor
- User switches period tab → `selectedMonth` reset to `null`, data reloads with current month
- App start / screen entry → `selectedMonth = null` (default)

### StatisticsPeriodRangeResolver (domain layer — minimal change)

**Added parameter:**
- `operator fun invoke(period: StatsPeriod, anchorMillis: Long? = null): StatisticsDateRange`
- When `anchorMillis` is null, behavior is identical to current (relative to "now")
- When `anchorMillis` is provided, the range is computed relative to that timestamp instead of "now"

### GetPeriodSummaryUseCase (domain layer — minimal change)

**Added parameter:**
- `operator fun invoke(period: StatsPeriod, anchorMillis: Long? = null): Flow<PeriodSummary>`
- Passes `anchorMillis` through to `StatisticsPeriodRangeResolver`

## New UI State Types

### CategoryLegendItem
Derived from `StatisticsCategoryDisplayItem` for the compact legend.
- `name: String` — category name (truncated if needed)
- `color: Color` — segment color
- `percentage: String` — e.g., "32%"
- `isOther: Boolean` — true for aggregated "Other" entry

No persistence needed — computed on the fly in the composable from the existing category list.

## Design Token Updates (Color.kt)

Not a data model change, but documents the token value updates:

| Token Name | Current Value | New Value | Scope |
|------------|---------------|-----------|-------|
| BackgroundLight | `0xFFF5F5F7` | `0xFFF5F4F1` | Global |
| BackgroundDark | `0xFF0D0D0D` | `0xFF1A1918` | Global |
| SurfaceDark | `0xFF1A1A1A` | `0xFF2A2928` | Global |
| TextPrimaryLight | `0xFF1A1A1A` | `0xFF1A1918` | Global |
| TextSecondaryLight | `0x80000000` | `0xFF9C9B99` | Global |
| TextSecondaryDark | (derive) | `0xFF6D6C6A` | Global |
| ExpenseCoral | `0xFFFF6B6B` | `0xFFD08068` | Global |
| DividerLight | (surfaceBorder) | `0xFFEDECEA` | Global |
| DividerDark | (surfaceBorder) | `0xFF3A3938` | Global |
| GreenAccentLight | — | `0xFF3D8A5A` | New |
| GreenAccentDark | — | `0xFF4DB87A` | New |
