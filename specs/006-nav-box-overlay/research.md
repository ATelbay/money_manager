# Research: Navigation Box-Overlay Refactor

**Date**: 2026-03-12 | **Branch**: `006-nav-box-overlay`

## R1: Bottom bar padding delivery mechanism

**Decision**: Explicit `bottomBarPadding: Dp` parameter passed from MoneyManagerApp → MoneyManagerNavHost → top-level Route composables via `modifier`.

**Rationale**: All 4 top-level Routes (TransactionListRoute, StatisticsRoute, AccountListRoute, SettingsRoute) already accept `modifier: Modifier = Modifier`. Adding `Modifier.padding(bottom = bottomBarPadding)` is zero-cost and follows Compose's modifier-based layout model. No new abstractions needed.

**Alternatives considered**:
- `CompositionLocal`: Unnecessary indirection for a single value consumed by 4 call sites. CompositionLocals are better suited for cross-cutting concerns (theme, strings), not layout measurements.
- `onGloballyPositioned` runtime measurement: Introduces a frame delay — the first frame renders without padding, then corrects. This is the exact layout shift problem we're solving.
- `SubcomposeLayout`: Overkill. Measuring one child to constrain another is powerful but unnecessary when the height is known at compile time.

## R2: Material 3 NavigationBar height

**Decision**: Use 80.dp as the fixed bottom bar height constant.

**Rationale**: Material 3's `NavigationBar` has a fixed height defined by the spec (80.dp). This is the `NavigationBarTokens.ContainerHeight` value in the Compose Material 3 source. Using a constant avoids measurement overhead.

**Risk**: If Material 3 changes the NavigationBar height in a future version, the constant would need updating. Mitigated by: (a) this value has been stable since M3 launch, (b) a version bump would be caught in visual review.

**Alternatives considered**:
- Dynamic measurement: Adds complexity and frame delay.
- Reading `NavigationBarDefaults` token: The token is internal to the M3 library and not part of the public API.

## R3: Z-order and touch event behavior

**Decision**: Bottom bar is always composed (when `showBottomBar = true`) and sits above the NavHost in z-order. Detail screens cover it via opaque backgrounds.

**Rationale**: Box children are rendered in declaration order — NavHost first, then bottom bar on top. When a top-level screen is showing, the bottom bar overlays the content area (with padding preventing occlusion). When a detail screen is showing, the NavHost transition renders the detail screen with an opaque background that visually covers the bottom bar.

**Touch behavior**: The bottom bar remains in the composition tree during detail screens. However, since detail screen content fills the entire NavHost (which is the first Box child), and the bottom bar is the second child (drawn on top), touch events go to the bottom bar first. But the detail screen content in the NavHost fills the whole screen, covering the bar visually.

**Resolution**: Since Box z-order means the bottom bar receives touches on top, and detail screens are in the NavHost (drawn first/behind the bar), we need the bottom bar to NOT compose when detail screens are active. This is already handled by `showBottomBar` = `currentTopLevel != null`. When on a detail screen, `currentTopLevel` is null, so `showBottomBar` is false and the bar is not composed. No touch-through issue exists.

## R4: Circle-reveal theme animation compatibility

**Decision**: No changes needed — the circle-reveal animation operates on a layer snapshot that is independent of the Scaffold/Box choice.

**Rationale**: The theme animation in MainActivity uses `rememberGraphicsLayer()` + `drawWithContent` to snapshot the current theme, then overlays a `CircleRevealShape` mask. This mechanism operates on the rendered pixel output, not the composition structure. Replacing Scaffold with Box does not affect the snapshot/reveal mechanism because:
1. The `MoneyManagerApp` composable is wrapped in `Box(Modifier.drawWithContent { ... })` — this Box remains unchanged.
2. The reveal overlay is a sibling Box at the same level — also unchanged.

## R5: SharedTransitionLayout compatibility

**Decision**: No changes needed — SharedTransitionLayout wraps the NavHost content, which is unaffected by the parent layout change.

**Rationale**: `SharedTransitionLayout` is inside `MoneyManagerNavHost` and wraps the `NavHost` composable. Whether the parent is a `Scaffold` or a `Box`, the SharedTransitionLayout and its CompositionLocals (`LocalSharedTransitionScope`, `LocalAnimatedVisibilityScope`) operate identically. The shared element transitions depend on NavHost's `AnimatedContentScope`, not the parent layout.
