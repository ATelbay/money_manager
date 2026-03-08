# Implementation Plan: Animation Audit & Motion System

**Branch**: `001-animation-audit` | **Date**: 2026-03-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-animation-audit/spec.md`

## Summary

Провести аудит всех анимаций, создать централизованную motion-систему на базе M3 `MotionScheme` и исправить баг смены темы. Все 45+ анимаций в проекте будут переведены на единые токены, навигация — на M3 Emphasized easing, а circular reveal будет переписан с одним NavController.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Material 3, Navigation Compose 2.9.7
**Storage**: N/A (чисто UI-фича)
**Testing**: JUnit 4 + MockK + Turbine (unit), ComposeTestRule (UI)
**Target Platform**: Android (minSdk defined in convention plugins)
**Project Type**: Mobile-app (Android)
**Performance Goals**: 60 fps навигационных переходов, 0 dropped frames на среднем устройстве
**Constraints**: Все анимации ≤1000ms, Reduce Motion → ≤50ms, без лишних recomposition
**Scale/Scope**: ~45 анимаций в 13+ файлах, 8 presentation-модулей + core:ui + app

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | ✅ PASS | Motion tokens в `core:ui` (shared infrastructure). Нет новых модулей — всё в существующих. |
| II. Kotlin-First & Jetpack Compose | ✅ PASS | 100% Compose, без XML. |
| III. Material 3 Design System | ✅ PASS | Переход на `MaterialTheme.motionScheme` и M3 easing. |
| IV. Animation & Motion | ✅ PASS | Основная цель фичи. Центральные токены, `graphicsLayer`, `remember`-based specs. |
| V. Hilt DI | ✅ N/A | Не затрагивается. |
| VI. Room Database | ✅ N/A | Не затрагивается. |
| VII. Testing Architecture | ✅ PASS | Unit-тесты для motion tokens, UI-тесты для навигации. |
| VIII. Firebase Ecosystem | ✅ N/A | Не затрагивается. |
| IX. Type-Safe Navigation | ✅ PASS | NavHost сохраняется, переходы обновляются, type-safe routes без изменений. |
| X. Statement Import | ✅ N/A | Не затрагивается. |
| XI. Preferences DataStore | ✅ PASS | `theme_mode` остаётся в DataStore. Может добавиться `reduce_motion` ключ. |

**Gate Result**: ✅ All principles pass. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-animation-audit/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── motion-tokens.md # Motion token contract
└── tasks.md             # Phase 2 output (from /speckit.tasks)
```

### Source Code (repository root)

```text
core/ui/src/main/java/com/atelbay/money_manager/core/ui/
├── theme/
│   ├── Theme.kt                    # MoneyManagerTheme (exists)
│   ├── Color.kt                    # Color schemes (exists)
│   └── Motion.kt                   # NEW: Motion tokens object
├── components/
│   ├── CircleRevealShape.kt        # Exists — refactored
│   ├── MoneyManagerFAB.kt          # Exists — tokenized
│   ├── MoneyManagerButton.kt       # Exists — tokenized
│   ├── MoneyManagerBottomNavBar.kt  # Exists — tokenized
│   ├── BalanceCard.kt              # Exists — tokenized
│   ├── IncomeExpenseCard.kt        # Exists — tokenized
│   └── TransactionListItem.kt     # Exists — tokenized
└── util/
    └── ReduceMotion.kt             # NEW: Accessibility helper

app/src/main/java/com/atelbay/money_manager/
├── MainActivity.kt                 # Refactored — single NavController reveal
└── navigation/
    └── MoneyManagerNavHost.kt       # Refactored — M3 easing tokens

presentation/
├── transactions/ui/list/TransactionListScreen.kt  # Tokenized
├── statistics/ui/StatisticsScreen.kt              # Tokenized
└── settings/ui/SettingsScreen.kt                  # Tokenized
```

**Structure Decision**: Изменения в существующих модулях (`core:ui`, `app`, presentation модули). Единственный новый файл — `Motion.kt` в `core:ui/theme/`. Нет новых Gradle-модулей.

## Complexity Tracking

> No violations — no justification needed.
