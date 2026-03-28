# Feature Specification: Pre-iOS Port Feature Bundle

**Feature Branch**: `016-pre-ios-features`
**Created**: 2026-03-27
**Status**: Draft
**Input**: 5 features to implement before iOS port: current month period, date picker, recurring transactions, category budgets, CSV export

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Current Month as Default Period (Priority: P1)

A user opens the app and immediately sees transactions from the current calendar month (March 1 – today), not the last 30 rolling days. This matches how people naturally think about their spending — "how much did I spend this month?" The existing "30 days" period remains available as a separate option.

**Why this priority**: This is the simplest change with the highest daily-use impact. Every user sees the period filter on every launch. Calendar month alignment makes income/expense summaries match paycheck cycles and monthly bills.

**Independent Test**: Can be fully tested by opening the transaction list and verifying the default filter shows only transactions from the 1st of the current month through today.

**Acceptance Scenarios**:

1. **Given** the user opens the app for the first time (or after clearing preferences), **When** the transaction list loads, **Then** the "This Month" chip is selected by default and only transactions from the 1st of the current month through today are shown
2. **Given** the user is viewing "This Month", **When** today is March 15, **Then** only transactions from March 1–15 are shown, and the income/expense summary reflects only those transactions
3. **Given** the period chips row is visible, **When** the user looks at the available periods, **Then** they see: This Month | Day | Week | 30 days | Year (in that order)
4. **Given** the user selects "30 days", **When** the list refreshes, **Then** it shows the last 30 rolling days (existing behavior, unchanged)

---

### User Story 2 - Custom Date Filtering via DatePicker (Priority: P1)

A user wants to see transactions for a specific month (e.g., "January 2026") or a custom date range (e.g., "March 5–20"). They tap a calendar icon next to the period chips, choose between month selection or range selection, and the transaction list filters accordingly. The income/expense summary updates to match.

**Why this priority**: Combined with Story 1, this gives users full control over time-based filtering — the most fundamental interaction in a finance app. Users reviewing past months or specific date windows (vacation, pay period) need this daily.

**Independent Test**: Can be tested by tapping the calendar icon, selecting a month or custom range, and verifying the transaction list and summary update correctly.

**Acceptance Scenarios**:

1. **Given** the user is on the transaction list, **When** they tap the calendar icon at the end of the period chips row, **Then** a date picker dialog appears with two modes: "Month" and "Range"
2. **Given** the date picker is open in "Month" mode, **When** the user selects "January 2026", **Then** the dialog closes, all period chips deselect, and the list shows only January 2026 transactions with a label like "Январь 2026"
3. **Given** the date picker is open in "Range" mode, **When** the user picks March 5 to March 20, **Then** the dialog closes, all period chips deselect, and the list shows transactions from March 5–20 with a label like "5 мар – 20 мар"
4. **Given** a custom date range is active (no period chip selected), **When** the user taps any period chip (e.g., "Week"), **Then** the custom range clears, that chip becomes active, and the list reverts to the chip's standard behavior
5. **Given** a custom month is selected, **When** the user views the income/expense summary card, **Then** the totals reflect only the transactions in that custom month

---

### User Story 3 - Recurring Transactions (Priority: P2)

A user has monthly bills (rent, subscriptions, salary) that repeat on a schedule. They create a recurring transaction template specifying amount, category, account, and frequency (daily/weekly/monthly/yearly). The app automatically generates real transactions on the scheduled dates. When the user opens the app after missing several days, all pending transactions are back-filled.

**Why this priority**: Recurring transactions reduce manual data entry significantly for regular income/expenses. However, this is a new system (entity, generation logic, two screens) rather than an enhancement to existing flows, so it's P2.

**Independent Test**: Can be tested by creating a monthly recurring transaction, advancing past its next date, and verifying that real transactions appear in the list with correct amounts and categories.

**Acceptance Scenarios**:

1. **Given** the user navigates to the recurring transactions list (from Settings), **When** they tap the add button, **Then** a form appears with fields: amount, type (income/expense), category, account, frequency, start date, optional end date, and note
2. **Given** the user creates a monthly recurring for "Rent" at 150,000 on the 1st of each month starting March 1, **When** they save it, **Then** it appears in the recurring list showing amount, category, frequency ("Monthly"), and next date
3. **Given** a monthly recurring was last generated on March 1, **When** the user opens the app on April 3, **Then** the system auto-generates a transaction for April 1 in the regular transaction list and updates the account balance
4. **Given** a weekly recurring is set for every Monday, **When** the user hasn't opened the app for 3 weeks, **Then** 3 transactions are generated (one for each missed Monday)
5. **Given** a recurring transaction has an end date of March 31, **When** April 1 arrives, **Then** no further transactions are generated and the recurring is marked inactive
6. **Given** the user views the recurring list, **When** they toggle the active/paused switch on a recurring, **Then** it stops/resumes generating transactions
7. **Given** the user is on the recurring list, **When** they delete a recurring template, **Then** it is removed from the list but previously generated transactions remain untouched

---

### User Story 4 - Category Budgets (Priority: P2)

A user wants to set monthly spending limits per category (e.g., "Food: 100,000 per month"). They create budgets, and the app shows progress bars indicating how much has been spent vs. the limit for the current calendar month. Color coding (green/yellow/red) provides at-a-glance status.

**Why this priority**: Budgets add proactive financial awareness (spending control) vs. reactive tracking. This is a new system similar in scope to recurring transactions, hence P2.

**Independent Test**: Can be tested by creating a budget for a category, adding expense transactions in that category, and verifying the progress bar and percentage update correctly.

**Acceptance Scenarios**:

1. **Given** the user navigates to the budgets screen (from Settings), **When** they tap the add button, **Then** a form appears with: expense category picker and monthly limit amount
2. **Given** the user creates a budget for "Food" with a limit of 100,000, **When** they view the budgets list, **Then** it shows: "Food" icon/name, a progress bar, and a label showing spent vs. limit
3. **Given** a "Food" budget exists with a 100,000 limit and the user has spent 60,000 on food this month, **When** they view budgets, **Then** the progress bar shows 60%, colored green
4. **Given** spending reaches 75,000 (75%), **When** the user views budgets, **Then** the progress bar turns yellow
5. **Given** spending reaches 95,000 (95%), **When** the user views budgets, **Then** the progress bar turns red
6. **Given** spending exceeds 100,000, **When** the user views budgets, **Then** the bar is full/red and an "Over budget" label appears
7. **Given** it is April 1 (new month), **When** the user views budgets, **Then** all "spent" amounts reset to 0 (budgets track current calendar month only)
8. **Given** the budget list exists, **When** viewing items, **Then** they are sorted by percentage used (most-used first)

---

### User Story 5 - CSV Export (Priority: P3)

A user wants to export their transactions to a CSV file to share with an accountant, import into a spreadsheet, or keep as a backup. They trigger the export from Settings, and the app generates a CSV and opens the system share sheet.

**Why this priority**: Export is a "nice to have" that serves occasional use (tax time, accountant requests) rather than daily interaction. The scope is small (single use case, no new screens).

**Independent Test**: Can be tested by tapping "Export to CSV" in Settings, verifying a share sheet appears, and opening the resulting CSV file to confirm correct data formatting.

**Acceptance Scenarios**:

1. **Given** the user is in Settings, **When** they tap "Export to CSV", **Then** a loading indicator appears while the export is generated
2. **Given** the export finishes successfully, **When** the share sheet opens, **Then** the user can share or save a CSV file named `money_manager_export_YYYY-MM-DD.csv`
3. **Given** the CSV file is opened, **When** inspecting the contents, **Then** it has columns: Date, Type, Amount, Category, Account, Note — with a header row and comma-separated values
4. **Given** a transaction note contains commas (e.g., "Rent, March"), **When** exported, **Then** the note field is properly quoted in the CSV
5. **Given** the export fails (e.g., no transactions exist), **When** an error occurs, **Then** the user sees an appropriate error message instead of a crash
6. **Given** the user has transactions in multiple accounts, **When** exporting, **Then** all transactions across all accounts are included with the account name in each row

---

### Edge Cases

- **Current month on the 1st**: When today is the 1st of the month, "This Month" should show only today's transactions (range = 1st–1st)
- **Custom range with no transactions**: When a selected month/range has zero transactions, the list should show an empty state and the summary should show zero income / zero expense
- **Recurring with past start date**: If a user creates a recurring transaction with a start date in the past, missed transactions between start and today should be generated immediately
- **Recurring day-of-month overflow**: For a monthly recurring on the 31st, months with fewer days (e.g., February) should generate on the last day of that month
- **Budget for deleted category**: If a category linked to a budget is deleted, the budget should still display (with the last known category info) but no new spending will accumulate
- **CSV with special characters**: Transaction notes with quotes, newlines, or Unicode characters must be properly escaped in the CSV output
- **Large export**: If the user has thousands of transactions, the export should not freeze the UI (must run in background)
- **Recurring + budget interaction**: Transactions generated by recurring templates should count toward budget spending for the month they are generated in

## Requirements *(mandatory)*

### Functional Requirements

**Period & Date Filtering**

- **FR-001**: System MUST provide a "Current Month" period option that filters transactions from the 1st of the current calendar month through today
- **FR-002**: "Current Month" MUST be the default period when the transaction list first loads
- **FR-003**: The existing "30 days" rolling period MUST remain available as a separate option labeled "30 days" (distinct from "This Month")
- **FR-004**: Period chips MUST display in order: This Month, Day, Week, 30 days, Year
- **FR-005**: System MUST provide a calendar icon button at the end of the period chips row that opens a date picker dialog
- **FR-006**: The date picker MUST support two modes: month selection (pick a specific month+year) and date range selection (pick start and end dates)
- **FR-007**: When a custom date is selected, all period chips MUST visually deselect and a label showing the selected range MUST appear
- **FR-008**: When a period chip is tapped after a custom date selection, the custom range MUST clear and the chip's standard behavior MUST apply
- **FR-009**: Income/expense summary and balance MUST recalculate based on whichever filter is active (period chip or custom date range)

**Recurring Transactions**

- **FR-010**: System MUST allow users to create recurring transaction templates with: amount, type (income/expense), category, account, frequency (daily/weekly/monthly/yearly), start date, optional end date, and optional note
- **FR-011**: For monthly frequency, the user MUST be able to specify the day of month (1–31). For months with fewer days than the selected day, the transaction MUST be generated on the last day of that month (e.g., day 31 in February generates on Feb 28/29)
- **FR-012**: For weekly frequency, the user MUST be able to specify the day of week (Monday–Sunday)
- **FR-013**: On app startup, the system MUST auto-generate all missed transactions between the last generated date and today for each active recurring template
- **FR-014**: Transaction generation MUST be atomic — either all pending transactions for a recurring template are created, or none are
- **FR-015**: Generated transactions MUST update the associated account balance
- **FR-016**: Recurring templates with a past end date MUST be automatically deactivated
- **FR-017**: Users MUST be able to view, create, edit, pause/resume, and delete recurring templates
- **FR-018**: Deleting a recurring template MUST NOT affect previously generated transactions
- **FR-019**: The recurring transactions list MUST be accessible from the Settings screen

**Category Budgets**

- **FR-020**: System MUST allow users to set a monthly spending limit for any expense category
- **FR-021**: Budget progress MUST show current month's actual spending vs. the set limit
- **FR-022**: Budget progress MUST be color-coded: green (<70%), yellow (70–90%), red (>90%)
- **FR-023**: When spending exceeds the limit, an "Over budget" indicator MUST appear
- **FR-024**: Budget spending calculations MUST use the current calendar month (1st through end of month)
- **FR-025**: Budget list MUST be sorted by percentage used (most consumed first)
- **FR-026**: Users MUST be able to create, edit, and delete budgets
- **FR-027**: The budgets screen MUST be accessible from the Settings screen

**CSV Export**

- **FR-028**: System MUST export all transactions to a CSV file with columns: Date (yyyy-MM-dd), Type, Amount, Category, Account, Note
- **FR-029**: CSV MUST properly handle special characters (commas, quotes, newlines) in field values using standard CSV quoting rules
- **FR-030**: Export MUST run in background without freezing the UI, showing a loading indicator
- **FR-031**: On successful export, the system MUST open the platform share sheet for the user to save or share the file
- **FR-032**: On export failure, the system MUST show a user-friendly error message
- **FR-033**: The export action MUST be accessible from the Settings screen

**Localization**

- **FR-034**: All new UI text MUST be localized in three languages: Russian, English, and Kazakh

### Key Entities

- **Period (updated)**: Time range filter for the transaction list. Adds "Current Month" (calendar month: 1st to today) alongside existing periods (Day, Week, 30 days, Year). The "30 days" option retains the rolling-window behavior previously called "Month"
- **Custom Date Range**: A user-specified time window (either a specific month+year or an arbitrary start-end date pair) that overrides the period chip filter when active
- **Recurring Transaction Template**: A blueprint for auto-generating transactions on a schedule. Key attributes: amount, income/expense type, category, account, frequency (daily/weekly/monthly/yearly), schedule parameters (day of month/week), start date, optional end date, active status, and the date of last generation
- **Budget**: A monthly spending cap for a specific expense category. Key attributes: linked category, monthly limit. Derived at display time: amount spent (sum of matching expenses in current calendar month), remaining amount, percentage consumed
- **CSV Export**: A temporary file containing all transactions formatted as comma-separated values with a header row, shared via the platform share mechanism

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users see current calendar month transactions by default on every app launch, reducing mental overhead of interpreting which transactions are visible
- **SC-002**: Users can filter transactions by any specific month or arbitrary date range in under 3 taps (tap calendar, select, confirm)
- **SC-003**: Users with regular income/expenses can set up recurring templates and never manually enter those transactions again — the app generates them automatically on each launch
- **SC-004**: All pending recurring transactions are generated within 2 seconds of app startup, regardless of how many days were missed
- **SC-005**: Users can see at a glance whether they are within budget for each spending category via color-coded progress bars
- **SC-006**: Budget progress accurately reflects current calendar month spending, updating immediately when new transactions are added
- **SC-007**: Users can export all transactions to a CSV file and share it in under 10 seconds for a typical dataset (up to 5,000 transactions)
- **SC-008**: The exported CSV opens correctly in spreadsheet applications with proper column separation and character encoding
- **SC-009**: All new UI elements are accessible in Russian, English, and Kazakh with no untranslated strings

## Assumptions

- The "Month" period label will be renamed to "30 days" in all three languages to differentiate it from "Current Month" / "This Month"
- Monthly recurring transactions set for day 29–31 will fall back to the last day of shorter months (e.g., Feb 28)
- Budget spending is computed against expense transactions only (not income) for the current calendar month
- CSV export includes ALL transactions (not filtered by current period/account) — the user gets a complete dataset
- A FileProvider will need to be added to the app manifest for CSV sharing
- The recurring transaction generation runs on app startup to ensure data consistency before the UI is displayed
- One budget per category — creating a budget for a category that already has one will update the existing budget
