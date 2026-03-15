# Feature Specification: Statistics UX Cleanup + Category Transaction Drill-Down

**Feature Branch**: `008-statistics-category-drilldown`
**Created**: 2026-03-15
**Status**: Draft
**Input**: User description: "Improve the Statistics screen by removing duplicate expense/income controls, using the summary cards as the only selector, and adding category transaction drill-down that preserves the active statistics context."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Statistics has one clear expense/income selector (Priority: P1)

As a user, when I open Statistics, I should see one obvious way to switch between expense and income. The summary cards should be the selector, and the duplicate top toggle should be gone.

**Why this priority**: The current screen repeats the same choice twice, which makes the top of the screen feel noisy and ambiguous before the user even starts reading the charts.

**Independent Test**: Open Statistics and confirm the standalone top expense/income toggle is absent. Verify both summary cards remain visible, the active card is clearly selected, the inactive card remains visible but subdued, and tapping either card changes the active type.

**Acceptance Scenarios**:

1. **Given** I open Statistics, **When** the screen renders, **Then** there is no standalone expense/income toggle above the summary cards.
2. **Given** I am on Statistics, **When** the top section renders, **Then** both expense and income summary cards remain visible at the same time.
3. **Given** expense is the active statistics type, **When** I view the summary cards, **Then** the expense card is clearly selected and the income card remains visible but less emphasized.
4. **Given** income is the active statistics type, **When** I tap the expense summary card, **Then** expense becomes the active statistics type.
5. **Given** expense is the active statistics type, **When** I tap the income summary card, **Then** income becomes the active statistics type.

---

### User Story 2 - All statistics content stays in sync with the selected type (Priority: P1)

As a user, when I switch between expense and income from the summary cards, every statistics visualization should update together so I always know I am looking at one consistent dataset.

**Why this priority**: Switching type is only trustworthy if the donut chart, bar chart, totals, category list, and empty state all reflect the same active selection.

**Independent Test**: Open Statistics, switch between expense and income using the summary cards, and verify the donut chart, bar chart, category breakdown, center values, totals, and empty state logic all update consistently.

**Acceptance Scenarios**:

1. **Given** expense statistics are visible, **When** I tap the income card, **Then** the donut chart, bar chart, category breakdown, totals, and center values update to income data.
2. **Given** income statistics are visible, **When** I tap the expense card, **Then** the donut chart, bar chart, category breakdown, totals, and center values update to expense data.
3. **Given** one transaction type has no data in the selected period, **When** I switch to that type, **Then** Statistics shows the correct empty state for that type instead of stale content from the previous type.

---

### User Story 3 - I can drill down from a category row to its transactions (Priority: P1)

As a user, when I review the category breakdown on Statistics, I want to tap a category row and open a dedicated transaction list for that category without leaving the Statistics flow.

**Why this priority**: Category totals are informative, but they are not actionable unless I can inspect the transactions behind them.

**Independent Test**: Open Statistics, choose a period and transaction type, tap a category row, and verify a dedicated detail screen opens inside Statistics. Confirm the donut chart, donut slices, and legend do not open drill-down.

**Acceptance Scenarios**:

1. **Given** I am viewing category breakdown on Statistics, **When** I tap a category row, **Then** a dedicated drill-down screen opens for that category inside the Statistics flow.
2. **Given** I am on Statistics, **When** I tap the donut chart, a donut slice, or the chart legend, **Then** no category drill-down opens.
3. **Given** I open category drill-down from Statistics, **When** the detail screen appears, **Then** I remain in the Statistics navigation flow rather than being switched to the Home tab or another top-level destination.
4. **Given** I am on Statistics, **When** I tap a category row, **Then** the transactions are not expanded inline under the row on the same screen.

---

### User Story 4 - Drill-down preserves the exact statistics context and reacts to edits (Priority: P2)

As a user, when I inspect a category from Statistics, I want the detail screen to preserve the exact category, type, period, and date range I tapped from, and I want both screens to stay correct after edits or deletions.

**Why this priority**: Drill-down loses value if it shows a different filter window than Statistics or if it returns me to stale totals after I edit a transaction.

**Independent Test**: Open Statistics in a known context such as YEAR + Income, tap a category, verify the list contains only matching transactions ordered newest first, edit or delete a transaction, and confirm the drill-down and originating Statistics screen refresh without losing the active period or type.

**Acceptance Scenarios**:

1. **Given** I am viewing YEAR + Income on Statistics, **When** I tap a category row, **Then** the drill-down screen shows only that category's income transactions for the exact same date range Statistics used for YEAR at the moment I tapped.
2. **Given** I open the drill-down screen, **When** the header renders, **Then** it clearly shows the selected category identity and the current type and period context.
3. **Given** I open the drill-down screen, **When** transactions are shown, **Then** they are ordered newest first.
4. **Given** I tap a transaction in drill-down, **When** the existing detail or edit flow opens and I save or delete the transaction, **Then** the drill-down list refreshes to reflect the change.
5. **Given** I opened drill-down from Statistics before the current day boundary, **When** the calendar day changes while I remain in drill-down or transaction edit, **Then** the drill-down stays pinned to the exact preserved date range captured when I tapped the category row.
6. **Given** the category is renamed or recolored after I open drill-down, **When** the latest category metadata is available, **Then** the drill-down header updates to the latest category identity; otherwise it keeps the tapped snapshot as fallback.
7. **Given** I return from drill-down to Statistics after viewing or editing transactions, **When** the Statistics screen appears again, **Then** the same period and same transaction type remain active and the statistics reflect the latest data.
8. **Given** the selected category has no matching transactions for the preserved context, **When** the drill-down screen loads, **Then** it shows a proper empty state instead of a blank screen.

---

### Edge Cases

- A category row is tapped while viewing YEAR, and the resulting drill-down range spans more than one calendar year.
- The selected type has no data for the current period, and switching types must show the correct empty state with no stale chart data.
- A category has only one matching transaction and that transaction is deleted from the drill-down flow.
- Category names are long enough to truncate in the breakdown list or drill-down header.
- A category is renamed or recolored while the user is moving between Statistics and drill-down.
- The current day rolls over while the user stays inside drill-down or transaction edit.
- The user returns from transaction edit after changing a transaction so it no longer belongs to the selected category, type, or date range.

## Scope Constraints

- This feature refines the existing Statistics experience and does not redesign the entire app.
- Existing Statistics charts remain part of the screen and keep their current purpose.
- The only drill-down entry point is category rows in the breakdown section.
- Drill-down does not expand inline inside the Statistics screen.
- Drill-down does not route through a different top-level destination such as Home.

## Clarifications

### Session 2026-03-15

- Q: Which control remains as the expense/income selector? → A: The existing expense and income summary cards are the only selector.
- Q: Which UI element opens drill-down? → A: Only category rows in the breakdown section open drill-down.
- Q: How should drill-down appear? → A: As a dedicated detail screen inside the Statistics flow.
- Q: How exact must filtering be? → A: The drill-down uses the exact same resolved date range Statistics was using when the category row was tapped.

### Session 2026-03-16

- Q: What happens if the current day changes while the user stays inside drill-down or transaction edit? → A: Drill-down remains pinned to the exact `startMillis` and `endMillis` captured when the category row was tapped.
- Q: What happens when the user returns to Statistics after a day rollover? → A: Statistics keeps the same selected period and transaction type, then resumes its normal live current-period range resolution when shown again.
- Q: If category metadata changes after drill-down opens, should the header use the snapshot or the latest category data? → A: Prefer live category metadata when available, and fall back to the tapped snapshot only if the category cannot be resolved.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Statistics MUST expose exactly one transaction-type selector in the top section.
- **FR-002**: The standalone top expense/income toggle MUST be removed.
- **FR-003**: The expense and income summary cards MUST be the only control that selects the active statistics type.
- **FR-004**: Both summary cards MUST remain visible at all times.
- **FR-005**: The active summary card MUST be clearly visually selected.
- **FR-006**: The inactive summary card MUST remain visible but be clearly less emphasized than the active card.
- **FR-007**: Tapping the expense summary card MUST switch Statistics to expense mode.
- **FR-008**: Tapping the income summary card MUST switch Statistics to income mode.
- **FR-009**: Switching the active summary card MUST update the donut chart, bar chart, category breakdown, totals, center values, and empty state logic to the selected type.
- **FR-010**: Each category row in the Statistics breakdown MUST be tappable and visually communicated as an interactive row.
- **FR-011**: Only category rows in the breakdown section MUST open drill-down.
- **FR-012**: The donut chart, donut slices, and chart legend MUST remain non-tappable for drill-down.
- **FR-013**: Statistics MUST NOT expand transactions inline inside the Statistics screen.
- **FR-014**: Tapping a category row MUST open a dedicated drill-down screen inside the Statistics flow.
- **FR-015**: The drill-down flow MUST NOT switch the user to the Home tab or reuse a different top-level destination as its primary navigation path.
- **FR-016**: The drill-down screen MUST be filtered by the tapped category.
- **FR-017**: The drill-down screen MUST be filtered by the statistics transaction type that was active when the user tapped.
- **FR-018**: The drill-down screen MUST be filtered by the selected statistics period and the exact same resolved date range Statistics used at tap time.
- **FR-019**: The drill-down screen MUST remain pinned to that preserved `startMillis` and `endMillis` for the lifetime of the drill-down session, even if the wall-clock date changes while the user remains inside drill-down or transaction edit.
- **FR-020**: The drill-down screen MUST clearly show the selected category identity, including category name and icon when available, and category color or accent when appropriate.
- **FR-021**: The drill-down header SHOULD prefer the latest available category metadata for the selected category and MUST fall back to the tapped snapshot metadata if the category cannot be resolved.
- **FR-022**: The drill-down screen MUST clearly show the current statistics context, including expense or income and the selected period.
- **FR-023**: The drill-down screen MUST show only transactions that match the preserved category, type, and date range context.
- **FR-024**: Transactions in drill-down MUST be ordered newest first.
- **FR-025**: Tapping a transaction from drill-down MUST open the existing transaction detail or edit flow.
- **FR-026**: If no transactions match the preserved drill-down context, the drill-down screen MUST show a proper empty state instead of a blank screen.
- **FR-027**: Returning from drill-down MUST keep Statistics on the same selected period and same selected transaction type.
- **FR-028**: When Statistics is shown again after drill-down, it MAY recompute its normal live current-period date range for the preserved period while keeping the same selected period and transaction type.
- **FR-029**: Statistics MUST reflect edits or deletions made through the drill-down flow after the user returns.

### Key Entities

- **Statistics Type Selector**: The pair of always-visible summary cards that chooses whether Statistics is showing expense data or income data.
- **Statistics Context Snapshot**: The selected statistics period, selected transaction type, and exact resolved date range active at the moment the user opens drill-down. This remains fixed for the lifetime of that drill-down session.
- **Category Drill-Down**: A dedicated Statistics detail screen that lists only the transactions for one category within one preserved Statistics context.
- **Category Breakdown Row**: A tappable row in the breakdown section that represents one category aggregate and serves as the only drill-down entry point.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Statistics shows exactly one expense/income selector, with no duplicate top toggle remaining on the screen.
- **SC-002**: Both summary cards remain visible in all Statistics states, and exactly one card is visually active at a time.
- **SC-003**: Switching between the summary cards updates the donut chart, bar chart, category breakdown, totals, center values, and empty state with no stale data from the previous type.
- **SC-004**: Users can open drill-down from a category aggregate in one tap, and category rows are the only drill-down entry point.
- **SC-005**: 100% of transactions shown in drill-down match the tapped category, active transaction type, and exact date range preserved from Statistics.
- **SC-006**: Transactions in drill-down are always ordered newest first.
- **SC-007**: Returning from drill-down preserves the previously active statistics period and transaction type every time.
- **SC-008**: After edits or deletions from drill-down, the updated transactions and aggregates are visible without stale Statistics content when the user returns.
