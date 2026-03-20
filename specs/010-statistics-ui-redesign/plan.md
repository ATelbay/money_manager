# Implementation Plan: Statistics Screen UI Redesign

**Branch**: `010-statistics-ui-redesign` | **Date**: 2026-03-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/010-statistics-ui-redesign/spec.md`

## Summary

Redesign the Statistics screen UI to match the Money Manager design system: consolidate bar chart + expense/income toggle + total into a single card, replace chip-based period selector with segmented control, restructure donut chart to horizontal layout with side legend and inline expand, add calendar month picker pill in header, and align all color tokens to the design palette. Minimal domain-layer change required (optional anchor parameter for custom month selection).

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Material 3, Vico 2.4.3, Hilt 2.58
**Storage**: Room 2.8.4 (unchanged), Preferences DataStore 1.1.7
**Testing**: JUnit 4, MockK, Turbine, ComposeTestRule
**Target Platform**: Android (minSdk per project config)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps during chart scroll and toggle animations
**Constraints**: 360dp–412dp device width, offline-first, preserve all existing testTags
**Scale/Scope**: 1 screen refactor (~960 LOC), 2 domain files (optional param addition), 2 core:ui files (token update + segmented button)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | No new modules. Existing module boundaries respected. |
| II. Kotlin-First & Compose | PASS | All UI in Compose. State hoisting maintained. testTags preserved + added. |
| III. Material 3 Design System | PASS | Colors from `MoneyManagerTheme.colors`. No hardcoded hex in composables. |
| IV. Animation & Motion | PASS | Use `MoneyManagerMotion` constants for color/visibility transitions. |
| V. Hilt DI | PASS | No new DI bindings needed. ViewModel already `@HiltViewModel`. |
| VI. Room Database | N/A | No database changes. |
| VII. Testing Architecture | PASS | Existing tests preserved. New ViewModel state (selectedMonth) needs tests. |
| VIII. Firebase Ecosystem | N/A | No Firebase changes. |
| IX. Type-Safe Navigation | PASS | No new routes. Existing navigation unchanged. |
| X. Statement Import Pipeline | N/A | Not touched. |
| XI. Preferences DataStore | N/A | Not touched. |

**Note**: FR-011 says "no domain changes" but research (R2) found that the calendar filter requires adding an optional `anchorMillis` parameter to `StatisticsPeriodRangeResolver` and `GetPeriodSummaryUseCase`. This is a backward-compatible addition (default = null = current behavior). See Complexity Tracking below.

**Post-Phase-1 re-check**: All gates still pass. The domain change is minimal and backward-compatible.

## Project Structure

### Documentation (this feature)

```text
specs/010-statistics-ui-redesign/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: research findings
├── data-model.md        # Phase 1: entity changes
├── quickstart.md        # Phase 1: build & verify guide
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
core/ui/src/main/java/com/atelbay/money_manager/core/ui/
├── theme/
│   └── Color.kt                          # Update design token values
└── components/
    └── MoneyManagerSegmentedButton.kt     # Refactor styling for new design

domain/statistics/src/main/java/com/atelbay/money_manager/domain/statistics/usecase/
├── StatisticsPeriodRangeResolver.kt       # Add optional anchorMillis param
└── GetPeriodSummaryUseCase.kt             # Add optional anchorMillis param

presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/
├── ui/
│   ├── StatisticsScreen.kt                # Major refactor (~960 LOC)
│   └── MonthPickerDialog.kt               # New: custom month picker composable
├── state/
│   └── StatisticsState.kt                 # Add selectedMonth field
└── viewmodel/                             # (or wherever ViewModel lives)
    └── StatisticsViewModel.kt             # Add setMonth(), update loadSummary()

presentation/statistics/src/test/
└── ...StatisticsViewModelTest.kt          # Add selectedMonth tests

domain/statistics/src/test/
└── ...StatisticsPeriodRangeResolverTest.kt # Add anchorMillis tests
```

**Structure Decision**: Existing multi-module structure. No new modules created. Changes span `core:ui` (tokens + component), `domain:statistics` (optional param), and `presentation:statistics` (UI refactor + new composable).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Domain change (FR-011 says "no domain changes") | Calendar filter (User Story 4) needs `StatisticsPeriodRangeResolver` to accept a custom month anchor | Implementing range logic in ViewModel duplicates the resolver's responsibility and creates two sources of truth for date range computation. The change is backward-compatible (optional param with null default). |
