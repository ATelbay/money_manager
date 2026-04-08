# Tasks: 017-bugfixes-and-sync

**Feature**: Bugfixes and Sync Improvements
**Branch**: `017-bugfixes-and-sync`
**Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md) | **Data Model**: [data-model.md](data-model.md)

---

## User Story Mapping

| Story | Description | FR | Plan Phase |
|-------|-------------|-----|------------|
| US1 | Exchange Rate Auto-Sort | FR-1 | Phase 3 |
| US2 | Parser Config Rename | FR-2 | Phase 1 |
| US3 | Chart Empty State | FR-3 | Phase 2 |
| US4 | Account Sync Deduplication | FR-4 | Phase 5 |
| US5 | Balance Flicker Fix | FR-5 | Phase 4 |
| US6 | Budget & Recurring Transaction Sync | FR-6 | Phase 6 |
| US7 | Single App Instance on Import | FR-7 | Phase 7 |

---

## Phase 1: Parser Config Rename [US2]

**Goal**: Rename `ParserConfig` → `RegexParserProfile` and `TableParserConfig` → `TableParserProfile` across the entire codebase while preserving serialization and Room table compatibility.

**Test criteria**: `./gradlew assembleDebug test` passes. All existing parser tests pass. No runtime behavior change.

- [ ] T001 [P] [US2] Rename `ParserConfig` → `RegexParserProfile` and `ParserConfigList` → `RegexParserProfileList` in `core/remoteconfig/src/main/java/.../ParserConfig.kt`. Add `@SerialName("ParserConfig")` to preserve JSON serialization. Rename the file to `RegexParserProfile.kt`.
- [ ] T002 [P] [US2] Rename `TableParserConfig` → `TableParserProfile` in `core/model/src/main/java/.../TableParserConfig.kt`. Add `@SerialName("TableParserConfig")`. Rename the file to `TableParserProfile.kt`.
- [ ] T003 [P] [US2] Rename `ParserConfigEntity` → `RegexParserProfileEntity` in `core/database/src/main/java/.../entity/ParserConfigEntity.kt`. Keep `@Entity(tableName = "parser_configs")`. Rename file to `RegexParserProfileEntity.kt`.
- [ ] T004 [P] [US2] Rename `ParserConfigDao` → `RegexParserProfileDao` in `core/database/src/main/java/.../dao/ParserConfigDao.kt`. Update references in `core/database/.../MoneyManagerDatabase.kt` and `core/database/.../di/DatabaseModule.kt`. Rename file to `RegexParserProfileDao.kt`.
- [ ] T005 [P] [US2] Rename provider/syncer classes: `ParserConfigProvider` → `RegexParserProfileProvider`, `FirebaseParserConfigProvider` → `FirebaseRegexParserProfileProvider`, `ParserConfigSyncer` → `RegexParserProfileSyncer` in `core/remoteconfig/src/main/java/.../`. Update Hilt bindings in `core/remoteconfig/.../di/RemoteConfigModule.kt`. Rename all 3 files.
- [ ] T006 [P] [US2] Rename `ParserConfigFirestoreDto` → `RegexParserProfileFirestoreDto` in `core/firestore/src/main/java/.../dto/ParserConfigFirestoreDto.kt`. Update references in `core/firestore/.../datasource/FirestoreDataSource.kt` and `FirestoreDataSourceImpl.kt`. Rename file.
- [ ] T007 [US2] Update all consuming files that import old class names across `core/parser/`, `core/ai/`, `core/datastore/`, `domain/import/`, `presentation/import/`. Full list in research.md "Fix 2". This includes: `StatementParser.kt`, `RegexStatementParser.kt`, `TableStatementParser.kt`, `BankDetector.kt`, `GeminiService.kt`, `GeminiServiceImpl.kt`, `UserPreferences.kt`, `ParseStatementUseCase.kt`, `SubmitParserCandidateUseCase.kt`, `ImportViewModel.kt`.
- [ ] T008 [US2] Rename test files and update test references: `ParserConfigTestFactory` → `RegexParserProfileTestFactory`, `ParserConfigSyncTest` → `RegexParserProfileSyncTest`. Update all test files in `core/parser/src/test/`, `domain/import/src/test/`, `presentation/import/src/test/`, `core/ai/src/test/`.
- [ ] T009 [US2] Verify: run `./gradlew assembleDebug test` — must compile and all parser tests pass with zero runtime behavior change.

---

## Phase 2: Chart Empty State [US3]

**Goal**: Show a localized empty-state placeholder when all chart amounts are zero, instead of rendering a misleading chart with "₸ 1" Y-axis.

**Test criteria**: Navigate to Statistics → select a period with zero transactions → see text placeholder, no chart.

- [ ] T010 [P] [US3] Add `allAmountsZero: Boolean = false` field to the chart state in `presentation/statistics/.../StatisticsViewModel.kt`. In `updateChartModel()`, after the existing `if (points.isEmpty()) return` guard, add: `val allZero = amounts.all { it == 0.0 }`. If `allZero`, set the flag in state and return without running chart model producer.
- [ ] T011 [P] [US3] Add localized empty-state strings to `core/ui/.../theme/AppStrings.kt` — add fields `chartNoExpenses` and `chartNoIncome` with RU ("Нет расходов за период" / "Нет доходов за период"), EN ("No expenses for this period" / "No income for this period"), KK variants.
- [ ] T012 [US3] In `presentation/statistics/.../StatisticsScreen.kt` `UnifiedChartCard` composable, check `allAmountsZero` flag. When true, show the localized placeholder text (expense or income variant based on selected type) centered in the card area instead of `VicoBarChartSection`.

---

## Phase 3: Exchange Rate Auto-Sort [US1]

**Goal**: Remove base/target currency concept from rate widget. Auto-sort so the more expensive currency (higher KZT quote) always appears on the left: "1 USD = 489.50 KZT".

**Test criteria**: Select USD+KZT in either order → always "1 USD = X KZT". Select EUR+USD → "1 EUR = X USD".

- [ ] T013 [US1] In `presentation/settings/.../SettingsViewModel.kt` `buildRateDisplay()` (lines 245-256): compare `rate.quotes[base.code]` vs `rate.quotes[target.code]`. Put the higher-quote currency on the left. Return `"1 ${expensive.code} = ${format(expensiveToKzt / cheapToKzt)} ${cheap.code}"`. Handle edge case where quotes are equal (alphabetical fallback). If either currency's KZT quote is null/unavailable, fall back to alphabetical order by currency code.
- [ ] T014 [P] [US1] In `presentation/settings/.../CurrencyPickerSide.kt`: rename enum values `BASE` → `FIRST`, `TARGET` → `SECOND`. This enum is `@Serializable` for nav — verify nav serialization still works.
- [ ] T015 [P] [US1] In `core/ui/.../theme/AppStrings.kt`: remove or simplify `baseCurrency` and `targetCurrency` string fields. Simplify `rateLabel` lambda — it can become a simple string "Курс валют" / "Exchange rate" / "Валюта бағамы" since the display string is self-explanatory.
- [ ] T016 [US1] In `presentation/settings/.../CurrencyPickerScreen.kt`: remove `SideChip` composable calls for BASE/TARGET (lines 126-142). Replace with two neutral tappable currency chips (no "Base"/"Target" labels). Update the pair summary display (line 120) to remove directional arrow. Update `selected` logic (line 69) to use renamed enum values.
- [ ] T017 [US1] In `presentation/settings/.../CurrencyPickerRoute.kt`: update `onSelect` branching to use renamed `FIRST`/`SECOND` enum values. Call `setBaseCurrency`/`setTargetCurrency` accordingly (DataStore keys unchanged). Update `app/.../navigation/Destinations.kt` default to `CurrencyPickerSide.FIRST`.

---

## Phase 4: Balance Flicker Fix [US5]

**Goal**: Eliminate balance card flickering on screen load by deduplicating flow emissions and fixing animation behavior.

**Test criteria**: Open transaction list → balance appears instantly (no animation). Switch accounts → smooth single animation. No flicker during exchange rate refresh.

- [ ] T018 [US5] In `presentation/transactions/.../list/TransactionListViewModel.kt`: add `.distinctUntilChanged()` on `dataFlow` (the inner 5-flow combine at lines 91-105), before the outer combine with `filterFlow`. `DataParams` is a data class so structural equality works.
- [ ] T019 [US5] In `presentation/transactions/.../list/TransactionListViewModel.kt`: inside the outer combine lambda, when computing balance for multi-currency accounts — if `exchangeRate == null`, skip balance update and retain the previous state's balance value to prevent null→value transition flicker. On initial state (no previous balance), show the raw base-currency balance without conversion rather than blocking display.
- [ ] T020 [US5] In `core/ui/.../components/BalanceCard.kt`: add `var isFirstEmission by remember { mutableStateOf(true) }`. In the `LaunchedEffect(balance)` block (lines 73-85): if `isFirstEmission`, call `animatable.snapTo(balance)` and set `isFirstEmission = false`; else call `animatable.animateTo(balance, tween(...))`.

---

## Phase 5: Account Sync Deduplication [US4]

**Goal**: Prevent duplicate accounts after first sync by reordering sync phases and adding name+currency fallback matching.

**Test criteria**: Create accounts offline → sign in → exactly N accounts (no duplicates). Second device sign-in → accounts correct.

**Dependency**: Must complete before Phase 6 (shared sync code in LoginSyncOrchestrator, PullSyncUseCase).

- [ ] T021 [US4] In `core/database/.../dao/AccountDao.kt`: add `@Query("SELECT * FROM accounts WHERE name = :name AND currency = :currency AND isDeleted = 0 AND remoteId IS NULL LIMIT 1") suspend fun getByNameAndCurrency(name: String, currency: String): AccountEntity?`.
- [ ] T022 [US4] In `data/sync/.../LoginSyncOrchestrator.kt` `runSync()`: reorder from `pull → pushPending → pushAll` to `pushPending → pull → pushAll`. Move `syncManager.pushAllPending()` before `pullSyncUseCase(userId)`.
- [ ] T023 [US4] In `data/sync/.../PullSyncUseCase.kt` `pullAccounts()`: after `val local = accountDao.getByRemoteId(dto.remoteId)` returns null, add fallback: decrypt account name from DTO, then `val fallback = accountDao.getByNameAndCurrency(decryptedName, dto.currency)`. If `fallback != null`, copy `dto.remoteId` onto the fallback entity via `accountDao.update(fallback.copy(remoteId = dto.remoteId))`, then proceed with upsert (not insert).

---

## Phase 6: Budget & Recurring Transaction Sync [US6]

**Goal**: Wire budgets and recurring transactions to existing Firestore sync infrastructure. Push on create/update/delete, pull on login.

**Test criteria**: Create budget → Firestore doc appears. Create recurring template → Firestore doc appears. Fresh install sign-in → budgets and recurring templates pull correctly. Auto-generated transactions sync via existing pushAllPending.

**Dependency**: Requires Phase 5 (US4) complete — overlapping files: LoginSyncOrchestrator, PullSyncUseCase, SyncManager.

### 6a: DAO sync queries

- [ ] T024 [P] [US6] Add sync queries to `core/database/.../dao/BudgetDao.kt`: `getByRemoteId(remoteId: String): BudgetEntity?`, `getPendingSync(): List<BudgetEntity>` (WHERE remoteId IS NULL AND isDeleted = 0), `getDeletedWithRemoteId(): List<BudgetEntity>` (WHERE isDeleted = 1 AND remoteId IS NOT NULL), `upsertSync(budgets: List<BudgetEntity>)` (INSERT OR REPLACE), `clearRemoteIds()` (UPDATE budgets SET remoteId = NULL), `softDeleteById(id: Long, updatedAt: Long)`.
- [ ] T025 [P] [US6] Add sync queries to `core/database/.../dao/RecurringTransactionDao.kt`: same 6 queries as BudgetDao — `getByRemoteId`, `getPendingSync`, `getDeletedWithRemoteId`, `upsertSync`, `clearRemoteIds`, `softDeleteById`.

### 6b: Firestore DTOs and mappers

- [ ] T026 [P] [US6] Create `core/firestore/src/main/java/.../dto/BudgetDto.kt` with fields: `@DocumentId remoteId`, `categoryRemoteId`, `monthlyLimit` (encrypted String), `createdAt`, `updatedAt`, `isDeleted`, `encryptionVersion`. All fields must have defaults for Firestore deserialization.
- [ ] T027 [P] [US6] Create `core/firestore/src/main/java/.../mapper/BudgetDtoMapper.kt` with `BudgetEntity.toDto(cipher, categoryRemoteId)` and `BudgetDto.toEntity(localId, cipher, localCategoryId)`. Follow `AccountDtoMapper` pattern: encrypt `monthlyLimit`, handle null cipher gracefully, return null on decrypt failure.
- [ ] T028 [P] [US6] Create `core/firestore/src/main/java/.../dto/RecurringTransactionDto.kt` with fields: `@DocumentId remoteId`, `amount` (encrypted), `type`, `categoryRemoteId`, `accountRemoteId`, `note`, `frequency`, `startDate`, `endDate`, `dayOfMonth`, `dayOfWeek`, `lastGeneratedDate`, `isActive`, `createdAt`, `updatedAt`, `isDeleted`, `encryptionVersion`.
- [ ] T029 [P] [US6] Create `core/firestore/src/main/java/.../mapper/RecurringTransactionDtoMapper.kt` with entity↔dto mapping. Encrypt `amount`. Resolve `categoryRemoteId` ↔ `categoryId` and `accountRemoteId` ↔ `accountId`. Return null on decrypt failure.

### 6c: FirestoreDataSource

- [ ] T030 [US6] Add to `core/firestore/.../datasource/FirestoreDataSource.kt` interface: `suspend fun pushBudget(userId: String, dto: BudgetDto)`, `suspend fun pullBudgets(userId: String): List<BudgetDto>`, `suspend fun pushRecurringTransaction(userId: String, dto: RecurringTransactionDto)`, `suspend fun pullRecurringTransactions(userId: String): List<RecurringTransactionDto>`.
- [ ] T031 [US6] Implement the 4 new methods in `core/firestore/.../datasource/FirestoreDataSourceImpl.kt` using Firestore collections `users/{userId}/budgets` and `users/{userId}/recurring_transactions`. Follow existing `pushAccount`/`pullAccounts` pattern.

### 6d: SyncManager extensions

- [ ] T032 [US6] In `data/sync/.../SyncManager.kt`: add `BudgetDao` and `RecurringTransactionDao` to constructor (Hilt `@Inject`). Add `syncBudget(id: Long): Job` and `syncRecurring(id: Long): Job` fire-and-forget methods following `syncAccount` pattern. Add per-entity mutex maps (`budgetMutexes`, `recurringMutexes`).
- [ ] T033 [US6] In `data/sync/.../SyncManager.kt` `pushAllPending()`: add budget block (fetch `budgetDao.getPendingSync()`, assign remoteId, push to Firestore) and recurring block after existing transaction block. Also push deleted budgets/recurring with remoteId (tombstones).
- [ ] T034 [US6] In `data/sync/.../SyncManager.kt` `clearSyncMetadata()`: add `budgetDao.clearRemoteIds()` and `recurringDao.clearRemoteIds()`.

### 6e: PullSyncUseCase extensions

- [ ] T035 [US6] In `data/sync/.../PullSyncUseCase.kt`: add `BudgetDao`, `RecurringTransactionDao` to constructor. Add private `pullBudgets(userId: String)` using same LWW pattern as `pullAccounts`. Resolve `categoryRemoteId` → local `categoryId` via `categoryDao.getByRemoteId()`. Skip (do not insert) budgets whose `categoryRemoteId` cannot be resolved to a local `categoryId` — log a warning. Same pattern as transaction pull FK resolution.
- [ ] T036 [US6] In `data/sync/.../PullSyncUseCase.kt`: add private `pullRecurringTransactions(userId: String)`. Resolve both `categoryRemoteId` and `accountRemoteId` to local IDs. Skip DTOs with unresolvable FKs.
- [ ] T037 [US6] In `data/sync/.../PullSyncUseCase.kt` `invoke()`: update pull order to `pullAccounts → pullCategories → pullBudgets → pullRecurringTransactions → pullTransactions`.

### 6f: Repository wiring

- [ ] T038 [P] [US6] In `data/budgets/.../BudgetRepositoryImpl.kt`: inject `SyncManager`. Call `syncManager.syncBudget(id)` after `save()` (using returned ID) and after `delete()`.
- [ ] T039 [P] [US6] In `data/recurring/.../RecurringTransactionRepositoryImpl.kt`: inject `SyncManager`. Call `syncManager.syncRecurring(id)` after `save()`, `delete()`, and `toggleActive()`. Also verify that `generateDueTransactions()` inserts transactions with `remoteId = null` so they are picked up by `pushAllPending()`.

### 6g: Build verification

- [ ] T040 [US6] Verify: run `./gradlew assembleDebug test` — must compile with all new sync wiring. Manually test: create budget → check Firestore doc, create recurring → check Firestore doc, fresh install sign-in → data pulls correctly.

---

## Phase 7: Single App Instance on Import [US7]

**Goal**: Prevent duplicate app instances when PDF is shared from another app. Return to source app after import completes.

**Test criteria**: Share PDF from file manager → import → returns to file manager. Normal app launch unaffected. App already open + share PDF → reuses instance.

- [ ] T041 [US7] In `app/src/main/AndroidManifest.xml` line 21: change `android:launchMode="singleTop"` to `android:launchMode="singleTask"`.
- [ ] T042 [US7] In `app/.../navigation/PendingNavigationManager.kt`: add `private val _launchedFromExternal = MutableStateFlow(false)` and public `val launchedFromExternal: StateFlow<Boolean>`. Add `fun markExternalLaunch()` setter and `fun clearExternalLaunch()` resetter.
- [ ] T043 [US7] In `app/.../MainActivity.kt`: in `onCreate()`, check if `intent?.action` is `Intent.ACTION_SEND` or `Intent.ACTION_VIEW` — if so, call `pendingNavigationManager.markExternalLaunch()`. Add same check in `onNewIntent()`.
- [ ] T044 [US7] In `app/.../MainActivity.kt`: in the `LaunchedEffect` that consumes `NavigationAction.OpenImport` (lines 250-263), after `pendingNavigationManager.consume()`, observe import completion. When import screen pops back (or on a separate effect), if `launchedFromExternal` is true, call `finish()` and `clearExternalLaunch()`.

---

## Phase 8: Final Verification

- [ ] T045 Run full build and test suite: `./gradlew assembleDebug test lint`. All 7 fixes must compile together with no conflicts.

---

## Dependencies

```
T001-T008 (US2 rename) ──┐
T010-T012 (US3 chart)  ──┤
T013-T017 (US1 rate)   ──┤── all independent, parallelizable
T018-T020 (US5 balance) ─┤
T041-T044 (US7 import) ──┘
                          │
T021-T023 (US4 dedup)  ──── must complete before US6
                          │
T024-T040 (US6 sync)   ──── depends on US4
                          │
T045 (final verify)     ──── depends on all
```

## Parallel Execution Plan

**Wave 1** (5 independent stories, can run simultaneously):
- Agent A: US2 (T001-T009) — parser config rename
- Agent B: US3 (T010-T012) ��� chart empty state
- Agent C: US1 (T013-T017) — exchange rate auto-sort
- Agent D: US5 (T018-T020) — balance flicker fix
- Agent E: US7 (T041-T044) — import single instance

**Wave 2** (after Wave 1, or can start as soon as sync files are free):
- Agent F: US4 (T021-T023) — account sync dedup

**Wave 3** (after US4 complete):
- Agent G: US6 (T024-T040) — budget & recurring sync
  - Within US6: T024-T029 are parallelizable (DAOs + DTOs + mappers)
  - T030-T031 depend on DTOs (T026-T029)
  - T032-T037 depend on DAOs + DTOs
  - T038-T039 depend on SyncManager extensions (T032-T034)

**Wave 4**:
- T045 — final verification

## Implementation Strategy

**MVP**: Wave 1 delivers 5 of 7 fixes independently. Each is a shippable increment.

**Incremental delivery order** (by risk, lowest first):
1. US2 (rename) — zero runtime risk, pure refactor
2. US3 (chart) — small isolated UI fix
3. US5 (balance) — targeted 2-file fix
4. US1 (rate widget) — UI behavior change, easily reversible
5. US7 (import) — manifest change, test thoroughly
6. US4 (account dedup) — sync logic, test with real Firestore
7. US6 (budget/recurring sync) — largest scope, most integration points

---

## Summary

| Metric | Value |
|--------|-------|
| Total tasks | 45 |
| US1 (rate widget) | 5 tasks |
| US2 (rename) | 9 tasks |
| US3 (chart) | 3 tasks |
| US4 (account dedup) | 3 tasks |
| US5 (balance) | 3 tasks |
| US6 (budget/recurring sync) | 17 tasks |
| US7 (import) | 4 tasks |
| Final verification | 1 task |
| Parallelizable tasks | 20 (marked [P]) |
| Max parallel agents (Wave 1) | 5 |
