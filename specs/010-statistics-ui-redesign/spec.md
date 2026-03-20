# Feature Specification: Statistics Screen UI Redesign

**Feature Branch**: `010-statistics-ui-redesign`
**Created**: 2026-03-17
**Status**: Draft
**Input**: User description: "Align StatisticsScreen with Money Manager design system — UI-only refactor, no domain/data changes"

## Clarifications

### Session 2026-03-17

- Q: "See all" category behavior — navigate to full-screen list or expand inline? → A: Expand the card inline to show all categories (no navigation away from Statistics screen).
- Q: Calendar picker granularity — adaptive per tab, always month, or full date range? → A: Always a month picker regardless of active period tab — selected month becomes the anchor for any period.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Unified Chart Card with Expense/Income Toggle (Priority: P1)

A user opens the Statistics tab and sees a single, cohesive chart card containing a bar chart, expense/income toggle pills, and total amount. Instead of two separate summary cards (expenses and income), the user taps small toggle pills within the chart card header to switch between expense and income views. The total row at the bottom of the card shows the aggregate for the selected type.

**Why this priority**: This is the most significant structural change — it consolidates the chart and summary into one card, replacing the current two-card layout. All other changes are incremental styling updates.

**Independent Test**: Can be tested by switching between Expense/Income pills and verifying the chart, title, subtitle, and total row update correctly for each type.

**Acceptance Scenarios**:

1. **Given** user is on the Statistics screen with transaction data, **When** they view the chart card, **Then** they see a single card with: chart header (title + date range + toggle pills), bar chart, and total row at the bottom.
2. **Given** the chart card is visible with "Expenses" selected, **When** user taps the "Income" pill, **Then** the chart title changes to "Income", the bar chart updates to income data, and the total row shows total income amount.
3. **Given** the chart card is visible, **When** viewing the total row, **Then** the label reads "Total expenses" or "Total income" (matching the active pill) and the amount is formatted according to currency settings.
4. **Given** the period is MONTH (scrollable chart), **When** the user scrolls the bar chart horizontally, **Then** a scroll indicator below the chart reflects the current scroll position proportionally.

---

### User Story 2 - Segmented Period Selector (Priority: P1)

A user sees the period selector (Week | Month | Year) as a polished segmented control instead of individual chips. The active tab is highlighted with a filled background and subtle shadow, while inactive tabs appear muted.

**Why this priority**: The period selector is prominently placed and used on every visit — its visual quality sets the tone for the entire screen.

**Independent Test**: Can be tested by tapping each period tab and verifying visual state changes (active highlight, text weight) and that the data reloads for the selected period.

**Acceptance Scenarios**:

1. **Given** user is on the Statistics screen, **When** they view the period selector, **Then** they see a segmented control with three equal-width tabs: "Week", "Month", "Year".
2. **Given** "Month" is the active tab, **When** user taps "Week", **Then** "Week" becomes visually active (filled background, semibold text, shadow) and "Month" becomes inactive (transparent, muted text).
3. **Given** the app is in dark mode, **When** viewing the period selector, **Then** the container uses the dark surface color and active tab uses the dark active color with appropriate contrast.

---

### User Story 3 - Compact Donut Chart with Side Legend (Priority: P2)

A user views the "By Category" section and sees a compact card with the donut chart on the left and a legend of the top categories on the right. A "See all" link in the section header lets them access the full category breakdown.

**Why this priority**: This restructures the category section for better space efficiency but doesn't change functionality — the donut chart and category data remain the same.

**Independent Test**: Can be tested by verifying the donut chart + legend layout renders correctly with categories, and that "See all" expands inline to show the full category list.

**Acceptance Scenarios**:

1. **Given** user has categorized transactions, **When** viewing the "By Category" section, **Then** they see a section header ("By Category" + "See all" link) and a card with a donut chart on the left and a vertical legend on the right.
2. **Given** there are more than 3 categories, **When** viewing the legend, **Then** only the top 3 categories are shown individually, with a 4th "Other" entry aggregating the remaining categories.
3. **Given** the "By Category" section is visible, **When** user taps "See all", **Then** the card expands inline to show all categories (no navigation away from the Statistics screen). Tapping "See all" again (or a "Show less" toggle) collapses back to top 3 + "Other".
4. **Given** a category is shown in the legend, **When** viewing its row, **Then** it displays a colored dot matching the donut segment, the category name, and its percentage.

---

### User Story 4 - Calendar Filter Button in Header (Priority: P2)

A user sees a calendar filter pill in the screen header next to the "Statistics" title. Tapping it opens a date/month picker, allowing the user to override the default period range.

**Why this priority**: Adds new interactive functionality (date filtering) but is secondary to the core layout restructuring.

**Independent Test**: Can be tested by tapping the calendar pill, selecting a date range, and verifying the statistics update for the custom range.

**Acceptance Scenarios**:

1. **Given** user is on the Statistics screen, **When** they view the header, **Then** they see "Statistics" title on the left and a rounded calendar pill on the right showing the current period label (e.g., "Mar 2026").
2. **Given** the calendar pill is visible, **When** user taps it, **Then** a month picker dialog appears.
3. **Given** the month picker is open, **When** user selects a month, **Then** the statistics refresh using that month as the anchor for the active period tab, and the pill label updates to reflect the chosen month (e.g., "Jan 2026").
4. **Given** a custom date range is active, **When** the user switches period tabs (Week/Month/Year), **Then** the custom range is cleared and the tab's default range calculation resumes.

---

### User Story 5 - Dark Mode Design Token Alignment (Priority: P3)

A user in dark mode sees the Statistics screen with consistent, design-system-compliant colors: correct card surface, text, border, shadow, and accent colors matching the established token palette.

**Why this priority**: Visual polish — important for consistency but doesn't change layout or behavior.

**Independent Test**: Can be tested by switching to dark mode and visually verifying each element's color matches the design token spec.

**Acceptance Scenarios**:

1. **Given** the app is in dark mode, **When** viewing the Statistics screen, **Then** card backgrounds use the dark card surface token, primary text uses the dark primary token, muted text uses the muted token, and dividers use the dark divider token.
2. **Given** the app is in dark mode, **When** viewing green accent elements (e.g., "See all" link), **Then** the dark-mode green accent is used (not the light-mode green).
3. **Given** the app is in light mode, **When** viewing the Statistics screen, **Then** cards use white backgrounds, the screen background uses the light background token, and the green accent uses the light-mode green.

---

### Edge Cases

- What happens when there are zero categories (empty state)? The donut chart card and category section should not render; existing empty state behavior is preserved.
- What happens when all transactions are in one category? The donut shows a single full-circle segment; the legend shows only that one category (no "Other").
- What happens when the bar chart has no data points for the selected type? The chart area shows the existing "unavailable" placeholder within the new card structure.
- What happens when the user selects a custom date range via the calendar pill and then rotates the device? The custom range survives configuration changes (preserved in ViewModel state).
- What happens when category names are very long in the compact legend? Text is truncated with ellipsis to fit the available space.
- What happens when the scroll indicator is shown but the chart is not scrollable? The scroll indicator only appears when the chart content exceeds the visible area.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The screen MUST display a segmented control for period selection (Week | Month | Year) instead of individual chip buttons, with the active tab visually distinguished by filled background and shadow.
- **FR-002**: The bar chart, expense/income toggle, and total amount MUST be consolidated into a single elevated card with toggle pills in the card header instead of separate summary stat cards.
- **FR-003**: The expense/income toggle pills MUST switch the chart data, chart title, and total row between expense and income views using the existing transaction type change callback.
- **FR-004**: The chart card MUST include a scroll indicator (track + thumb) below the bar chart when the chart is scrollable, reflecting the current scroll position.
- **FR-005**: The "By Category" section MUST display the donut chart on the left and a vertical legend (top 3 categories + "Other" aggregation) on the right within a single card.
- **FR-006**: The category section header MUST include a "See all" link that expands the card inline to show all categories. A collapse action ("Show less") MUST return the card to the compact top-3 + "Other" view.
- **FR-007**: The screen header MUST include a calendar filter pill that shows the current month label and opens a month picker on tap (always month granularity regardless of active period tab).
- **FR-008**: When a custom month is selected via the month picker, the statistics MUST refresh using that month as the anchor for the active period, overriding the default "current" calculation.
- **FR-009**: All existing test tag values MUST be preserved. New UI elements MUST receive appropriate test tags.
- **FR-010**: All colors MUST use the design token palette for both light and dark modes as specified in the design system.
- **FR-011**: No breaking changes MUST be made to domain interfaces, data layer, chart model production, multi-currency resolution, or navigation routes. Backward-compatible parameter additions (optional params with null defaults) are permitted.

### Key Entities *(no new entities — existing only)*

- **StatisticsState**: Existing screen state — may gain a custom date range field for the calendar filter feature.
- **StatisticsChartState**: Existing chart metadata (title, dateRangeLabel, points, isScrollable) — unchanged.
- **StatisticsCategoryDisplayItem**: Existing category display model (category, displayAmount, displayPercentage) — unchanged.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 5 visual sections (header, period selector, chart card, category section, color tokens) match the design system specification when compared side-by-side.
- **SC-002**: Users can switch between expense and income views with a single tap on toggle pills within the chart card, maintaining equivalent or better interaction speed.
- **SC-003**: The category section displays a compact donut + legend layout that fits within a single viewport height without scrolling to see the donut and top categories.
- **SC-004**: All existing UI test tags remain functional and pass existing automated tests without modification.
- **SC-005**: Dark mode and light mode each render with correct design tokens — zero hardcoded color values outside the token palette.
- **SC-006**: The calendar filter pill allows users to select a custom date range and see updated statistics within the standard data-loading time.
- **SC-007**: The screen renders correctly on devices from 360dp to 412dp width without layout overflow or truncation of critical content.

## Assumptions

- The calendar filter feature requires a minor ViewModel addition (custom date range state + setter), but no domain/use-case changes — the existing period summary use case already accepts date range parameters.
- The "See all" expands the category card inline (no new navigation routes needed). The expanded view reuses existing category list rendering.
- The chart library's scroll state is accessible for driving the custom scroll indicator position.
- Font styling uses the system default sans-serif (no custom Outfit font import required).
- The scroll indicator is only needed when the chart content is horizontally scrollable.
