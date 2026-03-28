# Research: Pre-iOS Port Feature Bundle

**Branch**: `016-pre-ios-features` | **Date**: 2026-03-27

## R1: Room Migration Strategy (single vs. multiple)

**Decision**: Single migration 4→5 adding both `recurring_transactions` and `budgets` tables.

**Rationale**: Both tables are new (CREATE TABLE), have no interdependencies, and ship in the same branch. A single migration is simpler to maintain and test. The existing pattern (`MIGRATION_2_3`, `MIGRATION_3_4`) uses one file per version bump.

**Alternatives considered**:
- Two migrations (4→5 for recurring, 5→6 for budgets): Unnecessarily complex for tables shipping together. Would be appropriate if features were on separate branches.

## R2: Period Enum — Handling of existing `ALL` value

**Decision**: Keep `Period.ALL` in the enum but remove it from the visible chip row. The chip order becomes: `CURRENT_MONTH`, `TODAY`, `WEEK`, `MONTH`, `YEAR`. `ALL` remains usable programmatically (e.g., for CSV export) but has no chip.

**Rationale**: Removing an enum value risks breaking existing code that references `Period.ALL`. The user's spec explicitly defines the chip order without ALL, but doesn't request its removal from the enum.

**Alternatives considered**:
- Remove `ALL` entirely: Risk of compilation errors in code paths that reference it. Unnecessary scope creep.
- Add `ALL` chip back: Contradicts the spec's explicit chip order.

## R3: DatePicker Implementation — Month Picker Mode

**Decision**: Build a custom month grid composable (3×4 grid of month buttons + year navigation arrows). Use Material 3 `DateRangePicker` for the range mode. Both modes live in a single dialog with tab switching.

**Rationale**: Material 3 does not have a dedicated month-only picker component. The statistics screen already has a month picker pattern in `presentation/statistics/` that can be referenced. A custom grid is lightweight and fits the M3 design language.

**Alternatives considered**:
- Use `DatePicker` in year-month mode: M3 DatePicker doesn't support a month-only selection mode natively.
- Third-party library: Adds unnecessary dependency for a simple UI.

## R4: Recurring Transaction Generation — Timing and Trigger

**Decision**: Generate pending transactions in `MoneyManagerApp` (or `MainActivity.onCreate`) via a coroutine launched on `Dispatchers.IO`. The generation runs after database init but does not block UI rendering — the transaction list will reactively update via Flow when generation completes.

**Rationale**: Blocking the UI thread on startup is unacceptable. Since the ViewModel observes transactions via `Flow<List<TransactionEntity>>`, generated transactions will appear automatically once committed to Room. Running on IO with a Room `withTransaction` block ensures atomicity.

**Alternatives considered**:
- WorkManager periodic task: Over-engineered for a local app. Generation only matters when the user is actually viewing the app.
- Blocking generation before NavHost: Would delay app startup noticeably.

## R5: CSV Export — FileProvider Setup

**Decision**: Add a `FileProvider` in `AndroidManifest.xml` with a `file_paths.xml` resource pointing to the cache directory. Use `context.cacheDir` for temporary CSV files. Share via `Intent.ACTION_SEND` with `application/octet-stream` or `text/csv` MIME type.

**Rationale**: The app currently has no FileProvider. Android requires `content://` URIs (not `file://`) for sharing files with other apps. The cache directory is automatically cleaned by the system when storage is low.

**Alternatives considered**:
- `MediaStore` insertion: More complex, and CSV files don't belong in media storage.
- Direct `file://` URI: Fails on Android 7+ due to `FileUriExposedException`.

## R6: Budget Spending Query — DAO vs. UseCase Calculation

**Decision**: Add a DAO query `getExpenseSumByCategory(categoryId: Long, startDate: Long, endDate: Long): Flow<Double?>` to `TransactionDao` to compute spending efficiently in SQL. The `GetBudgetsWithSpendingUseCase` combines budget data with spending queries.

**Rationale**: SQL aggregation (`SUM(amount)`) is more efficient than loading all transactions into memory and summing in Kotlin, especially as transaction count grows. The existing `TransactionDao` already has complex queries with JOINs.

**Alternatives considered**:
- Load all transactions and filter/sum in UseCase: Wasteful for large datasets. Poor separation of concerns — the database should handle aggregation.

## R7: Convention Plugin for New Modules

**Decision**: Domain modules use `moneymanager.android.library` + `moneymanager.android.hilt`. Data modules use the same pair plus Room dependencies. Presentation modules use `moneymanager.android.feature` (bundles Compose + Hilt + lifecycle + navigation).

**Rationale**: Matches the exact pattern used by existing `domain/transactions`, `data/transactions`, and `presentation/transactions` modules. Convention plugins ensure consistent configuration.

## R8: Recurring Day-of-Month Overflow

**Decision**: For monthly recurrings on day 29–31, clamp to the last day of the month using `LocalDate.of(year, month, 1).withDayOfMonth(min(dayOfMonth, month.length(isLeap)))`. This matches user expectation that a "31st of every month" recurring generates on Feb 28/29, Apr 30, etc.

**Rationale**: Standard behavior in calendar applications (Google Calendar, iOS Reminders). The spec's edge case section explicitly calls for this behavior.

## R9: AppStrings — New String Count Estimate

**Decision**: ~30 new string properties across 3 language instances. Grouped by feature:
- Period: 1 (currentMonth)
- DatePicker: 3 (selectDates, month, range)
- Recurring: 9 (recurring, daily, weekly, monthly, yearly, startDate, endDate, nextDate, recurringTransactions)
- Budgets: 7 (budgets, limit, spent, remaining, overBudget, newBudget, budgetLabel lambda)
- CSV: 3 (exportCsv, exportComplete, exportError)
- Settings rows: 3 (recurring, budgets, export labels — may overlap with above)
- Period rename: 1 (thirtyDays replacing month label)

**Rationale**: Each localization string must exist in all 3 languages (ru/en/kk). The AppStrings data class pattern requires adding properties plus values in all 3 instances.
