# Feature Specification: Bugfixes and Sync Improvements

**Spec ID**: 017-bugfixes-and-sync
**Status**: Draft
**Created**: 2026-04-06
**Branch**: `017-bugfixes-and-sync`

---

## Overview

A bundle of 7 targeted fixes addressing sync reliability, UI polish, architecture naming, and multi-instance prevention. The fixes span four domains: data synchronization (account dedup, budget/recurring sync), UI behavior (exchange rate widget, chart empty state, balance flickering), code clarity (parser config rename), and app lifecycle (import flow single-instance).

---

## Clarifications

### Session 2026-04-06
- Q: Should `pendingSync` dirty flag (FR-6.6) be included or deferred? → A: Deferred to follow-up spec. Ship budget/recurring sync with same `remoteId IS NULL` pattern as existing entities.

---

## Problem Statement

1. **Exchange rate widget forces base/target roles** — users must mentally assign "base" and "target" when picking two currencies, but the natural expectation is that the more valuable currency appears on the left (e.g., "1 USD = 489.50 KZT"). The current BASE/TARGET chip selector is confusing.

2. **ParserConfig naming is ambiguous** — `ParserConfig` and `TableParserConfig` don't convey their distinct roles (regex-based parsing vs. table-based parsing). New contributors and AI tooling struggle to distinguish them.

3. **Chart shows misleading "1 KZT" axis when no data exists** — when all amounts for a period are zero, the chart renders with a "1" Y-axis label instead of communicating that there is no data.

4. **Account sync creates duplicates on first login** — offline-created accounts have no `remoteId`, so the pull phase can't match them against Firestore documents, inserting duplicates.

5. **Balance card flickers on screen load** — the balance animation restarts on every Flow emission (including intermediate null-to-real exchange rate transitions), causing visible flickering.

6. **Budgets and recurring transactions don't sync to Firestore** — both entities have `remoteId`/`isDeleted` fields but zero sync wiring. Additionally, auto-generated recurring transactions bypass sync entirely.

7. **PDF import can open a second app instance** — `singleTop` launch mode doesn't prevent cross-task duplication when a PDF is shared from another app while the main app is already running.

---

## User Scenarios & Testing

### Scenario 1: Exchange Rate Display (Fix #1)
**Actor**: User viewing exchange rates in Settings
- User opens Settings and sees the exchange rate widget
- User taps to pick two currencies (e.g., USD and KZT)
- The widget automatically displays the more valuable currency on the left: "1 USD = 489.50 KZT"
- No BASE/TARGET selector is shown — just two currency pickers
- **Acceptance**: The higher-value currency always appears as the "1 X" side regardless of selection order

### Scenario 2: Parser Config Rename (Fix #2)
**Actor**: Developer / maintainer
- Developer searches codebase for parser-related configuration
- Finds `RegexParserProfile` (regex-based configs) and `TableParserProfile` (table-based configs)
- JSON/Firestore serialization names remain unchanged — no data migration needed
- Room table name `parser_configs` remains unchanged
- **Acceptance**: All references updated; project compiles; no runtime behavior change

### Scenario 3: Chart Empty State (Fix #3)
**Actor**: User viewing statistics for a period with no transactions
- User navigates to Statistics screen
- Selects a date range with zero expenses/income
- Instead of a chart with "1" Y-axis label, sees a clear empty-state message ("No expenses/income for this period")
- **Acceptance**: Empty-state placeholder shown when all amounts are zero; chart hidden

### Scenario 4: Account Sync Deduplication (Fix #4)
**Actor**: User who created accounts offline, then signs in
- User creates accounts (e.g., "Kaspi Gold", "Cash") while offline
- User signs in for the first time
- Sync pushes local accounts to Firestore first, then pulls
- No duplicate accounts appear after sync completes
- **Acceptance**: Accounts matched by name+currency when `remoteId` is null; linked and upserted without duplication

### Scenario 5: Balance Animation (Fix #5)
**Actor**: User viewing the transaction list
- User opens the transaction list screen
- Balance loads and displays smoothly — no flickering or repeated animation restarts
- On subsequent balance changes (e.g., switching accounts), balance animates smoothly from old to new value
- **Acceptance**: First load snaps to value immediately; subsequent changes animate once; no intermediate flicker from null exchange rates

### Scenario 6: Budget & Recurring Transaction Sync (Fix #6)
**Actor**: User with budgets and recurring transactions who signs in on a new device
- User creates budgets and recurring transaction templates on Device A
- User signs in on Device B
- All budgets and recurring templates sync from Firestore
- Auto-generated transactions from recurring templates also sync
- **Acceptance**: Budgets and recurring transactions appear identically on both devices; no orphaned local-only records

### Scenario 7: Single App Instance on PDF Import (Fix #7)
**Actor**: User sharing a PDF bank statement from another app
- User opens a PDF in their file manager or banking app
- Shares it to Money Manager via the share sheet
- If Money Manager is already running, it reuses the existing instance
- After successful import, the app returns to the source app (if launched via share)
- **Acceptance**: Only one app instance exists at all times; share-launched sessions return to source app after import

---

## Functional Requirements

### FR-1: Exchange Rate Auto-Sort
- FR-1.1: When two currencies are selected for rate display, determine which has a higher KZT quote per 1 unit
- FR-1.2: Display the higher-value currency on the left as "1 X = Y.YY Z"
- FR-1.3: Remove the BASE/TARGET side selector from the currency picker
- FR-1.4: The app's display currency setting (used for balances and statistics) remains unchanged and independent of the rate widget

### FR-2: Parser Config Rename
- FR-2.1: Rename `ParserConfig` class to `RegexParserProfile`
- FR-2.2: Rename `TableParserConfig` class to `TableParserProfile`
- FR-2.3: Update all referencing files (providers, syncers, DAOs, entities, DTOs)
- FR-2.4: Preserve serialization names via annotations — no Firestore or JSON format change
- FR-2.5: Preserve Room table name `parser_configs` via explicit annotation

### FR-3: Chart Empty State
- FR-3.1: Detect when all chart data amounts are zero for the selected period
- FR-3.2: Hide the chart and show a localized empty-state message
- FR-3.3: Support both expense and income chart types

### FR-4: Account Sync Deduplication
- FR-4.1: Reorder sync phases: push pending local changes first, then pull from Firestore
- FR-4.2: When pulling accounts, if no match by `remoteId`, fall back to matching by name + currency (among non-deleted, unlinked accounts)
- FR-4.3: When a fallback match is found, link it by setting `remoteId` and upsert
- FR-4.4: Existing sync behavior for accounts with `remoteId` remains unchanged

### FR-5: Balance Flicker Fix
- FR-5.1: Suppress balance updates while exchange rate data is still loading (null)
- FR-5.2: Apply `distinctUntilChanged()` to prevent redundant emissions of identical balance values
- FR-5.3: On first balance load, snap to value immediately (no animation)
- FR-5.4: On subsequent balance changes, animate smoothly from previous to new value

### FR-6: Budget & Recurring Transaction Sync
- FR-6.1: Wire budgets to the existing sync infrastructure (push on create/update/delete, pull on login)
- FR-6.2: Wire recurring transaction templates to the existing sync infrastructure
- FR-6.3: Auto-generated transactions from recurring templates must go through sync (not bypass it)
- FR-6.4: Create Firestore DTOs for budgets and recurring transactions following the existing DTO pattern
- FR-6.5: Add pull logic for budgets and recurring transactions in the sync use case
- ~~FR-6.6~~: `pendingSync` dirty flag deferred to a follow-up spec (see `specs/017-bugfixes-and-sync/TODO.md`)

### FR-7: Single App Instance
- FR-7.1: Ensure only one instance of the app exists regardless of how it's launched
- FR-7.2: When launched via external share (ACTION_SEND/ACTION_VIEW) and import completes, return to the source app
- FR-7.3: Normal app launches (from launcher) are unaffected

---

## Success Criteria

1. **No duplicate accounts after first sync** — a user who creates N accounts offline sees exactly N accounts after signing in and syncing
2. **Balance loads without visible flicker** — on screen open, the balance value appears once and stays stable (no re-animation within the first 2 seconds)
3. **Chart clearly communicates empty state** — users seeing a zero-data period get a text message, not a misleading axis label
4. **Rate widget is self-explanatory** — users select two currencies and immediately see the intuitive "1 expensive = X.XX cheap" format without needing to understand base/target concepts
5. **Full entity sync coverage** — budgets and recurring transactions sync bidirectionally; auto-generated transactions are not orphaned locally
6. **Single instance guaranteed** — sharing a PDF while the app is open reuses the running instance; after import, share-launched sessions close
7. **Zero runtime regressions from rename** — parser config rename produces identical serialized output; all existing bank parsing continues to work

---

## Key Entities

| Entity | Change |
|--------|--------|
| Account | Fallback dedup query by name+currency during sync pull |
| Budget | New: Firestore sync wiring (push/pull), DTO |
| RecurringTransaction | New: Firestore sync wiring (push/pull), DTO |
| ParserConfig → RegexParserProfile | Rename (serialization preserved) |
| TableParserConfig → TableParserProfile | Rename (serialization preserved) |

---

## Scope & Boundaries

### In Scope
- All 7 fixes as described above
- Database migration for budget/recurring sync DTOs (if needed)
- Localized empty-state strings (RU, EN, KK)

### Out of Scope
- Multi-currency conversion logic (existing `baseCurrency` DataStore key behavior unchanged)
- New sync conflict resolution strategies beyond last-write-wins
- UI redesign of the currency picker beyond removing BASE/TARGET chips
- New bank parser profiles
- Changes to other launch modes or deep link handling beyond the PDF import fix

---

## Dependencies

- Existing sync infrastructure (`SyncManager`, `PullSyncUseCase`, Firestore DTOs) must be stable
- No Room migration needed — `pendingSync` is deferred (see TODO.md). Budget/recurring sync uses existing `remoteId`/`isDeleted` columns
- Firestore collections for `budgets` and `recurring_transactions` must be created (or auto-created on first write)

---

## Assumptions

- The "more valuable currency" is determined by comparing KZT quotes (the app's reference currency for exchange rates)
- Budget and recurring transaction Firestore sync follows the same last-write-wins pattern as accounts/categories/transactions
- `pendingSync` dirty flag is deferred — budget/recurring sync ships with the same `remoteId IS NULL` push pattern as existing entities
- Empty-state message text follows existing localization patterns (RU primary, EN, KK)
- `singleTask` launch mode is acceptable for this app's navigation patterns (no multi-window scenarios needed)

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| `singleTask` may affect back-stack behavior | Medium — users might experience unexpected back navigation | Test navigation flows thoroughly after change; verify `onNewIntent` handles all re-entry cases |
| Account dedup by name+currency could false-match | Low — unlikely two different accounts share same name+currency | Only match unlinked accounts (`remoteId IS NULL`); limit to non-deleted |
| Budget/recurring sync adds Firestore reads/writes | Low — small document count | Same batching as existing sync; no performance concern at typical scale |
| `pendingSync` migration on existing installs | Deferred | See TODO.md — will be addressed in a follow-up spec |
