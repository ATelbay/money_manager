# Implementation Plan: Navigation Box-Overlay Refactor

**Branch**: `006-nav-box-overlay` | **Date**: 2026-03-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-nav-box-overlay/spec.md`

## Summary

Replace the Scaffold + AnimatedVisibility bottom bar architecture with a Box-overlay pattern to eliminate layout shifts, animation desynchronization, and workaround hacks (forceHideBottomBar, pendingNavAction, animateDpAsState). The bottom bar becomes a static overlay layer; top-level screens receive explicit bottom padding while detail screens render full-screen over it.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Material 3, Navigation Compose 2.9.7
**Storage**: N/A (no data changes)
**Testing**: JUnit 4, ComposeTestRule (existing UI tests with `bottomBar` testTag)
**Target Platform**: Android (minSdk per project config)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps during navigation transitions, zero layout shifts
**Constraints**: No new dependencies; SharedTransitionLayout must remain functional
**Scale/Scope**: 2 files changed in `app` module (MainActivity.kt, MoneyManagerNavHost.kt)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | No new modules — refactor within `app` module only |
| II. Kotlin-First & Compose | PASS | Pure Compose changes |
| III. Material 3 Design System | PASS | `NavigationBar` (M3) unchanged |
| IV. Animation & Motion | PASS | Removes unnecessary AnimatedVisibility; preserves all nav transitions. Motion tokens reused from `MoneyManagerMotion` |
| V. Hilt DI | N/A | No DI changes |
| VI. Room Database | N/A | No DB changes |
| VII. Testing Architecture | PASS | `bottomBar` testTag preserved; existing UI tests unaffected |
| VIII. Firebase Ecosystem | N/A | No Firebase changes |
| IX. Type-Safe Navigation | PASS | Routes, SharedTransitionLayout, LocalAnimatedVisibilityScope all preserved |
| X. Statement Import Pipeline | N/A | No import changes |
| XI. Preferences DataStore | N/A | No DataStore changes |

**Result**: All applicable gates PASS. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/006-nav-box-overlay/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (minimal — no data changes)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/java/com/atelbay/money_manager/
├── MainActivity.kt                         # MoneyManagerApp composable — Scaffold→Box refactor
└── navigation/
    ├── MoneyManagerNavHost.kt              # Accept bottomBarPadding, pass to top-level routes
    └── MoneyManagerBottomBar.kt            # Unchanged (overlay positioning done by parent)
```

**Structure Decision**: All changes are within the `app` module. No new files or modules. The refactor touches 2 files (MainActivity.kt, MoneyManagerNavHost.kt) and leaves MoneyManagerBottomBar.kt unchanged.

## Design Decisions

### D1: Bottom bar padding delivery mechanism

**Decision**: Pass `bottomBarPadding: Dp` parameter from `MoneyManagerApp` → `MoneyManagerNavHost` → top-level Route composables via their existing `modifier` parameter.

**Rationale**: All 4 top-level Route composables (TransactionListRoute, StatisticsRoute, AccountListRoute, SettingsRoute) already accept `modifier: Modifier = Modifier`. Passing `Modifier.padding(bottom = bottomBarPadding)` through is the simplest approach — no CompositionLocals, no measurement callbacks, no new abstractions.

**Alternatives rejected**:
- `CompositionLocal`: Adds indirection; overkill for a single Dp value passed to 4 call sites.
- `onGloballyPositioned` measurement: Introduces frame delay and state; harder to reason about.

### D2: Bottom bar height source

**Decision**: Use `NavigationBarDefaults` height constant (80.dp per Material 3 spec) as the padding value.

**Rationale**: The Material 3 `NavigationBar` has a fixed height of 80.dp. Using a constant avoids the complexity of runtime measurement while being accurate for all devices.

**Edge-to-edge note**: The app uses `enableEdgeToEdge()`, so Material 3's `NavigationBar` internally applies bottom system insets (gesture navigation bar). The actual rendered height of the bottom bar is 80.dp + system inset. However, the `bottomBarPadding` value should still be 80.dp because: the `NavigationBar` overlay sits at `Alignment.BottomCenter` and handles its own inset padding internally. Top-level screen content only needs to avoid the 80.dp navigation bar area — the system gesture inset below it is handled by the `NavigationBar` itself. Verify on a gesture-navigation device during T012.

**Alternative rejected**:
- Dynamic measurement via `onSizeChanged`: Adds a frame of delay before padding is applied, causing a brief layout shift on first composition — the exact problem we're eliminating.

### D3: Bottom bar visibility on detail screens

**Decision**: The bottom bar is conditionally composed — only rendered when `showBottomBar` is true (`currentTopLevel != null`). On detail screens, `currentTopLevel` is null, so the bar is removed from composition entirely. No z-order or touch-through concerns.

**Rationale**: Conditional composition is simpler and more correct than relying on z-order coverage. Since detail screens set `currentTopLevel` to null, the bar is not composed, eliminating both visual overlap and touch event concerns. The composition cost of removing/adding a `NavigationBar` is negligible for navigation transitions that already involve full-screen content changes.

### D4: FAB navigation simplification

**Decision**: Remove `forceHideBottomBar`, `pendingNavAction`, and the associated `LaunchedEffect` with delay. The FAB `onAddClick` callback navigates directly via `navController.navigate(TransactionEdit())`.

**Rationale**: The delay hack existed solely to let the bottom bar exit animation complete before navigating. Since the bottom bar no longer animates in/out, no delay is needed.

### D5: Onboarding flow

**Decision**: Conditionally compose the bottom bar: only render it when `showBottomBar` is true (current route is a top-level destination). During onboarding, no top-level destination is active, so the bar is not composed.

**Rationale**: Simpler than always composing and relying on z-order coverage during onboarding, since onboarding screens may not have opaque full-screen backgrounds.

## Implementation Approach

### Step 1: Refactor MoneyManagerApp (MainActivity.kt)

Replace `Scaffold` with `Box`:

```kotlin
// BEFORE (simplified):
Scaffold(
    bottomBar = { AnimatedVisibility(...) { MoneyManagerBottomBar(...) } }
) { padding ->
    val animatedBottomPadding by animateDpAsState(padding.calculateBottomPadding())
    MoneyManagerNavHost(modifier = Modifier.padding(bottom = animatedBottomPadding))
}

// AFTER (simplified):
val bottomBarHeight = 80.dp  // NavigationBar M3 standard height
Box(Modifier.fillMaxSize()) {
    MoneyManagerNavHost(
        bottomBarPadding = bottomBarHeight,
        modifier = Modifier.fillMaxSize(),
    )
    if (showBottomBar) {
        MoneyManagerBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
```

Remove: `forceHideBottomBar`, `pendingNavAction`, `bottomBarVisibility`, `bottomBarDuration`, `lastTopLevel`, related `LaunchedEffect` and `SideEffect` blocks, `animateDpAsState`.

Keep: `currentTopLevel` computation (needed for `showBottomBar`), `pendingNavigationManager` handling (for PDF import deep links).

### Step 2: Update MoneyManagerNavHost

Add `bottomBarPadding: Dp` parameter. Apply `Modifier.padding(bottom = bottomBarPadding)` to the 4 top-level Route composables via their `modifier` parameter:

```kotlin
fun MoneyManagerNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    bottomBarPadding: Dp = 0.dp,
    onFabNavigate: (() -> Unit)? = null,  // simplified to direct navigate
) {
    // ...
    composable<Home>(...) {
        TransactionListRoute(
            modifier = Modifier.padding(bottom = bottomBarPadding),
            // ...
        )
    }
    // Same for Statistics, AccountList, Settings
}
```

### Step 3: Simplify FAB callback

Change `onFabNavigate` from a delayed two-phase operation to a direct navigation call:

```kotlin
// BEFORE:
onFabNavigate = {
    forceHideBottomBar = true
    pendingNavAction = { navController.navigate(TransactionEdit()) }
}

// AFTER:
onFabNavigate = { navController.navigate(TransactionEdit()) }
```

## Complexity Tracking

No constitution violations — this section is not applicable.
