# Data Model: Vico Bar Chart Integration

**Feature**: 009-statistics-chart-vico
**Date**: 2026-03-16

## Overview

This feature makes **no changes** to the domain or data layers. All entities below are presentation-layer state objects. The primary change is replacing `StatisticsChartState.yAxisLabels` with `CartesianChartModelProducer` and passing metadata via Vico's `ExtraStore`.

## Entities

### StatisticsChartPoint (existing — simplified)

Represents one bar in the chart. Used by ViewModel to build Vico model.

| Field | Type | Description |
|-------|------|-------------|
| bucketStartMillis | Long | Epoch ms for the day/month bucket start |
| displayLabel | String | X-axis label (e.g., "Mon", "15", "Jan") |
| amount | Double? | Currency-resolved amount; null = mixed-currency unavailable |
| isToday | Boolean | Whether this bar represents the current day/month |

**Changes**: No field changes. This entity continues to be produced by `buildChartPoints()`.

### StatisticsChartState (existing — modified)

Contract passed from ViewModel to UI for the bar chart section.

| Field | Type | Change |
|-------|------|--------|
| title | String | Unchanged |
| dateRangeLabel | String | Unchanged |
| points | ImmutableList\<StatisticsChartPoint\> | Unchanged — still used for unavailable check |
| yAxisLabels | ImmutableList\<String\> | **REMOVED** — Vico handles axis label generation internally |
| isScrollable | Boolean | Unchanged — drives scroll state creation |

### ExtraStore Keys (new)

Vico's `ExtraStore` carries metadata from `runTransaction` to axis formatters, marker, and decorations.

| Key | Type | Description |
|-----|------|-------------|
| xToLabelMapKey | Map\<Double, String\> | Maps x-index to display label for HorizontalAxis formatter |
| xToDateStringKey | Map\<Double, String\> | Maps x-index to formatted date string for marker tooltip |
| todayIndexKey | Int | The x-index of "today" bar; -1 if today is outside period |
| currencySymbolKey | String | User's base currency symbol (₸, $, €, ₽) for Y-axis formatter |
| currencyPrefixKey | Boolean | Whether currency symbol is a prefix ($50) or suffix (50 ₸) |

### CartesianChartModelProducer (new — Vico library type)

Held as a property in `StatisticsViewModel`. Updated via `runTransaction {}` whenever display data changes.

**Lifecycle**: Created once in ViewModel `init`, updated on every period/type/data change, never recreated.

## State Flow

```
displayedDailyExpenses / displayedMonthlyExpenses (existing flows)
    ↓
ViewModel.updateChartModel() (new method, replaces withChartContract for Vico data)
    ↓
modelProducer.runTransaction {
    columnSeries { series(x = indices, y = amounts) }
    extras { store -> store[xToLabelMapKey] = ...; store[todayIndexKey] = ... }
}
    ↓
CartesianChartHost recomposes automatically
    ↓
VerticalAxis formatter reads currencySymbolKey from ExtraStore
HorizontalAxis formatter reads xToLabelMapKey from ExtraStore
TodayColumnProvider reads todayIndexKey from ExtraStore
TodayDotDecoration reads todayIndexKey from ExtraStore
MarkerValueFormatter reads xToDateStringKey + currencySymbolKey from ExtraStore
```

## No Database Changes

- No Room entity changes
- No DAO changes
- No migration needed
- No Firestore schema changes
