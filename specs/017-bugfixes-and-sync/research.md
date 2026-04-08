# Research: 017-bugfixes-and-sync

## Decision Log

### Fix 1: Exchange Rate Auto-Sort

**Decision**: Modify `buildRateDisplay()` to auto-sort by KZT quote; remove BASE/TARGET UI concept from currency picker.
**Rationale**: The `ExchangeRate.quotes` map already stores KZT-per-unit for every currency. Comparing `quotes[currencyA]` vs `quotes[currencyB]` trivially determines which is "more expensive." DataStore keys (`base_currency`, `target_currency`) stay as storage — they just lose their directional semantics.
**Key files**:
- `SettingsViewModel.kt:245-256` — `buildRateDisplay()`: change `baseToKzt / targetToKzt` to always put higher-quote currency on left
- `CurrencyPickerScreen.kt:126-142` — remove `SideChip` for BASE/TARGET; replace with neutral "Currency 1"/"Currency 2" or just two tap targets
- `CurrencyPickerRoute.kt:14` — `initialActiveSide` parameter remains (needed to know which slot is being edited) but rename enum values
- `CurrencyPickerSide.kt` — rename `BASE/TARGET` → `FIRST/SECOND` or similar
- `AppStrings.kt:182-183` — remove or rename `baseCurrency`/`targetCurrency` labels
- `Destinations.kt` — `CurrencyPicker` data class keeps `activeSide` param
- `SettingsState.kt` — `baseCurrency`/`targetCurrency` fields stay (internal naming, not user-facing)
**Alternatives**: Could rename DataStore keys — rejected because it would require migration and the keys are internal.

### Fix 2: ParserConfig Rename

**Decision**: Rename classes only; preserve serialization and Room table names via annotations.
**Rationale**: `@SerialName` on data class preserves JSON compatibility. `@Entity(tableName = "parser_configs")` preserves Room table.
**Scope**: ~25 production files + ~15 test files reference `ParserConfig`; ~12 production + ~4 test files reference `TableParserConfig`.
**Key constraint**: `ParserConfigFirestoreDto` serialization name stays unchanged. `ParserConfigEntity` table stays `parser_configs`. Cloud function `promote-candidate.ts` references Firestore collection name (not Kotlin class) — no change needed.

### Fix 3: Chart Empty State

**Decision**: Add zero-amount check in `updateChartModel()` and show empty-state placeholder in `UnifiedChartCard`.
**Rationale**: Current `DynamicRangeProvider.getMaxY()` returns `1.0` when all bars are zero — chart renders with misleading Y-axis. The screen-level empty state only triggers when `displayedExpensesByCategory` is empty (no categories), not when categories exist but all amounts are zero.
**Implementation**: In `StatisticsViewModel.updateChartModel()` (after existing `if (points.isEmpty()) return`), add `if (amounts.all { it == 0.0 }) { set allZero flag; return }`. In `UnifiedChartCard`, check the flag and render localized placeholder text instead of `VicoBarChartSection`.

### Fix 4: Account Sync Deduplication

**Decision**: Reorder sync to pushPending→pull→pushAll; add name+currency fallback match in pullAccounts.
**Rationale**: Pushing first ensures offline-created accounts get `remoteId` assigned before pull tries to match. Fallback match by name+currency handles the edge case where push succeeded on Firestore but local `remoteId` wasn't set (crash mid-sync), or where accounts were created on two devices.
**Key changes**:
- `LoginSyncOrchestrator.runSync()`: reorder from pull→pushPending→pushAll to pushPending→pull→pushAll
- `AccountDao`: add `getByNameAndCurrency(name, currency)` query with `remoteId IS NULL AND isDeleted = 0 LIMIT 1`
- `PullSyncUseCase.pullAccounts()`: if `getByRemoteId` returns null, try `getByNameAndCurrency`; if found, link by setting `remoteId` then upsert

### Fix 5: Balance Flickering

**Decision**: Add `distinctUntilChanged()` on dataFlow; snap first balance value, animate subsequent.
**Rationale**: The 5-flow combine in `TransactionListViewModel` re-emits on every DataStore change (3 separate `.map` calls from one `dataStore.data`). `observeExchangeRateUseCase` also re-emits on cache→network transition. No deduplication anywhere in the chain.
**Key changes**:
- `TransactionListViewModel`: add `.distinctUntilChanged()` on `dataFlow` (before outer combine). `DataParams` is a data class — structural equality works.
- `TransactionListViewModel`: guard balance computation — if exchangeRate is null and multi-currency accounts exist, skip balance update (emit previous value)
- `BalanceCard.kt`: track `isFirstEmission` with `remember { mutableStateOf(true) }`. First time: `snapTo(balance)`. Subsequent: `animateTo(balance)`.

### Fix 6: Budget & Recurring Transaction Sync

**Decision**: Follow existing sync pattern exactly. No `pendingSync` flag (deferred).
**Rationale**: Both `BudgetEntity` and `RecurringTransactionEntity` already have `remoteId` and `isDeleted` fields. The DTO pattern (encrypt sensitive fields, carry FK as `remoteId` strings, `@DocumentId`) is well-established.
**What to build**:
- DAO queries: `getByRemoteId`, `getPendingSync`, `getDeletedWithRemoteId`, `upsertSync`, `clearRemoteIds` (for both BudgetDao and RecurringTransactionDao)
- DTOs: `BudgetDto` (with `categoryRemoteId`), `RecurringTransactionDto` (with `categoryRemoteId` + `accountRemoteId`)
- Mappers: entity↔dto for both, following `AccountDtoMapper` pattern
- `FirestoreDataSource`/`Impl`: `pushBudget`, `pullBudgets`, `pushRecurringTransaction`, `pullRecurringTransactions`
- `SyncManager`: add `syncBudget(id)`, `syncRecurring(id)`; extend `pushAllPending()` and `clearSyncMetadata()`
- `PullSyncUseCase`: add `pullBudgets(userId)` and `pullRecurringTransactions(userId)` in `invoke()`
- `BudgetRepositoryImpl`: inject SyncManager, call `syncBudget(id)` after save/delete
- `RecurringTransactionRepositoryImpl`: inject SyncManager, call `syncRecurring(id)` after save/delete/toggleActive
**FK resolution order in pull**: accounts → categories → budgets → recurring_transactions → transactions (budgets need categoryRemoteId; recurring needs both category + account)
**generateTransactionsAtomically**: Already inserts transactions with `remoteId=null` — they flow through `pushAllPending()` naturally. No change needed here.

### Fix 7: Import Single Instance

**Decision**: Change `singleTop` to `singleTask`; call `activity.finish()` after import when launched via external intent.
**Rationale**: `singleTop` only reuses Activity when it's at the top of the task. `singleTask` guarantees a single instance and routes all new intents through `onNewIntent()`. Trade-off: `singleTask` clears the back stack above the activity, but this is acceptable since Money Manager is a single-Activity Compose Navigation app.
**Key changes**:
- `AndroidManifest.xml:21`: `android:launchMode="singleTask"`
- Track whether launch was from external share (check `intent.action` in `onCreate`/`onNewIntent`)
- After import completes (success or cancel from share-launched session), call `activity.finish()` to return to source app
- `PendingNavigationManager`: may need a `launchedFromExternal` flag
