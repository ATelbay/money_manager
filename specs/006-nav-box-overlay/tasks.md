# Tasks: Navigation Box-Overlay Refactor

**Input**: Design documents from `/specs/006-nav-box-overlay/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not requested — no test tasks generated.

**Organization**: Tasks grouped by user story. US1+US2 share the core refactor (both P1). US3 and US4 are achieved by the same structural change but tracked separately.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Exact file paths included in descriptions

## Path Conventions

All changes are in `app/src/main/java/com/atelbay/money_manager/`:
- `MainActivity.kt` — `MoneyManagerApp` composable
- `navigation/MoneyManagerNavHost.kt` — NavHost with all routes

---

## Phase 1: Setup

**Purpose**: No setup needed — no new dependencies, modules, or files.

*(Skipped — all changes are refactors to existing files)*

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational work needed — refactoring existing code only.

*(Skipped — no shared infrastructure to create)*

---

## Phase 3: User Story 1+2 — Seamless navigation + Always-visible bottom bar (Priority: P1) 🎯 MVP

**Goal**: Replace Scaffold with Box-overlay layout. Bottom bar is a static overlay on top-level screens; no layout shifts during navigation.

**Independent Test**: Navigate between all 4 tabs — bottom bar stays stationary with no content jumps. Navigate to any detail screen and back — no layout shift.

### Implementation

- [x] T001 [US1] Remove animation hack state variables (`forceHideBottomBar`, `pendingNavAction`, `bottomBarVisibility`, `bottomBarDuration`, `lastTopLevel`, `animatedBottomPadding`) and their associated `LaunchedEffect`/`SideEffect` blocks from `MoneyManagerApp` in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
- [x] T002 [US1] Replace `Scaffold` with `Box(Modifier.fillMaxSize())` in `MoneyManagerApp`. Place `MoneyManagerNavHost` as first child (fills entire Box) and `MoneyManagerBottomBar` as second child with `Modifier.align(Alignment.BottomCenter)`, conditionally composed when `showBottomBar` is true, in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
- [x] T003 [US2] Add `bottomBarPadding: Dp` parameter (default 0.dp) to `MoneyManagerNavHost` in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`
- [x] T004 [US2] Apply `Modifier.padding(bottom = bottomBarPadding)` to the 4 top-level Route composables (TransactionListRoute, StatisticsRoute, AccountListRoute, SettingsRoute) via their existing `modifier` parameter in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`
- [x] T005 [US1] Pass `bottomBarPadding = 80.dp` (Material 3 NavigationBar height) from `MoneyManagerApp` to `MoneyManagerNavHost` in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`

**Checkpoint**: Tab navigation is seamless, bottom bar is stationary, top-level content is not obscured by the bar overlay.

---

## Phase 4: User Story 3 — Detail screens cover the bottom bar (Priority: P2)

**Goal**: Detail screens render full-screen, covering the bottom bar area. No extra padding applied to detail routes.

**Independent Test**: Open each detail screen (TransactionEdit, CategoryEdit, AccountEdit, Import, CategoryList, CurrencyPicker, SignIn) and verify the bottom bar is not visible.

### Implementation

- [x] T006 [US3] Verify that detail route composables in `MoneyManagerNavHost` do NOT receive `bottomBarPadding` — they should use their default `modifier` (no bottom padding), allowing them to render over the bottom bar area, in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`

**Checkpoint**: Detail screens fill entire screen. Bottom bar is not composed on detail screens (`showBottomBar` = false when `currentTopLevel` = null), so no touch-through or z-order concerns.

---

## Phase 5: User Story 4 — FAB navigation without delays (Priority: P2)

**Goal**: FAB navigates directly to TransactionEdit without delay or intermediate animation state.

**Independent Test**: Tap FAB on Home screen — navigation is instant with no visible delay.

### Implementation

- [x] T007 [US4] Simplify `onFabNavigate` callback to directly call `navController.navigate(TransactionEdit())` — remove the two-phase `forceHideBottomBar`/`pendingNavAction` pattern in `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`

**Checkpoint**: FAB tap navigates instantly. No bottom bar exit animation precedes the navigation.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Clean up imports, verify build, validate all scenarios.

- [x] T008 [P] Remove unused imports (`AnimatedVisibility`, `animateDpAsState`, `MutableTransitionState`, `slideInVertically`, `slideOutVertically`, `fadeIn`, `fadeOut`, `tween` if no longer used, `delay`) from `app/src/main/java/com/atelbay/money_manager/MainActivity.kt`
- [x] T009 [P] Verify `MoneyManagerNavHost` imports are clean — add `androidx.compose.ui.unit.Dp` import if needed, remove any unused imports, in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`
- [ ] T010 Build verification: run `./gradlew assembleDebug` and fix any compile errors
- [ ] T011 Run `./gradlew test` to verify existing unit tests pass
- [ ] T012 Run manual verification per `specs/006-nav-box-overlay/quickstart.md` and edge cases from spec.md: (1) tab navigation — bottom bar stationary, no layout shifts; (2) detail navigation — bar hidden, no layout shift on back; (3) FAB — instant navigation; (4) theme switch — circle-reveal animation works with Box layout; (5) onboarding flow — bottom bar not visible during Onboarding/CreateAccount; (6) shared element transitions — TransactionEdit shared elements animate correctly; (7) rapid tab switching — overlay stable; (8) system insets — bottom bar padding correct on gesture-navigation devices

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 3 (US1+US2)**: No dependencies — start immediately. This is the core refactor.
- **Phase 4 (US3)**: Depends on Phase 3 — detail screen behavior depends on Box layout being in place.
- **Phase 5 (US4)**: Depends on Phase 3 — FAB simplification depends on removal of animation hacks.
- **Phase 6 (Polish)**: Depends on Phases 3-5 completion.

### User Story Dependencies

- **US1+US2 (P1)**: Core refactor — no dependencies on other stories. MVP.
- **US3 (P2)**: Verification task — depends on US1+US2 Box layout being complete.
- **US4 (P2)**: FAB simplification — depends on US1 hack removal.

### Within Phase 3 (US1+US2)

```
T001 (remove hacks) → T002 (Scaffold→Box) → T005 (pass padding)
                                                    ↑
T003 (add param) → T004 (apply to routes) ──────────┘
```

- T001 and T003 can start in parallel (different files)
- T002 depends on T001 (same file, hacks must be removed first)
- T004 depends on T003 (same file, param must exist)
- T005 depends on T002 and T004 (wires both files together)

### Parallel Opportunities

```
# Parallel start — different files:
Agent A: T001 (MainActivity.kt — remove hacks)
Agent B: T003 (MoneyManagerNavHost.kt — add param)

# Then sequential within each file:
Agent A: T001 → T002 → T005
Agent B: T003 → T004

# After Phase 3:
T006, T007 can run in parallel (verification + FAB, different concerns)

# Polish parallel:
T008 and T009 in parallel (different files)
```

---

## Implementation Strategy

### MVP First (US1+US2 Only)

1. Complete Phase 3: Core Scaffold→Box refactor
2. **STOP and VALIDATE**: Tab navigation seamless, no layout shifts
3. This alone delivers the primary value (layout shift elimination)

### Incremental Delivery

1. Phase 3: US1+US2 → Core refactor → Validate tab+detail navigation
2. Phase 4: US3 → Verify detail screens cover bar → Validate
3. Phase 5: US4 → Simplify FAB → Validate instant navigation
4. Phase 6: Polish → Clean imports, build, full manual test

---

## Notes

- Total scope: 2 files modified, 0 files created, 0 files deleted
- T006 is a verification/audit task, not a code change (detail routes already don't receive padding)
- T007 may already be done as part of T001 (hack removal includes the FAB pattern) — verify and mark complete if so
- The `bottomBarPadding = 80.dp` constant comes from Material 3 NavigationBar spec height
- `showBottomBar` logic (`currentTopLevel != null`) is already correct and retained from current code
