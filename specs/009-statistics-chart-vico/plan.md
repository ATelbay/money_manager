# Implementation Plan: Replace Bar Chart with Vico Library

**Branch**: `009-statistics-chart-vico` | **Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-statistics-chart-vico/spec.md`

## Summary

Replace the custom-drawn bar chart (`ChartBars`, `YAxisLabels`, `StatisticsBarChartSection`) in the Statistics screen with Vico 2.4.3 `CartesianChartHost`. This adds currency-formatted Y-axis labels, native horizontal scroll with fling for Month view, tap tooltips, and a "today" bar visual highlight. No domain/data layer changes — purely presentation layer.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Vico 2.4.3 (`compose-m3`), Hilt 2.58
**Storage**: N/A (no data changes)
**Testing**: JUnit 4, MockK 1.14.9, Turbine 1.2.1, ComposeTestRule
**Target Platform**: Android (minSdk as project default)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps chart rendering, smooth scroll/fling
**Constraints**: Presentation layer only — no domain/data module changes
**Scale/Scope**: 1 module affected (`presentation:statistics`), ~4 files modified

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | No new modules needed. Change is within `presentation:statistics` only. Dependency boundaries preserved — Vico is a UI library added to presentation layer. |
| II. Kotlin-First & Jetpack Compose | PASS | Vico `compose-m3` is a Compose-native library. All code in Kotlin. State hoisting maintained — ViewModel owns `CartesianChartModelProducer`. |
| III. Material 3 Design System | PASS | Vico `compose-m3` integrates with MaterialTheme. Colors derived from `MaterialTheme.colorScheme`. |
| IV. Animation & Motion | PASS | Vico handles bar entry animation internally. No custom animation specs needed. |
| V. Hilt DI | PASS | No new injectable components. `CartesianChartModelProducer` is created directly in ViewModel (plain object, not a service). |
| VI. Room Database | N/A | No database changes. |
| VII. Testing Architecture | PASS | ViewModel unit tests will verify `runTransaction` calls. UI tests will use existing `testTag` pattern on the chart host. |
| VIII. Firebase Ecosystem | N/A | No Firebase changes. |
| IX. Type-Safe Navigation | N/A | No navigation changes. |
| X. Statement Import Pipeline | N/A | No import changes. |
| XI. Preferences DataStore | N/A | No DataStore changes. |

**Post-design re-check**: All gates still PASS. The `Decoration` and custom `ColumnProvider` implementations are internal to the composable — no architectural boundary violations.

## Project Structure

### Documentation (this feature)

```text
specs/009-statistics-chart-vico/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Vico API research & decisions
├── data-model.md        # State model changes
├── quickstart.md        # Build & verify guide
├── design/              # Design reference screenshots
│   ├── statistics-light.png
│   └── statistics-dark.png
└── tasks.md             # (Phase 2 — /speckit.tasks)
```

### Source Code (affected files)

```text
presentation/statistics/
├── build.gradle.kts                              # Add vico-compose-m3 dependency
└── src/main/java/.../presentation/statistics/ui/
    ├── StatisticsScreen.kt                       # Replace StatisticsBarChartSection, ChartBars, YAxisLabels
    │                                             # Add: VicoBarChartSection, TodayColumnProvider, TodayDotDecoration
    ├── StatisticsViewModel.kt                    # Add CartesianChartModelProducer, updateChartModel()
    │                                             # Remove: buildYAxisLabels()
    └── StatisticsState.kt                        # Remove yAxisLabels from StatisticsChartState
```

**Structure Decision**: No new files or modules. All changes are modifications to existing files within `presentation:statistics`. New composables and helper classes are added as private declarations within `StatisticsScreen.kt`.

## Complexity Tracking

No constitution violations — table not needed.
