# Feature Specification: Navigation Box-Overlay Refactor

**Feature Branch**: `006-nav-box-overlay`
**Created**: 2026-03-12
**Status**: Draft
**Input**: User description: "Рефакторинг навигации: переход от Scaffold с AnimatedVisibility bottom bar к Box-overlay архитектуре"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Seamless navigation without layout shifts (Priority: P1)

As a user, when I navigate between top-level tabs (Home, Statistics, Accounts, Settings) and detail screens (TransactionEdit, CategoryEdit, AccountEdit, Import, CategoryList, CurrencyPicker, SignIn), the layout remains stable — no visible content jumps or flickering caused by the bottom bar appearing/disappearing.

**Why this priority**: Layout shifts are the core problem this refactor solves. The current Scaffold + AnimatedVisibility approach causes visible content displacement during navigation transitions.

**Independent Test**: Navigate between all screen pairs (tab→detail, detail→tab, tab→tab) and verify zero layout shifts occur during transitions.

**Acceptance Scenarios**:

1. **Given** I am on the Home screen (bottom bar visible), **When** I tap a transaction to open TransactionEdit, **Then** the detail screen renders full-screen over the bottom bar without any content jump or resize.
2. **Given** I am on TransactionEdit (detail screen), **When** I press back to return to Home, **Then** the Home screen appears with the bottom bar visible, and no layout shift occurs.
3. **Given** I am on the Statistics tab, **When** I switch to the Accounts tab via bottom bar, **Then** the tab transition plays smoothly and the bottom bar stays stationary throughout.
4. **Given** I am on a detail screen (e.g., Import), **When** the screen renders, **Then** the content fills the entire screen area including the space behind the bottom bar.

---

### User Story 2 - Bottom bar always visible on top-level screens (Priority: P1)

As a user, the bottom bar is always visible and interactive on top-level screens, sitting as an overlay at the bottom of the screen without any enter/exit animation.

**Why this priority**: The bottom bar is the primary navigation mechanism; it must work reliably without animation glitches.

**Independent Test**: Visit each top-level destination and verify the bottom bar is visible, correctly highlights the active tab, and responds to taps.

**Acceptance Scenarios**:

1. **Given** I am on any top-level screen (Home, Statistics, AccountList, Settings), **When** the screen is rendered, **Then** the bottom bar is visible at the bottom and the content above it is not obscured by the bar.
2. **Given** I am on the Home tab, **When** I tap the Settings icon in the bottom bar, **Then** the app navigates to Settings with the tab transition and the bottom bar stays in place (no slide/fade animation on the bar itself).

---

### User Story 3 - Detail screens cover the bottom bar (Priority: P2)

As a user, when I open a detail screen, the screen content covers the entire display including the bottom bar area.

**Why this priority**: Detail screens need full-screen real estate. The overlay architecture naturally hides the bottom bar by rendering detail content on top.

**Independent Test**: Open each detail screen and verify the bottom bar is not visible and not intercepting touch events.

**Acceptance Scenarios**:

1. **Given** I navigate to any detail screen (TransactionEdit, CategoryEdit, AccountEdit, Import, CategoryList, CurrencyPicker, SignIn), **When** the screen is rendered, **Then** the bottom bar is not visible — the detail screen covers it.
2. **Given** I am on a detail screen, **When** I interact with the bottom area of the screen, **Then** only the detail screen content responds (not the hidden bottom bar).

---

### User Story 4 - FAB navigation without delays (Priority: P2)

As a user, when I tap the FAB (add transaction) on the Home screen, navigation to TransactionEdit happens immediately without artificial delay.

**Why this priority**: The current workaround uses forceHideBottomBar + pendingNavAction + delay, which is fragile and adds latency. The overlay approach eliminates this hack.

**Independent Test**: Tap FAB on Home screen and verify navigation happens immediately.

**Acceptance Scenarios**:

1. **Given** I am on the Home screen, **When** I tap the FAB, **Then** I navigate to TransactionEdit immediately without any intermediate bottom bar animation or delay.

---

### Edge Cases

- Onboarding flow: bottom bar must not appear during Onboarding and CreateAccount screens.
- System insets: bottom bar padding must account for devices with gesture navigation bars.
- Theme switch: the circle-reveal theme animation must continue to work correctly with the Box overlay layout.
- Shared element transitions (TransactionEdit) must function when NavHost is no longer inside a Scaffold.
- Rapid tab switching: the overlay must remain stable when the user rapidly taps between bottom bar items.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST replace the Scaffold layout with a Box layout containing the NavHost and bottom bar as sibling layers.
- **FR-002**: The bottom bar MUST be rendered as an overlay aligned to bottom-center of the Box, on top of the NavHost content.
- **FR-003**: Top-level screens (Home, Statistics, AccountList, Settings) MUST apply bottom padding equal to the bottom bar height so their scrollable content is not obscured by the bar.
- **FR-004**: Detail screens (TransactionEdit, CategoryEdit, AccountEdit, Import, CategoryList, CurrencyPicker, SignIn) MUST render full-screen without extra bottom padding, visually covering the bottom bar.
- **FR-005**: The AnimatedVisibility wrapper around the bottom bar MUST be removed — the bar is always present in the layout on top-level screens and naturally covered on detail screens.
- **FR-006**: The forceHideBottomBar state, pendingNavAction delayed navigation, and animateDpAsState bottom padding animation MUST be removed from MoneyManagerApp.
- **FR-007**: The FAB on the Home screen MUST navigate directly to TransactionEdit without delay or intermediate animation state.
- **FR-008**: Screen transition animations (drillIn for detail screens, tabEnter/tabExit for tabs) MUST remain unchanged.
- **FR-009**: SharedTransitionLayout and shared element transitions MUST continue to function correctly.
- **FR-010**: The bottom bar MUST not be visible during the onboarding flow (Onboarding and CreateAccount screens).
- **FR-011**: The circle-reveal theme transition MUST continue to work with the new Box layout.

### Key Entities

- **TopLevelDestination**: Enum of tab destinations (HOME, STATISTICS, ACCOUNTS, SETTINGS) — determines whether the current route is a top-level screen that needs bottom bar padding.
- **Bottom Bar Padding**: A fixed value representing the NavigationBar height, applied as bottom content padding to top-level screen composables.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero layout shifts occur during any navigation transition (tab↔tab, tab→detail, detail→tab).
- **SC-002**: Navigation from FAB tap to detail screen happens with no artificial delay (eliminates the previous ~260ms workaround).
- **SC-003**: All existing screen transition animations (drill-in, tab enter/exit, shared elements) play identically to pre-refactor behavior.
- **SC-004**: At least 3 workaround mechanisms are removed: forceHideBottomBar state, pendingNavAction delayed navigation, animateDpAsState bottom padding animation.
- **SC-005**: The MoneyManagerApp composable has fewer state variables and no LaunchedEffect for delayed navigation.
- **SC-006**: All existing UI tests referencing bottomBar test tags continue to pass without modification.
