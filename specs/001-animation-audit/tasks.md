# Tasks: Animation Audit & Motion System

**Input**: Design documents from `/specs/001-animation-audit/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/motion-tokens.md

**Tests**: Not explicitly requested in spec. Test tasks omitted.

**Organization**: Tasks grouped by user story. US3 (motion token system) is the foundation for US1 and US2, so it's implemented in the Foundational phase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No new modules or dependencies needed. Verify build is green.

- [ ] T001 Verify project builds cleanly with `./gradlew assembleDebug`

---

## Phase 2: Foundational — Motion Token System (Blocking Prerequisites)

**Purpose**: Create centralized motion tokens and accessibility support. This IS the core of US3 (P3) but MUST be done first because US1 and US2 depend on it.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 Create `MoneyManagerMotion` object with all duration/easing/spec tokens in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/Motion.kt`
  - Duration tokens: `DurationShort=150`, `DurationMedium=300`, `DurationLong=500`, `DurationExtraLong=800`, `StaggerDelay=50L`, `ReducedDuration=10`
  - Easing tokens: `EnterEasing=EmphasizedDecelerateEasing`, `ExitEasing=EmphasizedAccelerateEasing`, `StandardEasing=FastOutSlowInEasing`
  - Navigation transition functions: `drillInEnter()`, `drillInExit()`, `drillInPopEnter()`, `drillInPopExit()`, `tabEnter()`, `tabExit()`
  - Prebuilt specs: `InteractionSpring`, `ColorTransition`, `CounterSpec`, `ChartSpec`, `DonutSpec`, `StaggerFadeSpec`, `ItemFadeInSpec`, `ItemFadeOutSpec`, `ItemPlacementSpec`
  - Helper functions: `duration(baseMs, reduceMotion)`, `staggerDelay(index, reduceMotion)`
  - Stagger cap: `staggerDelay()` must cap at `MaxStaggerItems = 10` — items beyond index 10 get zero additional delay (prevents 100+ items creating 5s+ total stagger, keeping within SC-005 ≤1s)
  - All specs as top-level vals (not inside composable scope)
- [ ] T003 Create `LocalReduceMotion` CompositionLocal and `isReduceMotionEnabled()` composable in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/util/ReduceMotion.kt`
  - Read `AccessibilityManager` (API 33+) or `Settings.Global.ANIMATOR_DURATION_SCALE`
  - Return `Boolean` — true when system requests reduced motion
- [ ] T004 Provide `LocalReduceMotion` in `MoneyManagerTheme` composable in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/Theme.kt`
  - Add `CompositionLocalProvider(LocalReduceMotion provides isReduceMotionEnabled())` wrapping existing content

**Checkpoint**: Motion tokens available from any module that depends on `core:ui`. Build passes.

---

## Phase 3: User Story 1 — Плавные и консистентные анимации (Priority: P1) 🎯 MVP

**Goal**: All 45+ animations use centralized tokens from `MoneyManagerMotion`. Zero hardcoded durations/easings outside `Motion.kt`.

**Independent Test**: Open each screen, verify transitions are consistent. Run `grep -rn "tween([0-9]" --include="*.kt" core/ui/src presentation/*/src app/src | grep -v Motion.kt` → should return 0 results.

### Navigation Transitions

- [ ] T005 [US1] Replace hardcoded easing/duration in `MoneyManagerNavHost` default transitions with `MoneyManagerMotion.drillInEnter()` / `.drillInExit()` / `.drillInPopEnter()` / `.drillInPopExit()` in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`
  - Remove imports of `LinearOutSlowInEasing`, `FastOutLinearInEasing`
  - Import `MoneyManagerMotion` from `core:ui`
- [ ] T006 [US1] Replace tab-route transitions (Home, Statistics, AccountList, Settings) with `MoneyManagerMotion.tabEnter()` / `.tabExit()` in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`
  - Apply to all 4 tab composable declarations (lines ~88-92, ~147-151, ~156-160, ~178-182)
- [ ] T007 [US1] Replace bottom bar animation hardcoded durations/easing with `MoneyManagerMotion` tokens in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
  - Replace `BottomBarAnimationDurationMs` const with `MoneyManagerMotion.DurationShort`
  - Replace `FastOutSlowInEasing` with `MoneyManagerMotion.StandardEasing`
  - Update `slideInVertically`, `fadeIn`, `slideOutVertically`, `fadeOut`, and `animateDpAsState` calls

### Core UI Components

- [ ] T008 [P] [US1] Replace spring/tween in `MoneyManagerFAB` with `MoneyManagerMotion.InteractionSpring` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/MoneyManagerFAB.kt`
- [ ] T009 [P] [US1] Replace spring in `MoneyManagerButton` with `MoneyManagerMotion.InteractionSpring` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/MoneyManagerButton.kt`
- [ ] T010 [P] [US1] Replace spring in `MoneyManagerBottomNavBar` with `MoneyManagerMotion.ColorTransition` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/MoneyManagerBottomNavBar.kt`
- [ ] T011 [P] [US1] Replace tween(800) in `BalanceCard` with `MoneyManagerMotion.CounterSpec` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/BalanceCard.kt`
- [ ] T012 [P] [US1] Replace tween(1000) in `IncomeExpenseCard` with `MoneyManagerMotion.CounterSpec` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/IncomeExpenseCard.kt`
- [ ] T013 [P] [US1] Replace color animation in `TransactionListItem` with `MoneyManagerMotion.ColorTransition` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/TransactionListItem.kt`

### Screen-Level Animations

- [ ] T014 [P] [US1] Tokenize `StatisticsScreen` animations in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`
  - Donut chart: replace `tween(800)` with `MoneyManagerMotion.DonutSpec`
  - Bar chart: replace `tween(600)` with `MoneyManagerMotion.ChartSpec`
  - Stagger: replace `delay(index * 60L)` with `delay(index * MoneyManagerMotion.StaggerDelay)` and `tween(300)` with `MoneyManagerMotion.StaggerFadeSpec`
- [ ] T015 [P] [US1] Tokenize `TransactionListScreen` animateItem specs in `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListScreen.kt`
  - Replace `tween(200)` fadeIn with `MoneyManagerMotion.ItemFadeInSpec`
  - Replace `spring(380f, 0.8f)` placement with `MoneyManagerMotion.ItemPlacementSpec`
  - Replace `tween(150)` fadeOut with `MoneyManagerMotion.ItemFadeOutSpec`
- [ ] T016 [P] [US1] Tokenize `SettingsScreen` theme selector animation in `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsScreen.kt`
  - Replace `tween(250)` with `MoneyManagerMotion.ColorTransition` for both background and content color transitions

### Recomposition Audit (FR-010)

- [ ] T017 [US1] Audit all tokenized components for composition-phase transform animations and migrate to `Modifier.graphicsLayer` where applicable across `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/` and `presentation/*/src/`
  - Check scale animations (FAB, Button) use `graphicsLayer { scaleX = ...; scaleY = ... }` instead of `Modifier.scale()`
  - Check alpha animations (stagger rows) use `graphicsLayer { alpha = ... }` (already correct in StatisticsScreen)
  - Check offset animations use `graphicsLayer { translationX = ... }` where applicable
  - Verify no animation drives recomposition through mutable state (use Layout Inspector recomposition counter)

**Checkpoint**: All animations use `MoneyManagerMotion` tokens. No unnecessary recompositions. `./gradlew assembleDebug` passes. Visual verification: all transitions consistent.

---

## Phase 4: User Story 2 — Корректная смена темы без артефактов (Priority: P2)

**Goal**: Theme switch uses single NavController + graphicsLayer snapshot. No flash of wrong screen.

**Independent Test**: Go to Settings, switch theme light→dark→system repeatedly. Verify: always see Settings screen during transition, never Home or other screen.

- [ ] T018 [US2] Add `inverted` parameter to `CircleRevealShape` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/CircleRevealShape.kt`
  - When `inverted=true`: clip area is *outside* the circle (old theme overlay shrinks to reveal new theme)
  - When `inverted=false` (default): existing behavior preserved
- [ ] T019 [US2] Refactor theme reveal in `MainActivity.kt` to single NavController + `rememberGraphicsLayer()` snapshot in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
  - Remove `bottomNavController` — keep only one `rememberNavController()`
  - Add `rememberGraphicsLayer()` to capture current frame before theme change
  - On theme change: record current content via `graphicsLayer.record(size) { drawContent() }`
  - Set `isRevealing = true`, show snapshot overlay with `CircleRevealShape(inverted=true)`
  - Animate `revealRadius` from `maxRadius → 0` (inverted: circle shrinks, revealing new theme underneath)
  - On animation complete: `isRevealing = false`, clear snapshot
  - Replace second `MoneyManagerApp(navController = bottomNavController, ...)` with snapshot layer
  - Use `MoneyManagerMotion.DurationLong` for reveal duration
  - Use `MoneyManagerMotion.StandardEasing` for reveal easing

**Checkpoint**: Theme switch works with single NavController. Settings→switch theme shows Settings throughout. No Home screen flash. Rapid switching doesn't crash.

---

## Phase 5: User Story 3 — Централизованная motion-система (Priority: P3)

**Goal**: Developer ergonomics — reduce motion support wired into all animated components.

**Independent Test**: Run `adb shell settings put global animator_duration_scale 0`, verify all animations effectively instant. Reset with `adb shell settings put global animator_duration_scale 1`.

- [ ] T020 [US3] Add reduce motion support to navigation transitions in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`
  - Read `LocalReduceMotion.current` and pass to `MoneyManagerMotion` duration helper
  - If reduce motion: use `MoneyManagerMotion.ReducedDuration` for all nav transition tweens
- [ ] T021 [US3] Add reduce motion support to bottom bar animation in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
  - Read `LocalReduceMotion.current` for bottom bar slide/fade durations
- [ ] T022 [P] [US3] Add reduce motion support to `StatisticsScreen` stagger and chart animations in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt`
  - Use `MoneyManagerMotion.staggerDelay(index, reduceMotion)` — returns 0 when reduce motion
  - Chart animations: use `MoneyManagerMotion.duration(baseMs, reduceMotion)`
- [ ] T023 [P] [US3] Add reduce motion support to `BalanceCard` and `IncomeExpenseCard` counter animations in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/BalanceCard.kt` and `core/ui/src/main/java/com/atelbay/money_manager/core/ui/components/IncomeExpenseCard.kt`
- [ ] T024 [US3] Add reduce motion support to theme reveal in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
  - If reduce motion: skip reveal animation, apply theme change instantly

**Checkpoint**: `adb shell settings put global animator_duration_scale 0` → all animations near-instant. Type of transition preserved (slide stays slide). No crashes.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup across all stories.

- [ ] T025 Verify zero hardcoded animation values outside `Motion.kt` — run grep check from quickstart.md
- [ ] T026 Run full build validation: `./gradlew assembleDebug test lint detekt`
- [ ] T027 Manual visual QA on device: verify all navigation transitions, theme switching, chart animations, stagger effects, button feedback
- [ ] T028 Run quickstart.md verification steps

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — verify build
- **Foundational (Phase 2)**: Depends on Phase 1 — creates motion tokens (BLOCKS all user stories)
- **US1 (Phase 3)**: Depends on Phase 2 — tokenizes all existing animations
- **US2 (Phase 4)**: Depends on Phase 2 — can run in parallel with US1
- **US3 (Phase 5)**: Depends on Phase 3 (needs tokenized animations to add reduce motion to)
- **Polish (Phase 6)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Depends on Foundation (Phase 2) only. No other story dependencies.
- **US2 (P2)**: Depends on Foundation (Phase 2) only. Independent of US1.
- **US3 (P3)**: Depends on US1 completion (reduce motion wraps tokenized animations).

### Within Each User Story

- Navigation tasks before component tasks (components may depend on nav patterns)
- Core:ui components can be parallelized (different files)
- Screen-level tasks can be parallelized (different modules)

### Parallel Opportunities

- **Phase 2**: T002 and T003 are sequential (T003 references types from T002). T004 depends on T003.
- **Phase 3**: T008-T016 are all parallelizable (different files in different modules)
- **Phase 4**: T018 before T019 (T019 uses inverted CircleRevealShape)
- **Phase 5**: T022 and T023 parallelizable (different files)

---

## Parallel Example: User Story 1

```
# After T005-T007 (navigation — sequential, same files):

# Launch all component tokenizations in parallel:
Task T008: "Replace spring in MoneyManagerFAB with MoneyManagerMotion.InteractionSpring"
Task T009: "Replace spring in MoneyManagerButton with MoneyManagerMotion.InteractionSpring"
Task T010: "Replace spring in MoneyManagerBottomNavBar with MoneyManagerMotion.ColorTransition"
Task T011: "Replace tween in BalanceCard with MoneyManagerMotion.CounterSpec"
Task T012: "Replace tween in IncomeExpenseCard with MoneyManagerMotion.CounterSpec"
Task T013: "Replace color animation in TransactionListItem with MoneyManagerMotion.ColorTransition"

# Launch all screen tokenizations in parallel:
Task T014: "Tokenize StatisticsScreen animations"
Task T015: "Tokenize TransactionListScreen animateItem specs"
Task T016: "Tokenize SettingsScreen theme selector animation"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundation (T002-T004)
3. Complete Phase 3: User Story 1 (T005-T017)
4. **STOP and VALIDATE**: All animations use centralized tokens, no unnecessary recompositions
5. This delivers the primary value — unified motion system

### Incremental Delivery

1. Setup + Foundation → Motion tokens available (T001-T004)
2. Add US1 → All animations tokenized + recomposition audit → Visual QA (T005-T017)
3. Add US2 → Theme bug fixed → Test theme switching (T018-T019)
4. Add US3 → Reduce motion → Accessibility test (T020-T024)
5. Polish → Final validation (T025-T028)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- US3 Foundation (motion tokens) is in Phase 2 because it blocks US1 and US2
- Theme bug (US2) can be worked in parallel with US1 after Foundation
- Commit after each task or logical group (per `git-conventional-commits` skill)
- Total: 28 tasks across 6 phases
