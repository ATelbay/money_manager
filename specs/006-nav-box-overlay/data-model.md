# Data Model: Navigation Box-Overlay Refactor

**Date**: 2026-03-12 | **Branch**: `006-nav-box-overlay`

## Summary

No data model changes. This is a pure UI layout refactor — no entities, database tables, or data flows are modified.

## State Changes

The following UI state variables are **removed** from `MoneyManagerApp`:

| Variable | Type | Purpose (removed) |
|----------|------|--------------------|
| `forceHideBottomBar` | `Boolean` | Forced bottom bar hidden during FAB navigation |
| `pendingNavAction` | `(() -> Unit)?` | Delayed navigation action after bar exit animation |
| `bottomBarVisibility` | `MutableTransitionState<Boolean>` | AnimatedVisibility state for bottom bar |
| `bottomBarDuration` | `Int` | Animation duration for bottom bar enter/exit |
| `animatedBottomPadding` | `Dp` (animated) | Animated bottom padding to sync with bar animation |
| `lastTopLevel` | `TopLevelDestination?` | Cached last top-level to show during exit animation |

The following UI state variables are **retained**:

| Variable | Type | Purpose |
|----------|------|---------|
| `currentTopLevel` | `TopLevelDestination?` | Determines `showBottomBar` boolean |
| `showBottomBar` | `Boolean` | Whether bottom bar is composed (true for top-level screens) |

## New Parameters

| Parameter | Location | Type | Purpose |
|-----------|----------|------|---------|
| `bottomBarPadding` | `MoneyManagerNavHost` | `Dp` | Bottom padding applied to top-level screen modifiers |
