# Feature Specification: Onboarding Screens Redesign

**Feature Branch**: `012-onboarding-redesign`
**Created**: 2026-03-21
**Status**: Draft
**Input**: User description: "Redesign the onboarding screens (presentation/onboarding) to match the Pencil design with Outfit font, custom colors, and updated layout"

## Design Reference

- **File**: `~/Documents/pencil/money_manager_ds/money_manager_screens.pen`
- **Page 1 (Welcome)**: node `vq5yF`
- **Page 2 (Track Finances)**: node `FuS5O`
- **Page 3 (Analyze Spending)**: node `LXNau`
- Use pencil MCP `get_screenshot(nodeId)` and `batch_get(nodeIds)` during implementation to verify visually.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - First-Time User Sees Redesigned Onboarding (Priority: P1)

A new user launches the app for the first time and sees a polished, branded onboarding flow with the Outfit font, green accent colors, circular icon containers, and a warm off-white background. The user can swipe through 3 pages, tap "Далее" to advance, or "Пропустить" to skip. On the last page, the button reads "Начать" and skip is hidden.

**Why this priority**: This is the entire feature — the visual redesign of the onboarding experience. Without this, there is no deliverable.

**Independent Test**: Open the app after clearing data, observe all 3 onboarding pages render with the new design (Outfit font, green colors, circular icons, off-white background).

**Acceptance Scenarios**:

1. **Given** a fresh install, **When** user launches the app, **Then** onboarding page 1 displays with off-white background (#F5F4F1), a wallet icon inside a 160dp green-tinted circle, title in Outfit SemiBold, and description in Outfit Regular.
2. **Given** user is on page 1, **When** user taps "Далее", **Then** pager animates to page 2.
3. **Given** user is on page 1 or 2, **When** user swipes left, **Then** pager advances to the next page.
4. **Given** user is on any non-last page, **When** user taps "Пропустить", **Then** onboarding completes and navigates to account creation.
5. **Given** user is on page 3 (last), **Then** button text reads "Начать", skip text is hidden, and tapping "Начать" completes onboarding.
6. **Given** user is on any page, **Then** page indicator dots show the correct active page (green dot) and inactive pages (gray dots).

---

### User Story 2 - Outfit Font Available for Onboarding (Priority: P1)

The Outfit font family (Regular, Medium, SemiBold weights) is added to the app's font resources and exposed as `OutfitFontFamily` in Type.kt. The global app typography remains unchanged — Outfit is used only within the onboarding screens.

**Why this priority**: The font is a prerequisite for the visual redesign. Without it, text styling cannot match the design.

**Independent Test**: Verify that `OutfitFontFamily` is defined in Type.kt with 3 weights, font files exist in `core/ui/src/main/res/font/`, and the global `MoneyManagerTypography` is not modified.

**Acceptance Scenarios**:

1. **Given** the font resources are added, **When** building the app, **Then** `outfit_regular.ttf`, `outfit_medium.ttf`, and `outfit_semibold.ttf` exist in `core/ui/src/main/res/font/`.
2. **Given** `OutfitFontFamily` is defined, **When** used in Compose, **Then** it provides Regular (W400), Medium (W500), and SemiBold (W600) font weights.
3. **Given** the change is made, **When** inspecting global typography, **Then** `MoneyManagerTypography` remains unchanged.

---

### Edge Cases

- What happens on very small screens (320dp width)? Content should still be readable — description has max width 300dp constraint, buttons fill available width with horizontal padding.
- What happens if the user rapidly swipes through pages? The page indicator and button text ("Далее"/"Начать") must stay in sync with the current page.
- What happens with right-to-left (RTL) layouts? The pager swipe direction should respect system RTL settings (handled automatically by HorizontalPager).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display onboarding screens with background color #F5F4F1.
- **FR-002**: System MUST render page icons at 64dp inside a 160dp circular container with green-light background (#3D8A5A at 7% opacity) and green icon tint (#3D8A5A).
- **FR-003**: System MUST render page titles in Outfit SemiBold, 26sp, letterSpacing -0.5sp, color #1A1918.
- **FR-004**: System MUST render page descriptions in Outfit Regular, 15sp, lineHeight 1.5x, color #6D6C6A, max width 300dp, centered.
- **FR-005**: System MUST render the main action button with green fill (#3D8A5A), rounded corners (14dp radius), 52dp height, and Outfit Medium 16sp white text.
- **FR-006**: System MUST render the skip action as plain text in Outfit Medium 14sp, color #9C9B99, without any button styling.
- **FR-007**: System MUST display page indicator dots at 8dp diameter with 8dp spacing; active dot colored #3D8A5A, inactive dots colored #D4D3D0.
- **FR-008**: System MUST preserve all existing testTags: `onboarding:screen`, `onboarding:pager`, `onboarding:indicator`, `onboarding:nextButton`, `onboarding:skipButton`.
- **FR-009**: System MUST maintain existing HorizontalPager swipe navigation between 3 pages.
- **FR-010**: System MUST show "Далее" on non-last pages and "Начать" on the last page as the main button text.
- **FR-011**: System MUST hide the skip text on the last page.
- **FR-012**: Layout MUST use 24dp horizontal padding, 40dp bottom padding, 32dp gap between content sections, 12dp gap between title and description, and 12dp gap between button and skip text.
- **FR-013**: Outfit font files MUST be added to `core/ui` resources without modifying global app typography.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 3 onboarding pages visually match the Pencil design reference (verified by screenshot comparison against nodes vq5yF, FuS5O, LXNau).
- **SC-002**: All existing UI test tags remain functional and discoverable by test automation.
- **SC-003**: Onboarding flow completion (swipe or tap through all pages, tap "Начать") works identically to the current implementation.
- **SC-004**: No visual regressions outside the onboarding screens — global typography, colors, and themes are unaffected.
- **SC-005**: App builds successfully and onboarding renders correctly on screen widths from 320dp to 420dp.

## Assumptions

- Outfit font .ttf files (Regular, Medium, SemiBold) will be sourced from Google Fonts and are freely licensed (OFL).
- The onboarding-specific colors are defined as local constants within OnboardingScreen.kt, not added to the global theme.
- The existing `OnboardingState`, `OnboardingViewModel`, and navigation logic (Route/ViewModel) are not modified — only the UI composables change.
- The hex value `#3D8A5A12` for GreenLight means #3D8A5A at ~7% opacity (the "12" suffix is alpha in hex = 18/255 ≈ 7%).
