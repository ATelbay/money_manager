# Research: Vico 2.x Bar Chart Integration

**Feature**: 009-statistics-chart-vico
**Date**: 2026-03-16

## Decision 1: Vico 2.4.3 API Compatibility

**Decision**: Vico 2.4.3 (`compose-m3` artifact) supports all required features.

**Rationale**: Research confirms the following APIs exist and satisfy spec requirements:
- `CartesianChartHost` — top-level composable accepting chart, modelProducer, scrollState, zoomState
- `ColumnCartesianLayer` with `ColumnProvider` — bar rendering with per-entry styling
- `CartesianChartModelProducer.runTransaction {}` — suspend function for reactive data updates
- `VerticalAxis.rememberStart(valueFormatter = ...)` — custom currency formatting
- `HorizontalAxis.rememberBottom(valueFormatter = ...)` — date label formatting via ExtraStore
- `rememberVicoScrollState(initialScroll = Scroll.Absolute.End)` — scroll to end on load
- `rememberDefaultCartesianMarker()` — tap tooltip with custom ValueFormatter
- `DashedShape` on guideline `LineComponent` — dashed horizontal grid lines
- `Decoration` interface — custom dot indicator above "today" bar

**Alternatives considered**: MPAndroidChart (XML-based, no Compose-native support), custom Canvas (current approach — lacks scroll/tooltip).

## Decision 2: Per-Entry Bar Opacity (Today Highlight)

**Decision**: Implement custom `ColumnCartesianLayer.ColumnProvider` to return different `LineComponent` instances per entry.

**Rationale**: `ColumnProvider.series()` applies one style per series, not per entry. The `getColumn(entry, seriesIndex, extraStore)` method receives the entry object, allowing branching on `entry.x` to match the "today" index. Two pre-built `LineComponent` instances (full opacity + reduced opacity) are returned based on the match.

**Alternatives considered**: Post-draw overlay (fragile, z-order issues), separate series for today (over-engineering for one bar).

## Decision 3: Dot Indicator Above "Today" Bar

**Decision**: Implement custom `Decoration` that draws a small circle on `context.canvas` at the "today" bar's x-position.

**Rationale**: Vico's `Decoration` interface provides `onDrawAboveChart(context, bounds)` which has access to the canvas and chart coordinate mapping. A custom implementation can compute the pixel position of the "today" x-value and draw a filled circle above it. The "today" x-index is passed via `ExtraStore`.

**Alternatives considered**: Compose `Box` overlay outside the chart (misalignment on scroll), marker always visible (conflicts with tap marker behavior).

## Decision 4: ModelProducer in ViewModel

**Decision**: Hold `CartesianChartModelProducer` as a property of `StatisticsViewModel`. Update via `runTransaction {}` inside a `viewModelScope.launch` collector.

**Rationale**: `runTransaction` is a suspend function designed for coroutine-based updates. Collecting the existing display data flows and calling `runTransaction` inside the collector mirrors the current `withChartContract()` pattern but delegates rendering math to Vico. The `ExtraStore` mechanism carries metadata (date map, today index, currency symbol) to formatters and decorations.

**Alternatives considered**: Producing model in composable `LaunchedEffect` (breaks ViewModel ownership of state), keeping `StatisticsChartState.points` and converting in UI (redundant intermediate).

## Decision 5: Y-Axis Currency Formatting

**Decision**: Use `CartesianValueFormatter` lambda that reads the currency symbol from `ExtraStore` and applies K/M abbreviation.

**Rationale**: The formatter receives the y-value as `Double` and formats it based on magnitude. The currency symbol is passed via `ExtraStore` during `runTransaction`, so the formatter is not hardcoded to any currency. This replaces the current `buildYAxisLabels()` method entirely.

**Alternatives considered**: Pre-formatting labels in ViewModel like today (rigid, Vico auto-scales ticks making pre-formatted labels misaligned).

## Decision 6: X-Axis Date Labels via ExtraStore

**Decision**: Pass an `x → displayLabel` map via `ExtraStore` during `runTransaction`. The `HorizontalAxis` formatter reads from this map.

**Rationale**: The ViewModel already computes `displayLabel` per chart point (day-of-week, day number, or month name). Passing it through `ExtraStore` avoids duplicating date formatting logic in the UI layer.

**Alternatives considered**: Using x-values as epoch days and formatting in the axis (requires date math in UI, violates ViewModel-owns-formatting pattern).

## Decision 7: Scroll Behavior per Period

**Decision**: Conditionally create `rememberVicoScrollState` and `rememberVicoZoomState` based on the current period.

**Rationale**:
- Month: `scrollEnabled = true`, `initialScroll = Scroll.Absolute.End`, `zoomEnabled = false`
- Week/Year: `scrollEnabled = false`, `zoomEnabled = false` — Vico auto-fits all bars when scroll is disabled

The period is part of `StatisticsState`, which already drives recomposition.

**Alternatives considered**: Always scrollable with zoom-to-fit (confusing UX for 7 bars in Week view).
