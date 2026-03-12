# Quickstart: Navigation Box-Overlay Refactor

**Branch**: `006-nav-box-overlay`

## What Changed

The app's root layout switched from `Scaffold` (with `AnimatedVisibility` bottom bar) to a `Box` overlay pattern:

- **Before**: `Scaffold` managed bottom bar visibility with `AnimatedVisibility` (slide+fade). NavHost received animated bottom padding. FAB navigation used a delayed two-phase hack.
- **After**: `Box` contains NavHost (full-screen) + bottom bar (overlay at bottom-center). Top-level screens get explicit bottom padding. Detail screens render over the bar. FAB navigates directly.

## Files Modified

1. **`app/.../MainActivity.kt`** — `MoneyManagerApp` composable: Scaffold→Box, removed animation hacks
2. **`app/.../navigation/MoneyManagerNavHost.kt`** — Added `bottomBarPadding: Dp` parameter, applied to top-level routes

## How to Verify

```bash
# Build
./gradlew assembleDebug

# Run existing tests
./gradlew test
./gradlew connectedAndroidTest
```

### Manual verification

1. Navigate between all 4 tabs — bottom bar stays stationary
2. Open a detail screen (tap transaction) — bottom bar disappears, no layout shift
3. Press back from detail — bottom bar reappears, no layout shift
4. Tap FAB — navigation is instant, no visible delay
5. Switch theme — circle-reveal animation works correctly
6. Complete onboarding flow — bottom bar appears only after first tab screen

## Architecture Notes

- `bottomBarPadding` = 80.dp (Material 3 NavigationBar height)
- Bottom bar is conditionally composed (only when `currentTopLevel != null`)
- Detail screens cover the bar area naturally — no AnimatedVisibility needed
- SharedTransitionLayout and all nav transitions unchanged
