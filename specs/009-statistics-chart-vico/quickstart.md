# Quickstart: Vico Bar Chart Integration

**Feature**: 009-statistics-chart-vico
**Date**: 2026-03-16

## Prerequisites

- Android Studio with project synced
- Existing `presentation:statistics` module building successfully

## Key Files

| File | Action |
|------|--------|
| `presentation/statistics/build.gradle.kts` | Add `implementation(libs.vico.compose.m3)` |
| `presentation/statistics/.../ui/StatisticsScreen.kt` | Replace `StatisticsBarChartSection`, `ChartBars`, `YAxisLabels` with Vico composables |
| `presentation/statistics/.../ui/StatisticsViewModel.kt` | Add `CartesianChartModelProducer`, replace `buildYAxisLabels()`, add `updateChartModel()` |
| `presentation/statistics/.../ui/StatisticsState.kt` | Remove `yAxisLabels` from `StatisticsChartState` |
| `gradle/libs.versions.toml` | Already has `vico = "2.4.3"` and `vico-compose-m3` alias — no changes needed |

## Build & Verify

```bash
# After changes:
./gradlew :presentation:statistics:assembleDebug

# Run unit tests:
./gradlew :presentation:statistics:test

# Full project build:
./gradlew assembleDebug

# Lint check:
./gradlew :presentation:statistics:lint
```

## Test Checklist

1. Open Statistics screen → verify Y-axis shows currency-formatted labels
2. Select "Month" → verify horizontal scroll, starts at right edge
3. Select "Week" → verify 7 bars fit without scroll
4. Select "Year" → verify 12 bars fit without scroll
5. Tap any bar → verify tooltip shows amount + date
6. Toggle Expenses ↔ Income → verify chart updates, scroll preserved
7. Switch to dark theme → verify colors adapt
8. Test with mixed-currency accounts → verify "unavailable" overlay
