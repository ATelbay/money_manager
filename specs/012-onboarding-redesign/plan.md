# Implementation Plan: Onboarding Screens Redesign

**Branch**: `012-onboarding-redesign` | **Date**: 2026-03-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-onboarding-redesign/spec.md`

## Summary

Redesign the 3-page onboarding flow to match Pencil design mockups. Changes are purely visual: add Outfit font family to `core:ui`, define onboarding-local color constants, replace generic `MoneyManagerButton`/`MoneyManagerTextButton` with custom-styled composables, and update layout spacing/sizing. No ViewModel, navigation, or data layer changes. All existing `testTag`s and pager logic are preserved.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Material 3
**Storage**: N/A (no data changes)
**Testing**: JUnit 4, ComposeTestRule (existing UI tests must keep working)
**Target Platform**: Android (minSdk per project config)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps onboarding pager scrolling (no change from current)
**Constraints**: No modifications to global theme/typography; Outfit font used only in onboarding
**Scale/Scope**: 2 files modified (`OnboardingScreen.kt`, `Type.kt`), 3 font files added

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | No new modules. Modifying `presentation:onboarding` + `core:ui` only. |
| II. Kotlin-First & Jetpack Compose | PASS | All Compose, no XML. State hoisting preserved. testTags preserved. |
| III. Material 3 Design System | VIOLATION (justified) | Onboarding uses hardcoded local colors instead of `MaterialTheme`. See Complexity Tracking. |
| IV. Animation & Motion | PASS | Existing `animateColorAsState` for dots preserved. No new animations. |
| V. Hilt DI | PASS | No DI changes. |
| VI. Room Database | N/A | No database changes. |
| VII. Testing Architecture | PASS | All `testTag`s preserved (FR-008). |
| VIII. Firebase Ecosystem | N/A | No Firebase changes. |
| IX. Type-Safe Navigation | PASS | No navigation changes. |
| X. Statement Import Pipeline | N/A | Not related. |
| XI. Preferences DataStore | PASS | No DataStore changes. `onboarding_completed` key unchanged. |

**Post-Phase 1 re-check**: Same results. No design decisions changed gate status.

## Project Structure

### Documentation (this feature)

```text
specs/012-onboarding-redesign/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (minimal — no data changes)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
core/ui/src/main/
├── java/com/atelbay/money_manager/core/ui/theme/
│   └── Type.kt                          # Add OutfitFontFamily definition
└── res/font/
    ├── outfit_regular.ttf               # NEW — Outfit W400
    ├── outfit_medium.ttf                # NEW — Outfit W500
    └── outfit_semibold.ttf              # NEW — Outfit W600

presentation/onboarding/src/main/java/com/atelbay/money_manager/presentation/onboarding/ui/
└── OnboardingScreen.kt                  # Redesigned composables + local colors
```

**Structure Decision**: No new modules or directories (except `core/ui/src/main/res/font/` which doesn't exist yet). Two existing files modified, three font resource files added.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| III. Hardcoded onboarding colors | Onboarding uses a unique brand palette (#3D8A5A green, #F5F4F1 background) that is specific to this flow and should not pollute the global Material theme. | Adding these as theme colors would affect all screens and break Material 3 dynamic color support. Local `val` constants scoped to OnboardingScreen.kt are the minimal approach. |
