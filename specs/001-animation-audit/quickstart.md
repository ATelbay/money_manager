# Quickstart: Animation Audit & Motion System

**Branch**: `001-animation-audit`

## Prerequisites

- Android Studio with Compose BOM 2026.01.01
- Project builds: `./gradlew assembleDebug`

## Key Files to Understand

1. **`core/ui/src/main/java/.../theme/Motion.kt`** — All motion tokens (NEW)
2. **`app/src/main/java/.../MainActivity.kt`** — Theme reveal animation (REFACTOR)
3. **`app/src/main/java/.../navigation/MoneyManagerNavHost.kt`** — Navigation transitions (REFACTOR)

## Implementation Order

### Phase 1: Create Motion Token System
1. Create `Motion.kt` in `core:ui/theme/` with all duration/easing/spec tokens
2. Create `ReduceMotion.kt` in `core:ui/util/` for accessibility
3. Wire `LocalReduceMotion` into `MoneyManagerTheme`

### Phase 2: Fix Theme Bug (P2 priority, but blocking)
1. Remove dual NavController from `MainActivity.kt`
2. Implement `graphicsLayer.record()` snapshot approach
3. Animate `CircleRevealShape(inverted=true)` on snapshot overlay
4. Test: switch theme on Settings → verify no Home screen flash

### Phase 3: Tokenize Navigation
1. Replace `MoneyManagerNavHost` easing with `MoneyManagerMotion.drillInEnter()` etc.
2. Replace tab transitions with `MoneyManagerMotion.tabEnter()` etc.
3. Update bottom bar animation in `MainActivity.kt`

### Phase 4: Tokenize Components
1. `MoneyManagerFAB.kt`, `MoneyManagerButton.kt` → `InteractionSpring`
2. `MoneyManagerBottomNavBar.kt` → `ColorTransition`
3. `BalanceCard.kt`, `IncomeExpenseCard.kt` → `CounterSpec`
4. `TransactionListItem.kt` → `ColorTransition`

### Phase 5: Tokenize Screens
1. `StatisticsScreen.kt` — donut, bar chart, stagger animations
2. `TransactionListScreen.kt` — animateItem specs
3. `SettingsScreen.kt` — theme selector color animation

### Phase 6: Reduce Motion
1. Add reduce motion checks to all animated components
2. Test with `adb shell settings put global animator_duration_scale 0`

## Verification

```bash
# Build
./gradlew assembleDebug

# Lint + Detekt
./gradlew lint detekt

# Unit tests
./gradlew test

# Check for hardcoded durations (should return 0 outside Motion.kt)
grep -rn "tween([0-9]" --include="*.kt" core/ui/src presentation/*/src app/src \
  | grep -v "Motion.kt" | grep -v "test/"
```

## Common Pitfalls

- Don't use `spring()` or `tween()` directly — always reference `MoneyManagerMotion.*`
- Don't create animation specs inside `@Composable` — they're top-level vals in `MoneyManagerMotion`
- Theme reveal: never create a second `NavController` — use graphics layer snapshot
- Reduce motion: keep transition *type* (slide stays slide), only shorten *duration*
