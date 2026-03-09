# Research: Animation Audit & Motion System

**Branch**: `001-animation-audit` | **Date**: 2026-03-08

## R-001: Material 3 MotionScheme API

**Decision**: Использовать `MaterialTheme.motionScheme` для получения стандартных `FiniteAnimationSpec` и создать собственный объект `MoneyManagerMotion` в `core:ui` для кастомных токенов (durations, stagger delays, chart-specific specs), которые M3 MotionScheme не покрывает.

**Rationale**:
- `MaterialTheme.motionScheme` (доступен с M3 1.3.0+, включён в BOM 2026.01.01) предоставляет `defaultSpatialSpec()`, `fastSpatialSpec()`, `slowSpatialSpec()`, `defaultEffectsSpec()`, `fastEffectsSpec()`, `slowEffectsSpec()` — готовые spring/tween specs для пространственных и effect-анимаций.
- Однако `MotionScheme` не покрывает: конкретные duration-токены, stagger delays, easing для навигации, chart animation durations. Для этого нужен собственный объект.
- M3 easing функции (`EmphasizedDecelerateEasing`, `EmphasizedAccelerateEasing`) доступны из пакета `androidx.compose.material3` — они соответствуют M3 motion guidelines.

**Alternatives considered**:
1. Только `MotionScheme` — недостаточно, не покрывает duration токены и stagger
2. Только кастомный объект без `MotionScheme` — изобретение велосипеда, потеря M3 compliance
3. CompositionLocal для motion — оверкилл, motion tokens статичны (не меняются по иерархии)

## R-002: M3 Easing Functions для навигации

**Decision**: Заменить `LinearOutSlowInEasing`/`FastOutLinearInEasing` на `EmphasizedDecelerateEasing`/`EmphasizedAccelerateEasing` из M3.

**Rationale**:
- Material 3 motion guidelines рекомендуют Emphasized easing для навигационных переходов
- `EmphasizedDecelerateEasing` — для входа элемента (enter) — элемент "прибывает" и замедляется
- `EmphasizedAccelerateEasing` — для выхода элемента (exit) — элемент "уходит" и ускоряется
- Текущие `LinearOutSlowInEasing`/`FastOutLinearInEasing` — это Material 2 easing, менее выразительные
- M3 Emphasized curves имеют более заметное замедление/ускорение, что делает переходы "премиальнее"

**Alternatives considered**:
1. Оставить M2 easing — работает, но не соответствует M3 guidelines
2. Custom cubic bezier — нет причин, M3 предоставляет готовые кривые

## R-003: Навигационные переходы — Tab vs Drill-in

**Decision**: Два типа переходов:
- **Tab switch** (Home, Statistics, AccountList, Settings): `fadeIn`/`fadeOut` с M3 `defaultEffectsSpec`
- **Drill-in** (все остальные): `slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut` с `EmphasizedDecelerateEasing`/`EmphasizedAccelerateEasing`

**Rationale**:
- Material 3 motion guidelines чётко разделяют:
  - **Container transform / Shared axis** — для drill-in навигации (slide + fade)
  - **Fade through** — для переключения между peer destinations (tabs)
- Текущая реализация уже следует этому паттерну (fade для табов, slide+fade для drill-in), нужно только обновить easing и duration на M3 токены

**Alternatives considered**:
1. Одинаковые переходы для всех — нарушает M3 guidelines, плохой UX
2. Shared axis для табов — слишком тяжело, не соответствует peer navigation

## R-004: AnimatedNavHost vs NavHost

**Decision**: Оставить `NavHost` — отдельный `AnimatedNavHost` **не нужен** и **deprecated**.

**Rationale**:
- Начиная с Navigation 2.7.0-alpha01, `NavHost` напрямую поддерживает `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition`
- `AnimatedNavHost` был из `accompanist-navigation-animation` и полностью merged в стандартный `NavHost`
- Проект уже использует Navigation 2.9.7 — все animation APIs доступны в `NavHost`

**Alternatives considered**:
1. Миграция на `AnimatedNavHost` — он deprecated и merged в NavHost

## R-005: Баг смены темы — Root Cause Analysis

**Decision**: Переписать circular reveal на **один NavController** + bitmap snapshot подход вместо двух NavController.

**Rationale — Root Cause**:
Текущая реализация (`MainActivity.kt:124-176`) использует **два независимых NavController** (`topNavController`, `bottomNavController`):
- Во время reveal анимации (500ms) отрисовываются два полных `MoneyManagerApp`:
  - Bottom layer: `bottomNavController` с `renderedTheme` (старая тема)
  - Top layer: `topNavController` с `themeMode` (новая тема), clip по `CircleRevealShape`
- **Проблема**: `bottomNavController` создан через `rememberNavController()` и начинает с `startDestination` — это **Home**. Если пользователь находится на Settings и переключает тему, bottom layer показывает Home (start destination), а не Settings.
- Это объясняет баг: на долю секунды виден "контент другого экрана".

**Решение — Bitmap snapshot**:
1. При смене темы: сделать snapshot текущего ComposeView как Bitmap
2. Показать snapshot как Image поверх основного контента
3. Анимировать CircleReveal на snapshot (сверху → clip уменьшается, открывая новую тему снизу)
4. По завершении: убрать snapshot
5. Один NavController, одна навигационная стопка — нет рассинхрона

**Alternatives considered**:
1. Синхронизация двух NavController — сложно, race conditions, двойная подписка на backStack
2. `View.drawToBitmap()` в ComposeView — работает, но нужен доступ к View из Compose
3. `Modifier.drawWithContent` + `graphicsLayer` — capture layer в `Picture` → `Bitmap`
4. `rememberGraphicsLayer()` + `drawWithContent { record() }` — новый API Compose 1.7+, чистый Compose подход ✅

## R-006: Reduce Motion / Accessibility

**Decision**: Читать системную настройку `Settings.Global.ANIMATOR_DURATION_SCALE` и/или `AccessibilityManager.isReduceMotionEnabled` (API 33+). При включённом Reduce Motion — сократить все duration до 0-50ms.

**Rationale**:
- Android с API 33 поддерживает `AccessibilityManager.isReduceMotionEnabled`
- Для более ранних API: `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` — стандартный способ
- M3 guidelines: тип перехода сохраняется (slide остаётся slide), но duration минимальный
- Реализация через `CompositionLocal<Boolean>` (`LocalReduceMotion`) — все токены автоматически сокращаются

**Alternatives considered**:
1. Полное отключение анимаций (`EnterTransition.None`) — нарушает навигационный контекст
2. Только `ANIMATOR_DURATION_SCALE` — не покрывает API 33+ reduce motion
3. Per-component проверка — дублирование кода, не масштабируется

## R-007: Circular Reveal без двух NavController — подход `rememberGraphicsLayer`

**Decision**: Использовать `Modifier.drawWithContent` + `graphicsLayer.record()` для захвата старой темы как записанного слоя, затем анимировать reveal.

**Rationale**:
- `rememberGraphicsLayer()` (Compose 1.7+, доступен в BOM 2026.01.01) позволяет записать composable в `GraphicsLayer`
- При смене темы: записываем текущий кадр через `record { drawContent() }`, затем рисуем этот слой поверх нового контента с clip `CircleRevealShape`
- Нулевой overhead: один NavController, одна навигационная стопка, одна подписка на state

**Implementation sketch**:
```kotlin
val graphicsLayer = rememberGraphicsLayer()
var snapshot by remember { mutableStateOf<GraphicsLayer?>(null) }

// Main content — always renders with current theme
MoneyManagerTheme(themeMode = themeMode) {
    Box(
        modifier = Modifier.drawWithContent {
            if (!isRevealing) {
                graphicsLayer.record(size) { this@drawWithContent.drawContent() }
            }
            drawContent()
        }
    ) { MoneyManagerApp(...) }
}

// Overlay: old theme snapshot with shrinking clip
if (isRevealing) {
    Box(modifier = Modifier
        .clip(CircleRevealShape(revealRadius.value, inverted = true))
        .drawWithCache { onDrawWithContent { drawLayer(snapshot!!) } }
    )
}
```

**Alternatives considered**:
1. `Picture` API (Android Canvas) — works but requires Android View interop
2. Two NavControllers (current) — root cause of the bug, must be replaced
3. `Modifier.captureToImage()` — testing API, not production-ready

## R-008: Duration Tokens — конкретные значения

**Decision**: Следующие duration-токены:

| Token | Value | Usage |
|-------|-------|-------|
| `DurationShort` | 150ms | Bottom bar, quick feedback, item fade |
| `DurationMedium` | 300ms | Nav transitions, color transitions, stagger fade |
| `DurationLong` | 500ms | Theme reveal, bar chart |
| `DurationExtraLong` | 800ms | Donut chart, balance counter |
| `StaggerDelay` | 50ms | Staggered list items |
| `ReducedDuration` | 10ms | Reduce motion fallback |

**Rationale**:
- M3 motion token mapping: Short1=50, Short2=100, Short3=150, Short4=200, Medium1=250, Medium2=300, Medium3=350, Medium4=400, Long1=450, Long2=500
- Наши токены маппятся на M3: Short→Short3, Medium→Medium2, Long→Long2
- ExtraLong (800ms) — кастомный для chart анимаций, M3 не имеет прямого аналога
- Stagger delay 50ms — рекомендация M3 для staggered animations

**Alternatives considered**:
1. Использовать M3 token names напрямую (Short1, Long2) — менее понятно в контексте приложения
2. Больше градаций (7+ токенов) — оверкилл для 45 анимаций
