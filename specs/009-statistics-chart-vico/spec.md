# Feature Specification: Replace Bar Chart with Vico Library

**Feature Branch**: `009-statistics-chart-vico`
**Created**: 2026-03-16
**Status**: Draft
**Input**: Replace custom Canvas bar chart with Vico 2.x CartesianChartHost in Statistics screen

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Currency-Formatted Y-Axis Labels (Priority: P1)

A user opens the Statistics screen to review their spending. The bar chart displays clear, currency-formatted labels on the vertical axis (e.g., "50K ₸", "200K $"), so they can immediately understand the scale of their expenses or income without mental calculation.

**Why this priority**: The current chart has no meaningful Y-axis labels — users cannot gauge absolute amounts at a glance. This is the core readability problem driving the replacement.

**Independent Test**: Can be verified by opening the Statistics screen with transaction data and confirming the Y-axis shows formatted values in the user's base currency.

**Acceptance Scenarios**:

1. **Given** a user with base currency ₸ and daily expenses ranging 0–200,000, **When** they view the Week chart, **Then** the Y-axis displays 4–5 evenly spaced labels formatted as abbreviated currency values (e.g., "0", "50K", "100K", "150K", "200K") with the currency symbol.
2. **Given** a user with base currency $ and small daily expenses (0–50), **When** they view the chart, **Then** Y-axis labels show whole numbers without abbreviation (e.g., "$0", "$10", "$25", "$50").
3. **Given** a user with mixed-currency accounts and no base currency conversion available, **When** they view the chart, **Then** the chart displays the existing "unavailable" overlay message instead of the bar chart.

---

### User Story 2 - Scrollable Month View with 30+ Daily Bars (Priority: P1)

A user viewing the Month period sees up to 31 daily bars. The chart scrolls horizontally with natural fling physics, starting at the most recent day (right edge), so they can browse the entire month without cramped, unreadable bars.

**Why this priority**: The current Month view crams 30 bars into fixed width, making individual days indistinguishable. Horizontal scroll with fling is the primary UX improvement.

**Independent Test**: Can be verified by selecting "Month" period and swiping left/right through the chart, confirming smooth scroll and that the chart initially shows the most recent days.

**Acceptance Scenarios**:

1. **Given** the period is set to Month with 30 daily data points, **When** the chart loads, **Then** it scrolls to show the most recent days (right edge visible first).
2. **Given** the Month chart is displayed, **When** the user swipes left, **Then** the chart scrolls smoothly with fling momentum to reveal earlier days.
3. **Given** the period is set to Week (7 bars) or Year (12 bars), **When** the chart loads, **Then** all bars fit within the visible area without scrolling, scaled to fill the available width.

---

### User Story 3 - Tap Tooltip with Exact Amount and Date (Priority: P2)

A user taps or long-presses on a bar to see a floating tooltip showing the exact monetary amount and the date for that bar. This provides precise data inspection without cluttering the default view.

**Why this priority**: The current chart has no touch interaction. While users can read approximate values from bar heights and Y-axis labels (P1), exact values require a tooltip for precision.

**Independent Test**: Can be verified by tapping any bar and confirming a tooltip appears with the correct formatted amount and date.

**Acceptance Scenarios**:

1. **Given** the chart displays bars with data, **When** the user taps on a bar for day "Mar 23", **Then** a tooltip appears above the bar showing the exact amount (e.g., "153,200 ₸") and date ("Mar 23").
2. **Given** a tooltip is showing, **When** the user taps elsewhere or scrolls, **Then** the tooltip dismisses.
3. **Given** the chart is in dark theme, **When** the user taps a bar, **Then** the tooltip has a light background with dark text (inverted from light theme's dark tooltip with white text).

---

### User Story 4 - "Today" Bar Visual Highlight (Priority: P3)

The bar representing the current day (or current month in Year view) stands out visually from other bars. It renders at full opacity while other bars are slightly muted, and a small accent-colored dot appears above it to draw the user's eye.

**Why this priority**: Helpful for quick orientation but not blocking core chart readability.

**Independent Test**: Can be verified by checking that one bar visually differs from others and corresponds to the current date.

**Acceptance Scenarios**:

1. **Given** today's date falls within the displayed period, **When** the chart renders, **Then** today's bar is at full opacity and other bars at reduced (~0.7) opacity.
2. **Given** today's date falls within the displayed period, **When** the chart renders, **Then** a small accent-colored dot indicator appears above the today bar.
3. **Given** today's date is outside the displayed period (e.g., viewing a past month), **When** the chart renders, **Then** all bars display at uniform reduced opacity with no dot indicator.

---

### Edge Cases

- What happens when all daily amounts are zero? The chart should render flat (zero-height) bars with Y-axis labels showing "0" at the top.
- What happens when there is only 1 day of data in a month? A single bar should render with proper Y-axis scaling, not stretched to fill the entire width.
- What happens when amounts are extremely large (e.g., 999,999,999)? Y-axis labels should abbreviate appropriately (e.g., "1B ₸" or "999M ₸").
- What happens on screen rotation or window resize? The chart should recompose to fill available width, maintaining scroll position in Month view.
- What happens when the user rapidly toggles between Expenses/Income? The chart should animate the data transition smoothly without flickering.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The bar chart MUST display vertical bars with rounded top corners, replacing the current custom-drawn rectangles.
- **FR-002**: The vertical axis MUST show currency-formatted labels using the user's selected base currency symbol (₸, $, €, ₽, etc.) with appropriate abbreviation (K for thousands, M for millions).
- **FR-003**: The horizontal axis MUST show contextual date labels: abbreviated day-of-week for Week period, day number for Month period, abbreviated month name for Year period.
- **FR-004**: Dashed horizontal grid lines MUST appear at each Y-axis tick mark.
- **FR-005**: In Month period, the chart MUST scroll horizontally with fling physics and MUST start scrolled to the most recent day (right edge).
- **FR-006**: In Week and Year periods, the chart MUST fit all bars within the visible width without scrolling.
- **FR-007**: Tapping a bar MUST display a floating tooltip showing the exact formatted amount and the date.
- **FR-008**: The tooltip MUST use inverted colors per theme: dark background with white text in light theme, light background with dark text in dark theme.
- **FR-009**: The "today" bar MUST render at full opacity (1.0) while other bars render at reduced opacity (~0.7).
- **FR-010**: A small accent-colored dot indicator MUST appear above the "today" bar.
- **FR-011**: The chart MUST adapt colors (bars, grid lines, axis text, tooltip) to both light and dark themes.
- **FR-012**: The existing Expenses/Income toggle above the chart MUST continue to switch the displayed data set.
- **FR-013**: The total row below the chart (e.g., "Total expenses: 1,245,600 ₸") MUST be preserved unchanged.
- **FR-014**: The chart data MUST be sourced from the existing daily/monthly expense and income data flows — no changes to the domain or data layers.
- **FR-015**: The donut chart (category breakdown) and "By Category" section MUST remain unchanged.
- **FR-016**: The bar chart MUST show an "unavailable" state when currency conversion data is missing (mixed-currency scenario), preserving existing behavior.

### Key Entities

- **ChartDataPoint**: Represents one bar — contains a date bucket, display label, monetary amount, and a flag for "today" highlight. *(Existing entity, may simplify since the charting library handles axis formatting internally.)*
- **ChartModelProducer**: The charting library's data producer — replaces the pre-built Y-axis label list. Fed from the ViewModel with resolved daily/monthly amounts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can read the exact monetary scale of any bar within 2 seconds by referencing the Y-axis labels, without needing to tap.
- **SC-002**: In Month view, users can scroll through all 28–31 daily bars smoothly with each bar being individually distinguishable (no overlapping or truncated labels).
- **SC-003**: Tapping any bar reveals the exact amount within 300ms, and the tooltip is readable without obscuring adjacent bars.
- **SC-004**: The chart correctly renders in both light and dark themes with all elements (bars, axes, grid, tooltips) maintaining readable contrast.
- **SC-005**: Users can identify "today's" bar at a glance without reading axis labels — verified by the visual opacity difference and dot indicator.
- **SC-006**: Switching between Expenses and Income updates the chart data without full screen reload or loss of scroll position (in Month view).
- **SC-007**: The chart renders correctly across all three periods (Week: 7 bars, Month: 28–31 bars, Year: 12 bars) with appropriate axis labels for each.

## Assumptions

- Currency formatting reuses existing utility logic from the project — the same formatting patterns used for the current Y-axis labels will be adapted for the new value formatter callbacks.
- The "today" bar highlight (opacity + dot) is achievable through per-entry styling without requiring a custom drawing layer.
- No new Gradle modules are needed — the charting library dependency is added to the existing presentation statistics module.
- The "unavailable" overlay for mixed-currency scenarios is handled at the section level (same as today) — the charting library component is simply not rendered in that case.

## Scope Boundaries

**In scope**:
- Replace `ChartBars`, `YAxisLabels`, and `StatisticsBarChartSection` composables with library-based chart implementation
- Adapt ViewModel to produce data compatible with the charting library's data producer
- Simplify `StatisticsChartState` if the library handles axis label formatting internally

**Out of scope**:
- Donut chart (category breakdown) — remains custom Canvas implementation
- Domain layer — no changes to data retrieval or aggregation
- Data layer — no changes to repositories or DAOs
- New Gradle modules — dependency added to existing module only
- Category breakdown section — no visual or functional changes
