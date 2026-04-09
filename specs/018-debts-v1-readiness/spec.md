# Feature Specification: Debts Feature + Statistics TopBar Fix + Production Readiness

**Spec ID**: 018-debts-v1-readiness
**Status**: Draft
**Created**: 2026-04-09
**Branch**: `018-debts-v1-readiness`

---

## Overview

Three work streams to bring MoneyManager to v1 release readiness:

1. **Debts Feature** — Track money lent to or borrowed from contacts, with lifecycle management (create → partial payments → paid off) and automatic transaction generation for payments.
2. **Statistics TopBar Fix** — Replace the plain Row header in StatisticsScreen with a standard TopAppBar to fix edge-to-edge rendering where the header overlaps the system status bar.
3. **Production Readiness** — Crashlytics integration, accessibility pass, privacy policy, and version discipline.

---

## Clarifications

### Session 2026-04-09
- Q: Can user change debt direction (LENT↔BORROWED) after payments with linked transactions exist? → A: Allow direction change but warn user and offer to delete all existing payments first.
- Q: Can a payment amount exceed the remaining balance (overpayment)? → A: Allow any positive amount (pre-fill with remainingAmount), but show a notice if amount exceeds remaining.

---

## Problem Statement

### Debts Feature

Users have no way to track money they've lent to others or borrowed from others. Personal finance apps commonly include debt tracking because informal loans between friends, family, and colleagues are frequent. Without this feature, users must mentally track who owes what or use a separate app, reducing MoneyManager's value as a complete financial tool.

### Statistics TopBar

The Statistics screen header renders behind the system status bar on devices with edge-to-edge display (API 35+). This is because the screen uses a plain `Row` composable instead of the Material 3 `TopAppBar` that all other screens use. The title and filter controls are partially obscured, degrading usability.

### Production Readiness

The app lacks crash reporting, accessibility compliance, a privacy policy (required by Play Store), and formal version numbering. These are blockers for a public v1 release.

---

## User Scenarios & Testing

### Work Stream 1: Debts Feature

#### Scenario 1.1: Create a New Debt (Lent)
**Actor**: User who lent money to a friend
1. User navigates to Settings → "Долги" row
2. DebtListScreen opens showing an empty state
3. User taps FAB to open the debt creation bottom sheet
4. User enters: contact name "Алексей", selects "Я дал в долг", enters amount 50,000, selects "Kaspi Gold" account, optionally adds a note
5. User taps Save
6. Debt appears in the list with green styling, showing "50,000 ₸" total and "0 / 50,000 ₸" progress
- **Acceptance**: Debt is persisted, appears in list with correct direction badge and zero progress

#### Scenario 1.2: Create a New Debt (Borrowed)
**Actor**: User who borrowed money
1. User creates a debt with direction "Я взял в долг"
2. Debt appears in the list with red styling
- **Acceptance**: Debt displays with "borrowed" visual treatment, direction stored correctly

#### Scenario 1.3: Record a Partial Payment (with Transaction)
**Actor**: User receiving a partial repayment
1. User taps a LENT debt card to open DebtDetailScreen
2. User sees debt info, progress bar at 0%, and empty payments list
3. User taps "Записать платёж" button
4. Payment bottom sheet opens with amount pre-filled to remaining balance
5. User changes amount to 20,000, leaves "Создать транзакцию" checkbox checked
6. User saves the payment
7. Progress bar updates to 40%, payment appears in history, an Income transaction "Возврат долга: Алексей" is created in the linked account
- **Acceptance**: Payment recorded, progress updated, linked transaction created with correct category ("Возврат долга"), amount, and account

#### Scenario 1.4: Record Payment without Transaction
**Actor**: User who wants to track payment progress without creating a transaction
1. User unchecks "Создать транзакцию" in the payment bottom sheet
2. Payment is recorded, progress updates, but no transaction is created
- **Acceptance**: Payment persisted with `transactionId = null`, no transaction row inserted

#### Scenario 1.5: Fully Pay Off a Debt
**Actor**: User whose debt is fully repaid
1. User records a payment that brings `paidAmount` to equal or exceed `totalAmount`
2. Debt shows "Погашен" badge in both list and detail views
3. Debt appears under the "Погашенные" filter chip
- **Acceptance**: Status auto-derives to PAID_OFF when `remainingAmount <= 0`

#### Scenario 1.6: Filter Debts
**Actor**: User managing multiple debts
1. User sees filter chips: Все / Мне должны / Я должен / Погашенные
2. Tapping a chip filters the list to show only matching debts
3. Summary card at top updates to reflect filtered totals
- **Acceptance**: Each filter correctly includes/excludes debts by direction and status

#### Scenario 1.7: Delete a Debt
**Actor**: User removing a debt
1. User swipes a debt card to dismiss
2. Confirmation appears; user confirms
3. Debt is soft-deleted (isDeleted = 1)
4. Linked payments are cascade-deleted
5. Previously created transactions are NOT deleted
- **Acceptance**: Debt removed from list, transactions remain intact

#### Scenario 1.8: Delete a Payment
**Actor**: User correcting a payment entry
1. In DebtDetailScreen, user deletes a payment
2. Progress bar recalculates, remaining amount increases
3. Linked transaction (if any) is NOT deleted
- **Acceptance**: Payment removed, debt totals recalculated, transaction untouched

#### Scenario 1.9: Edit a Debt
**Actor**: User correcting debt details
1. In DebtDetailScreen, user taps edit action
2. DebtEditBottomSheet opens pre-filled with current values
3. User modifies contact name and amount, saves
4. Detail screen reflects updated values
- **Acceptance**: All editable fields can be modified and persisted

#### Scenario 1.10: Sync Debts across Devices
**Actor**: Signed-in user with multiple devices
1. User creates a debt on device A
2. Debt and payments sync to Firestore
3. Device B pulls the debt during sync
- **Acceptance**: Debts and payments sync bidirectionally using the same push/pull pattern as accounts and transactions

### Work Stream 2: Statistics TopBar Fix

#### Scenario 2.1: Statistics Header Renders Below Status Bar
**Actor**: User opening the Statistics tab
1. User navigates to the Statistics tab
2. Title "Статистика" displays fully below the system status bar
3. Calendar filter pill displays in the actions area
4. Layout matches the visual alignment of Transactions, Accounts, and Settings tabs
- **Acceptance**: No content overlaps with the system status bar on edge-to-edge devices

### Work Stream 3: Production Readiness

#### Scenario 3.1: Crash Reporting
**Actor**: User experiencing an app crash
1. App crashes in production
2. Crash report with stack trace is sent to Firebase Crashlytics
3. If user is signed in, crash is associated with their uid
- **Acceptance**: Crashes appear in Crashlytics console; no crash data collected in debug builds

#### Scenario 3.2: Accessibility — Screen Reader Navigation
**Actor**: User with visual impairment using TalkBack
1. User navigates through the app using TalkBack
2. All icon-only buttons announce their purpose (e.g., "Add transaction", "Delete", "Back")
3. Interactive elements have at least 48dp touch targets
4. Section headers in Settings are announced as headings
- **Acceptance**: Full screen reader navigation path through all primary flows without unlabeled or inaccessible elements

#### Scenario 3.3: Privacy Policy
**Actor**: User reviewing app privacy before installing
1. User navigates to Settings → About section
2. User taps "Политика конфиденциальности" link
3. Browser opens showing the privacy policy page
4. Policy explains data collected, encryption, Google Sign-In usage, no third-party sharing, and data deletion process
- **Acceptance**: Link opens valid web page; content covers all required Play Store disclosure topics

#### Scenario 3.4: Version Numbering
**Actor**: Developer preparing release build
1. App version is set to 1.0.0 (versionCode=1)
2. Version is visible in Settings → About section
- **Acceptance**: Correct version displayed in-app and in APK metadata

---

## Functional Requirements

### FR-1: Debt Data Model

- FR-1.1: Debt entity stores: id, contactName, direction (LENT/BORROWED), totalAmount, currency (denormalized from account for offline/sync display without account lookup), accountId, note (optional), createdAt timestamp
- FR-1.2: Debt entity includes sync fields: remoteId, updatedAt, isDeleted (soft delete)
- FR-1.3: paidAmount is computed as the sum of all non-deleted payments for the debt
- FR-1.4: remainingAmount is computed as totalAmount − paidAmount
- FR-1.5: Status (ACTIVE/PAID_OFF) is derived at runtime: PAID_OFF when remainingAmount ≤ 0
- FR-1.6: accountId references the account the money came from (LENT) or went to (BORROWED)

### FR-2: Debt Payment Data Model

- FR-2.1: DebtPayment entity stores: id, debtId (FK), amount, date, note (optional), transactionId (nullable FK), createdAt
- FR-2.2: DebtPayment includes sync fields: remoteId, updatedAt, isDeleted
- FR-2.3: Deleting a debt soft-deletes (isDeleted=1) all its payments
- FR-2.4: Deleting a payment does NOT delete the linked transaction
- FR-2.5: If a linked transaction is deleted independently, transactionId becomes null (ON DELETE SET NULL)

### FR-3: Transaction Integration

- FR-3.1: When recording a payment with "Создать транзакцию" enabled and direction = LENT → create an Income transaction with description "Возврат долга: {contactName}" and category "Возврат долга"
- FR-3.2: When recording a payment with "Создать транзакцию" enabled and direction = BORROWED → create an Expense transaction with description "Погашение долга: {contactName}" and category "Долги"
- FR-3.3: Transaction uses the same accountId as the debt
- FR-3.4: "Создать транзакцию" checkbox defaults to checked; user can uncheck to skip transaction creation
- FR-3.5: Two new default categories are added: "Долги" (Expense) and "Возврат долга" (Income)
- FR-3.6: Migration_7_8 must INSERT the two new default categories so existing users receive them on upgrade (DefaultCategories.all() only runs on fresh install via RoomDatabase.Callback.onCreate)

### FR-4: Debt List Screen

- FR-4.1: Accessible from Settings → "Долги" row (icon: AccountBalanceWallet, amber/orange color)
- FR-4.2: Summary card at top showing total lent amount vs total borrowed amount
- FR-4.3: Filter chips: All / Lent / Borrowed / Paid Off — filters the list and updates summary
- FR-4.4: Each debt card shows: contactName, direction badge, totalAmount, progress bar (paidAmount/totalAmount ratio), remaining amount
- FR-4.5: Green color coding for "мне должны" (LENT), red for "я должен" (BORROWED)
- FR-4.6: FAB opens DebtEditBottomSheet for creating a new debt
- FR-4.7: Swipe-to-dismiss with confirmation for debt deletion
- FR-4.8: Empty state illustration/message when no debts exist

### FR-5: Debt Detail Screen

- FR-5.1: TopAppBar shows contact name with edit and delete actions
- FR-5.2: Debt info card: direction, total amount, account name, creation date, note
- FR-5.3: Progress section: visual progress bar + "Выплачено X из Y" text
- FR-5.4: Payments history as a scrollable list of payment items (amount, date, note)
- FR-5.5: "Записать платёж" button opens payment bottom sheet
- FR-5.6: Payment bottom sheet: amount (pre-filled with remainingAmount), date picker, note, "Создать транзакцию" checkbox; if entered amount exceeds remainingAmount, show an informational notice (but allow saving)
- FR-5.7: "Погашен" badge visible when debt is fully paid off

### FR-6: Debt Edit Bottom Sheet

- FR-6.1: Fields: contactName (text), direction selector (two chips: "Я дал в долг" / "Я взял в долг"), amount, account selector, date picker (default: today), note
- FR-6.2: Supports both create and edit modes (pre-fills fields when editing)
- FR-6.3: Validates required fields: contactName non-empty, amount > 0, account selected
- FR-6.4: If user changes direction on a debt that has existing payments, show a warning dialog explaining that linked transactions will become semantically mismatched, and offer to delete all existing payments before applying the change; if user declines, direction change is cancelled

### FR-7: Firestore Sync

- FR-7.1: Debts and debt payments sync to Firestore using the same push/pull pattern as existing entities (accounts, transactions, budgets)
- FR-7.2: SyncManager gains syncDebt(id) and syncDebtPayment(id) with per-id Mutex
- FR-7.3: Pull sync processes debts after accounts (to resolve accountId FK) and debt payments after debts (to resolve debtId FK). If accountId cannot be resolved locally, skip the debt (log warning); it will be resolved on next sync after the account is pulled

### FR-8: Localization

- FR-8.1: All user-facing strings available in three languages: Russian, English, Kazakh
- FR-8.2: Default category names localized: "Долги"/"Debts"/"Қарыздар" and "Возврат долга"/"Debt Repayment"/"Қарыз қайтару"

### FR-9: Statistics TopBar Fix

- FR-9.1: Replace the plain Row header in StatisticsScreen with Material 3 TopAppBar
- FR-9.2: Title displays "Статистика" with proper status bar insets
- FR-9.3: CalendarFilterPill renders in the actions slot
- FR-9.4: Visual alignment matches other tab screens (Transactions, Accounts, Settings)

### FR-10: Firebase Crashlytics

- FR-10.1: Crash reports sent to Firebase Crashlytics in release builds only
- FR-10.2: CrashlyticsTree (Timber.Tree subclass) logs Warning, Error, and WTF level messages to Crashlytics
- FR-10.3: Crashlytics user ID set on sign-in, cleared on sign-out
- FR-10.4: Crash collection disabled in debug builds

### FR-11: Accessibility

- FR-11.1: All icon-only buttons and decorative images have contentDescription across all screens
- FR-11.2: FABs and icon buttons have Role.Button semantics
- FR-11.3: All interactive elements have minimum 48dp touch targets
- FR-11.4: Section headers in Settings use heading semantics
- FR-11.5: SupportedCurrencies currency display names localized to English and Kazakh (currently Russian-only)

### FR-12: Privacy Policy

- FR-12.1: Privacy policy page hosted on GitHub Pages or Firebase Hosting
- FR-12.2: Content covers: data collected, Firestore encryption, Google Sign-In usage, no third-party data sharing, data deletion on sign-out
- FR-12.3: "Политика конфиденциальности" link added to Settings → About section
- FR-12.4: Policy URL meets Play Store requirements

### FR-13: Version Discipline

- FR-13.1: versionCode set to 1, versionName set to "1.0.0" for the first public Play Store release (existing "1.1" and git tags v1.0.0–v1.1.9 were internal dev milestones; versionCode was never incremented, confirming no prior Play Store upload)
- FR-13.2: Version incrementing strategy documented (semantic versioning)

---

## Key Entities

### Debt
| Field | Description |
|-------|-------------|
| id | Auto-generated primary key |
| contactName | Name of the person (who owes / who is owed) |
| direction | LENT or BORROWED |
| totalAmount | Original debt amount |
| paidAmount | Computed: sum of all payment amounts |
| remainingAmount | Computed: totalAmount − paidAmount |
| currency | Currency code (matches account currency) |
| accountId | FK to the account used for the debt |
| note | Optional text note |
| createdAt | Timestamp of debt creation |
| status | Derived: ACTIVE if remainingAmount > 0, PAID_OFF otherwise |

### DebtPayment
| Field | Description |
|-------|-------------|
| id | Auto-generated primary key |
| debtId | FK to parent Debt |
| amount | Payment amount |
| date | Date the payment was made |
| note | Optional text note |
| transactionId | Nullable FK to linked Transaction |
| createdAt | Timestamp of payment recording |

---

## Non-Functional Requirements

- NFR-1: Debt list scrolls smoothly with 100+ debts (lazy loading)
- NFR-2: Crashlytics adds no observable startup latency in release builds
- NFR-3: All screens pass TalkBack navigation without dead ends or unlabeled elements
- NFR-4: Privacy policy page loads in under 3 seconds

---

## Scope & Boundaries

### In Scope
- Full debt lifecycle: create, partial payments, pay off, edit, delete
- Transaction auto-creation on debt payments
- Two new default categories for debt-related transactions
- Firestore sync for debts and debt payments
- Statistics TopBar fix for edge-to-edge rendering
- Crashlytics integration (release builds)
- Accessibility pass across all screens
- Privacy policy page creation and linking
- Version numbering for v1

### Out of Scope
- Debt reminders or notifications
- Interest calculation on debts
- Debt-to-debt transfers
- Multi-currency debts (debt currency matches account currency)
- Recurring debt payments (auto-scheduled)
- Analytics/statistics for debts (charts, trends)
- Release smoke testing (manual process, not automated in this spec)

---

## Dependencies

- Existing account and transaction infrastructure (for FK relationships and transaction creation)
- Firestore sync infrastructure (SyncManager, PullSyncUseCase) for debt sync
- Firebase Crashlytics SDK and Gradle plugin
- GitHub Pages or Firebase Hosting for privacy policy
- Budgets module pattern (3-module architecture to follow exactly)

---

## Assumptions

- Debt currency always matches the linked account's currency (no cross-currency debts)
- The existing database migration infrastructure (Room) will handle adding two new tables in a single migration (version 7 → 8)
- Privacy policy will be a static HTML page, not requiring a CMS or dynamic content
- Accessibility pass focuses on semantic annotations; visual design changes (contrast ratios, font sizes) are not in scope unless they fail WCAG AA
- "Release smoke test" is a manual QA checklist, not an automated test suite — documented but not implemented as code
- Debt amounts are always positive; the direction field determines whether it's lent or borrowed
- Summary card totals are computed from active (non-deleted) debts only

---

## Success Criteria

- Users can create, track, and pay off debts end-to-end in under 2 minutes per debt
- Debt payment recording with linked transaction creation completes in a single user flow (no screen switching)
- All four filter states (All, Lent, Borrowed, Paid Off) correctly partition the debt list
- Statistics screen header is fully visible below the system status bar on all supported API levels
- App crashes in production are captured and visible in the Crashlytics console within 5 minutes
- Every screen in the app is navigable via TalkBack with all interactive elements labeled
- Privacy policy is accessible via Settings and meets Play Store content policy requirements
- App ships with version 1.0.0 (versionCode=1) clearly displayed in the About section
