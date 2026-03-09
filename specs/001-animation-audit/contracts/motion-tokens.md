# Contract: Motion Tokens API

**Module**: `core:ui` | **Package**: `com.atelbay.money_manager.core.ui.theme`

## Public API Surface

### MoneyManagerMotion

```kotlin
package com.atelbay.money_manager.core.ui.theme

object MoneyManagerMotion {
    // ── Duration tokens ──
    const val DurationShort: Int = 150
    const val DurationMedium: Int = 300
    const val DurationLong: Int = 500
    const val DurationExtraLong: Int = 800
    const val StaggerDelay: Long = 50L
    const val ReducedDuration: Int = 10

    // ── Easing tokens (M3) ──
    val EnterEasing: Easing          // EmphasizedDecelerateEasing
    val ExitEasing: Easing           // EmphasizedAccelerateEasing
    val StandardEasing: Easing       // FastOutSlowInEasing

    // ── Navigation transitions ──
    // Drill-in (forward/back with slide + fade)
    fun drillInEnter(): EnterTransition
    fun drillInExit(): ExitTransition
    fun drillInPopEnter(): EnterTransition
    fun drillInPopExit(): ExitTransition

    // Tab switch (crossfade only)
    fun tabEnter(): EnterTransition
    fun tabExit(): ExitTransition

    // ── Prebuilt animation specs ──
    val InteractionSpring: SpringSpec<Float>
    val ColorTransition: AnimationSpec<Color>
    val CounterSpec: TweenSpec<Float>
    val ChartSpec: TweenSpec<Float>
    val DonutSpec: TweenSpec<Float>
    val StaggerFadeSpec: TweenSpec<Float>
    val ItemFadeInSpec: TweenSpec<Float>
    val ItemFadeOutSpec: TweenSpec<Float>
    val ItemPlacementSpec: SpringSpec<IntOffset>

    // ── Reduce Motion helpers ──
    const val MaxStaggerItems: Int = 10   // Cap: items beyond this get 0 extra delay

    fun duration(baseMs: Int, reduceMotion: Boolean): Int
    fun staggerDelay(index: Int, reduceMotion: Boolean): Long
    // Returns: if reduceMotion → 0L; if index >= MaxStaggerItems → MaxStaggerItems * StaggerDelay; else index * StaggerDelay
}
```

### LocalReduceMotion

```kotlin
package com.atelbay.money_manager.core.ui.util

val LocalReduceMotion: ProvidableCompositionLocal<Boolean>

@Composable
fun isReduceMotionEnabled(): Boolean
```

## Usage Contract

### Navigation (MoneyManagerNavHost)

```kotlin
NavHost(
    enterTransition = { MoneyManagerMotion.drillInEnter() },
    exitTransition = { MoneyManagerMotion.drillInExit() },
    popEnterTransition = { MoneyManagerMotion.drillInPopEnter() },
    popExitTransition = { MoneyManagerMotion.drillInPopExit() },
) {
    composable<Home>(
        enterTransition = { MoneyManagerMotion.tabEnter() },
        exitTransition = { MoneyManagerMotion.tabExit() },
        popEnterTransition = { MoneyManagerMotion.tabEnter() },
        popExitTransition = { MoneyManagerMotion.tabExit() },
    ) { ... }
}
```

### Component animation

```kotlin
// Before:
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.92f else 1f,
    animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
)

// After:
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.92f else 1f,
    animationSpec = MoneyManagerMotion.InteractionSpring,
)
```

### Staggered animation

```kotlin
// Before:
delay(index * 60L)
alpha.animateTo(1f, tween(300))

// After:
val reduceMotion = LocalReduceMotion.current
delay(MoneyManagerMotion.staggerDelay(index, reduceMotion))
alpha.animateTo(1f, MoneyManagerMotion.StaggerFadeSpec)
```

## Backward Compatibility

- No public API breaking changes (core:ui does not expose animation specs today)
- All changes are internal implementation details of existing composables
- No new Gradle modules — only new files in existing `core:ui`

## Versioning

This contract is versioned with the `core:ui` module. Changes to token values are non-breaking. Changes to token names or removal of tokens require deprecation cycle.
