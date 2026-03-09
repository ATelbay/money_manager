# Feature Specification: Statistics Screen Audit & Bug Fixes

**Feature Branch**: `002-statistics-audit`
**Created**: 2026-03-09
**Status**: Draft
**Input**: User description: "Аудит и исправление багов на экране Статистика"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Statistics screen does not crash on any period (Priority: P1)

A user opens the Statistics screen and switches between WEEK, MONTH, and YEAR periods. The screen must render without crashes regardless of the number of data points.

**Why this priority**: App crashes are the most severe user-facing issue — they destroy trust and prevent any use of the feature.

**Independent Test**: Can be fully tested by switching to each period (WEEK/MONTH/YEAR) with varying amounts of transaction data (0, 1, 100, 365+ transactions) and verifying no crash occurs.

**Acceptance Scenarios**:

1. **Given** user has transactions spanning 12 months, **When** user selects YEAR period, **Then** bar chart renders without crash (no negative bar width)
2. **Given** user has transactions spanning 30 days, **When** user selects MONTH period, **Then** bar chart renders all bars without overlap or clipping
3. **Given** user has no transactions, **When** user opens Statistics, **Then** an appropriate empty state is shown (not a blank/frozen screen)

---

### User Story 2 - Period date range is correct and consistent (Priority: P1)

A user views statistics for a period and expects the data range to start at the beginning of the first day (00:00:00) and end at the end of the last day (23:59:59). Both bar chart and category breakdown must reflect the same date range.

**Why this priority**: Incorrect date ranges mean users see wrong financial data, undermining the app's core value proposition.

**Independent Test**: Can be verified by creating a transaction at 00:01 on the first day of a period and confirming it appears in the statistics.

**Acceptance Scenarios**:

1. **Given** user created a transaction at 00:05 AM today, **When** user views WEEK period, **Then** the transaction is included in the statistics (period start is 00:00:00, not 23:59:59)
2. **Given** user has transactions across 7 days, **When** user views WEEK period, **Then** bar chart and category breakdown show data for the exact same date range
3. **Given** user views YEAR period during a leap year, **When** February 29 has transactions, **Then** those transactions are included correctly

---

### User Story 3 - Error feedback instead of infinite loading (Priority: P1)

When the data layer encounters an error, the user sees a meaningful error message with a retry option instead of an infinite loading spinner.

**Why this priority**: Without error handling, any transient failure leaves users stuck with no recourse, creating the impression the app is broken.

**Independent Test**: Can be tested by simulating a data error and verifying the error state appears with a retry action.

**Acceptance Scenarios**:

1. **Given** a data loading error occurs, **When** user is on Statistics screen, **Then** an error message is displayed with a "Retry" action
2. **Given** user sees an error state, **When** user taps "Retry", **Then** the system attempts to reload the data
3. **Given** data loads successfully after retry, **When** results arrive, **Then** statistics display normally

---

### User Story 4 - Bar chart is readable at all periods (Priority: P2)

The bar chart displays data legibly regardless of period. Labels do not overlap, bars are appropriately sized, and days without transactions are represented (not omitted).

**Why this priority**: An unreadable chart provides no value — users cannot interpret their spending patterns if labels overlap or days are missing.

**Independent Test**: Can be tested by viewing bar chart at WEEK (7 bars), MONTH (~30 bars), and YEAR (~365 bars) and verifying all labels are legible and spacing is correct.

**Acceptance Scenarios**:

1. **Given** YEAR period is selected with daily data, **When** bar chart renders, **Then** bars are aggregated by month (12 bars total) to prevent overcrowding
2. **Given** MONTH period is selected, **When** some days have no transactions, **Then** those days still appear on the chart as zero-height bars
3. **Given** any period is selected, **When** bar chart renders, **Then** labels do not overlap adjacent labels
4. **Given** WEEK period is selected, **When** bar chart renders, **Then** each of the 7 days appears as an individual bar with a readable date label

---

### User Story 5 - Expense/Income toggle on statistics (Priority: P2)

Users can switch between viewing expense statistics and income statistics. By default, expenses are shown. The toggle affects both bar chart and category breakdown.

**Why this priority**: Showing only expenses gives an incomplete financial picture — users need income visibility to understand their full financial situation.

**Independent Test**: Can be tested by toggling between expense and income views and verifying chart data, category breakdown, and totals update accordingly.

**Acceptance Scenarios**:

1. **Given** user is on Statistics screen, **When** screen loads, **Then** expenses are shown by default with a visible toggle to switch to income
2. **Given** user taps the income toggle, **When** income data exists, **Then** bar chart shows daily income, donut chart shows income by category, and total reflects income sum
3. **Given** user taps the income toggle, **When** no income data exists, **Then** an appropriate empty state is shown for income
4. **Given** user switches from income back to expenses, **When** the toggle changes, **Then** all charts and breakdowns update to reflect expenses

---

### User Story 6 - Donut chart animation is smooth and non-flickering (Priority: P2)

The donut chart animates once when data first loads or when the user changes period/toggle. It does not re-animate on every data re-emission from the reactive data layer.

**Why this priority**: Flickering animations are visually distracting and make the app feel unpolished.

**Independent Test**: Can be tested by observing the donut chart during period switches — animation should play once per user-initiated change, not repeatedly.

**Acceptance Scenarios**:

1. **Given** user opens Statistics, **When** data loads, **Then** donut chart animates in once smoothly
2. **Given** data stream re-emits identical data, **When** donut chart is already displayed, **Then** no re-animation occurs
3. **Given** user switches period, **When** new data loads, **Then** donut chart animates to the new values once

---

### User Story 7 - Category percentages are accurate (Priority: P3)

Category percentages in the breakdown list use proper rounding and always sum to 100% (using largest-remainder or similar method).

**Why this priority**: Percentages not summing to 100% looks like a calculation error and reduces user confidence in the data.

**Independent Test**: Can be tested by creating transactions in 3+ categories and verifying the displayed percentages sum to exactly 100%.

**Acceptance Scenarios**:

1. **Given** user has expenses in 3 categories (41.8%, 33.3%, 24.9%), **When** percentages display, **Then** they show as rounded values that sum to 100% (e.g., 42% + 33% + 25%)
2. **Given** user has expenses in 1 category, **When** percentage displays, **Then** it shows 100%
3. **Given** user has expenses in many small categories, **When** percentages display, **Then** no category shows 0% if it has a non-zero amount (minimum 1%)

---

### Edge Cases

- What happens when all transactions in a period belong to a single category? (100% donut, single bar per day)
- What happens when the user has exactly 0 transactions in the selected period? (Empty state for both charts)
- What happens when the period spans a DST (Daylight Saving Time) change? (Date range should still be correct)
- What happens when transaction amounts are extremely large (e.g., 999,999,999)? (Center text in donut should not overflow)
- What happens when there are hundreds of categories? (Category list should scroll, not overflow)
- What happens during a leap year when YEAR period is selected? (366 days should be handled correctly)

## Clarifications

### Session 2026-03-09

- Q: How should the YEAR period bar chart aggregate data? → A: Monthly aggregation (12 bars, one per calendar month)
- Q: Should periods be rolling (last N days) or calendar-aligned? → A: Rolling periods (last 7 days, last 30 days, last 365 days from today)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST NOT crash when rendering bar chart for any period (WEEK, MONTH, YEAR) regardless of the number of data points
- **FR-002**: Period date range MUST use rolling windows (WEEK = last 7 days, MONTH = last 30 days, YEAR = last 365 days), starting at 00:00:00.000 of the first day and ending at 23:59:59.999 of today
- **FR-003**: Bar chart and category breakdown MUST use the same date range for the same period selection
- **FR-004**: System MUST display an error state with retry action when data loading fails
- **FR-005**: System MUST provide an expense/income toggle that switches all visualizations (bar chart, donut chart, category breakdown, total amount)
- **FR-006**: Bar chart MUST show all days in the period, including days with zero transactions (as zero-height bars or equivalent)
- **FR-007**: For YEAR period, bar chart MUST aggregate data by month (12 bars, one per calendar month) to maintain readability
- **FR-008**: Bar chart labels MUST NOT overlap adjacent labels at any period
- **FR-009**: Donut chart animation MUST play once per data change, not on every reactive data re-emission
- **FR-010**: Category percentages MUST use proper rounding that guarantees the sum equals 100%
- **FR-011**: Donut chart center text MUST remain readable for large amounts (handle text overflow gracefully)
- **FR-012**: Date labels on bar chart MUST use locale-appropriate formatting

### Key Entities

- **StatsPeriod**: The time range selector (WEEK, MONTH, YEAR) that determines what data is displayed
- **TransactionType filter**: Expense or Income — determines which transactions are included in statistics
- **DailyAmount**: A single day's aggregated amount, used as one bar in the chart. Must include zero-amount days
- **CategorySummary**: A category's total amount and percentage share within the selected period and transaction type

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Statistics screen renders without crash for all 3 periods (WEEK, MONTH, YEAR) with 0 to 1000+ transactions — 0 crashes
- **SC-002**: Period date range includes transactions created at 00:01 on the first day of the period — 100% of such transactions are counted
- **SC-003**: Category percentages always sum to exactly 100% for any combination of categories
- **SC-004**: Users can switch between expense and income views with all visualizations updating correctly
- **SC-005**: Bar chart labels have no visual overlap at WEEK, MONTH, and YEAR periods
- **SC-006**: Error state appears within 2 seconds of a data loading failure, with a working retry action
- **SC-007**: Donut chart animation plays exactly once per user-initiated data change (period switch or expense/income toggle)
