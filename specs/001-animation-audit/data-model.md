# Data Model: Animation Audit & Motion System

**Branch**: `001-animation-audit` | **Date**: 2026-03-08

## Entities

### MoneyManagerMotion (object — core:ui/theme/Motion.kt)

Центральный singleton с motion tokens. Не Room entity — Kotlin object с const/val свойствами.

```kotlin
object MoneyManagerMotion {
    // Duration tokens (ms)
    const val DurationShort = 150       // Bottom bar, quick feedback
    const val DurationMedium = 300      // Nav transitions, color changes
    const val DurationLong = 500        // Theme reveal, bar chart
    const val DurationExtraLong = 800   // Donut chart, balance counter
    const val StaggerDelay = 50L        // Per-item stagger delay

    // Easing (from M3)
    val EnterEasing = EmphasizedDecelerateEasing   // Entering elements
    val ExitEasing = EmphasizedAccelerateEasing     // Exiting elements
    val StandardEasing = FastOutSlowInEasing        // Standard emphasis

    // Prebuilt animation specs (top-level vals, allocated once)
    val NavEnterSpec: FiniteAnimationSpec<IntOffset>
    val NavExitSpec: FiniteAnimationSpec<IntOffset>
    val FadeInSpec: FiniteAnimationSpec<Float>
    val FadeOutSpec: FiniteAnimationSpec<Float>
    val InteractionSpring: SpringSpec<Float>         // stiffness=400, damping=0.6
    val ColorTransitionSpec: AnimationSpec<Color>
    val CounterSpec: FiniteAnimationSpec<Float>       // DurationExtraLong
    val ChartSpec: FiniteAnimationSpec<Float>          // DurationLong
    val DonutSpec: FiniteAnimationSpec<Float>          // DurationExtraLong
    val StaggerFadeSpec: FiniteAnimationSpec<Float>    // DurationMedium
}
```

**Relationships**: Используется всеми presentation-модулями и `core:ui` components через прямой import.

**Validation rules**:
- Все duration > 0
- DurationShort < DurationMedium < DurationLong < DurationExtraLong
- Reduce motion: все duration заменяются на `ReducedDuration` (10ms) через helper

### ReduceMotion (CompositionLocal — core:ui/util/ReduceMotion.kt)

```kotlin
val LocalReduceMotion: ProvidableCompositionLocal<Boolean>

@Composable
fun isReduceMotionEnabled(): Boolean
// Reads: AccessibilityManager (API 33+) or Settings.Global.ANIMATOR_DURATION_SCALE
```

**Relationships**: Предоставляется в `MoneyManagerTheme`, потребляется в `MoneyManagerMotion` helper-функциях.

### CircleRevealShape (refactored — core:ui/components/CircleRevealShape.kt)

Существующая Shape, расширяется параметром `inverted: Boolean`:
```kotlin
class CircleRevealShape(
    private val radius: Float,
    private val inverted: Boolean = false,  // NEW: для snapshot overlay
) : Shape
```

**State transitions**: Нет persistent state — pure UI animation.

## State Diagram: Theme Switch

```
[Settings Screen]
       │
       ▼ onThemeModeChange(mode)
[DataStore write: theme_mode = mode]
       │
       ▼ Flow emission
[MainActivity: themeMode state updates]
       │
       ├─ skipInitial=true → [renderedTheme = themeMode, skip]
       │
       └─ skipInitial=false, themeMode ≠ renderedTheme →
           │
           ▼
       [Record current frame via graphicsLayer.record()]
           │
           ▼
       [isRevealing = true]
           │
           ▼
       [Recompose: MoneyManagerTheme(themeMode=NEW)]
       [Overlay: recorded snapshot with CircleRevealShape(inverted=true)]
           │
           ▼
       [Animate revealRadius: maxRadius → 0 (inverted circle shrinks)]
           │
           ▼
       [isRevealing = false, snapshot = null]
           │
           ▼
       [Normal state: single theme, single NavController]
```

## Affected Files Inventory

| File | Module | Change Type |
|------|--------|-------------|
| `Motion.kt` | core:ui | **NEW** — all tokens |
| `ReduceMotion.kt` | core:ui | **NEW** — accessibility |
| `Theme.kt` | core:ui | EDIT — provide LocalReduceMotion |
| `CircleRevealShape.kt` | core:ui | EDIT — add `inverted` param |
| `MoneyManagerFAB.kt` | core:ui | EDIT — use Motion.InteractionSpring |
| `MoneyManagerButton.kt` | core:ui | EDIT — use Motion.InteractionSpring |
| `MoneyManagerBottomNavBar.kt` | core:ui | EDIT — use Motion.ColorTransitionSpec |
| `BalanceCard.kt` | core:ui | EDIT — use Motion.CounterSpec |
| `IncomeExpenseCard.kt` | core:ui | EDIT — use Motion.CounterSpec |
| `TransactionListItem.kt` | core:ui | EDIT — use Motion.ColorTransitionSpec |
| `MainActivity.kt` | app | EDIT — single NavController, graphicsLayer reveal |
| `MoneyManagerNavHost.kt` | app | EDIT — M3 easing, token durations |
| `TransactionListScreen.kt` | presentation:transactions | EDIT — tokenized animateItem |
| `StatisticsScreen.kt` | presentation:statistics | EDIT — tokenized chart/stagger |
| `SettingsScreen.kt` | presentation:settings | EDIT — tokenized theme selector |
