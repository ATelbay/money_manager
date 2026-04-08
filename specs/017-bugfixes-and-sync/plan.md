# Implementation Plan: 017-bugfixes-and-sync

**Branch**: `017-bugfixes-and-sync`
**Spec**: [spec.md](spec.md)
**Research**: [research.md](research.md)
**Data Model**: [data-model.md](data-model.md)

---

## Technical Context

| Aspect | Details |
|--------|---------|
| Architecture | Layer-centric multi-module: domain/ → data/ → presentation/, core/ shared |
| DI | Hilt — `@Singleton` for SyncManager, PullSyncUseCase, LoginSyncOrchestrator |
| Database | Room 2.8.4 — DAOs, entities in `core:database` |
| Sync | SyncManager (push) + PullSyncUseCase (pull) + LoginSyncOrchestrator (orchestration) |
| Firestore | `users/{userId}/{collection}` — DTOs with `@DocumentId`, encrypted fields |
| UI | Jetpack Compose + Material 3, Vico charts, Navigation Compose (type-safe) |
| Localization | 3 locales: RU (primary), EN, KK — `AppStrings.kt` interface |

---

## Implementation Phases

### Phase 1: Parser Config Rename (FR-2) — Zero risk, pure refactor
**Rationale**: Do this first because it's a clean rename with no behavioral change. Gets the large diff out of the way before functional changes, avoiding merge conflicts with later phases.

**Tasks**:
1. Rename `ParserConfig` → `RegexParserProfile` in `core/remoteconfig/ParserConfig.kt`. Add `@SerialName("ParserConfig")` to preserve JSON. Rename `ParserConfigList` → `RegexParserProfileList`.
2. Rename `TableParserConfig` → `TableParserProfile` in `core/model/TableParserConfig.kt`. Add `@SerialName("TableParserConfig")`.
3. Rename `ParserConfigEntity` → `RegexParserProfileEntity`. Keep `@Entity(tableName = "parser_configs")`.
4. Rename `ParserConfigDao` → `RegexParserProfileDao`. Update `MoneyManagerDatabase.kt` and `DatabaseModule.kt`.
5. Rename `ParserConfigProvider` → `RegexParserProfileProvider`, `FirebaseParserConfigProvider` → `FirebaseRegexParserProfileProvider`, `ParserConfigSyncer` → `RegexParserProfileSyncer`. Update `RemoteConfigModule.kt` Hilt bindings.
6. Rename `ParserConfigFirestoreDto` → `RegexParserProfileFirestoreDto`. Update `FirestoreDataSource` and `FirestoreDataSourceImpl`.
7. Update all consuming files across `core/parser/`, `core/ai/`, `core/datastore/`, `domain/import/`, `presentation/import/`.
8. Rename test files: `ParserConfigTestFactory` → `RegexParserProfileTestFactory`, `ParserConfigSyncTest` → `RegexParserProfileSyncTest`. Update all test references.
9. **Verify**: `./gradlew assembleDebug test` — must compile, all existing parser tests pass.

**Files** (~40 files): See research.md "Fix 2" for complete list.

---

### Phase 2: Chart Empty State (FR-3) — Small, isolated UI fix

**Tasks**:
1. In `StatisticsViewModel.kt`, add `allAmountsZero: Boolean` to chart state. In `updateChartModel()`, after `if (points.isEmpty()) return`, add: `val allZero = amounts.all { it == 0.0 }`. If `allZero`, set flag in state and return without running chart model producer.
2. In `StatisticsScreen.kt` `UnifiedChartCard`, check `allAmountsZero` flag. When true, show localized placeholder text ("Нет расходов за период" / "Нет доходов за период") instead of `VicoBarChartSection`. Use existing `EmptyState` pattern or a simple `Text` centered in the card area.
3. Add localized strings to `AppStrings.kt` — RU, EN, KK variants for "No expenses for this period" / "No income for this period".
4. **Verify**: Manual test — select a date range with zero transactions, confirm placeholder shown instead of chart with "₸ 1" axis.

**Files**: `StatisticsViewModel.kt`, `StatisticsScreen.kt`, `AppStrings.kt`

---

### Phase 3: Exchange Rate Auto-Sort (FR-1) — UI behavior change

**Tasks**:
1. In `SettingsViewModel.kt` `buildRateDisplay()`: compare `rate.quotes[base.code]` vs `rate.quotes[target.code]`. Put the higher-quote currency on the left. Return `"1 ${expensive.code} = ${format(expensiveToKzt / cheapToKzt)} ${cheap.code}"`.
2. In `CurrencyPickerScreen.kt`: remove `SideChip` composable calls for BASE/TARGET (lines 126-142). Replace with two neutral currency display slots (e.g., the two currencies shown as tappable chips without "Base"/"Target" labels). Each chip navigates to the currency list for that slot.
3. In `CurrencyPickerSide.kt`: rename `BASE` → `FIRST`, `TARGET` → `SECOND` (or keep enum values if renaming causes nav serialization issues — test).
4. In `AppStrings.kt`: remove or simplify `baseCurrency` and `targetCurrency` string fields. Update `rateLabel` lambda if needed (it currently takes `base, target` params — may simplify to just "Exchange rate" since the display string itself is self-explanatory).
5. In `CurrencyPickerRoute.kt`: update `onSelect` branching to use renamed enum values. No structural change.
6. **Verify**: Select USD+KZT in either order → always shows "1 USD = X KZT". Select EUR+USD → shows "1 EUR = X USD" (EUR has higher KZT quote).

**Files**: `SettingsViewModel.kt`, `CurrencyPickerScreen.kt`, `CurrencyPickerSide.kt`, `CurrencyPickerRoute.kt`, `AppStrings.kt`, `Destinations.kt`

---

### Phase 4: Balance Flicker Fix (FR-5) — Targeted flow fix

**Tasks**:
1. In `TransactionListViewModel.kt`: add `.distinctUntilChanged()` on `dataFlow` (the inner 5-flow combine), before the outer combine with `filterFlow`.
2. In `TransactionListViewModel.kt`: inside the outer combine lambda, when computing balance for multi-currency accounts — if `exchangeRate == null`, skip balance update and retain the previous state's balance value. This prevents the null→value transition from triggering a re-emission.
3. In `BalanceCard.kt`: add `var isFirstEmission by remember { mutableStateOf(true) }`. In the `LaunchedEffect(balance)` block: if `isFirstEmission`, call `animatable.snapTo(balance)` and set `isFirstEmission = false`; else call `animatable.animateTo(balance, ...)`.
4. **Verify**: Open transaction list — balance appears instantly (no animation on first load). Switch accounts — balance animates smoothly once. No visible flicker during exchange rate refresh.

**Files**: `TransactionListViewModel.kt`, `BalanceCard.kt`

---

### Phase 5: Account Sync Deduplication (FR-4) — Sync logic fix

**Tasks**:
1. In `AccountDao.kt`: add `getByNameAndCurrency(name: String, currency: String): AccountEntity?` query — `SELECT * FROM accounts WHERE name = :name AND currency = :currency AND isDeleted = 0 AND remoteId IS NULL LIMIT 1`.
2. In `LoginSyncOrchestrator.runSync()`: reorder from `pull → pushPending → pushAll` to `pushPending → pull → pushAll`. This ensures offline accounts get `remoteId` before pull tries to match.
3. In `PullSyncUseCase.pullAccounts()`: after `val local = accountDao.getByRemoteId(dto.remoteId)` returns null, add fallback: `val fallback = accountDao.getByNameAndCurrency(decryptedName, dto.currency)`. If `fallback != null`, set `remoteId` on the fallback entity and proceed with upsert instead of insert.
4. **Verify**: Create accounts offline → sign in → verify no duplicates. Sign in on second device → verify accounts appear correctly.

**Files**: `AccountDao.kt`, `LoginSyncOrchestrator.kt`, `PullSyncUseCase.kt`

---

### Phase 6: Budget & Recurring Sync (FR-6) — Largest change, depends on Phase 5

**Rationale**: This is the biggest phase. It follows the exact patterns established by existing account/category/transaction sync. Depends on Phase 5 because it modifies the same sync orchestration code.

**Tasks**:

#### 6a: DAO sync queries
1. Add to `BudgetDao.kt`: `getByRemoteId`, `getPendingSync`, `getDeletedWithRemoteId`, `upsertSync`, `clearRemoteIds`, `softDeleteById`.
2. Add to `RecurringTransactionDao.kt`: same 6 queries.

#### 6b: Firestore DTOs and mappers
3. Create `BudgetDto.kt` in `core/firestore/dto/` — fields: `remoteId`, `categoryRemoteId`, `monthlyLimit` (encrypted), `createdAt`, `updatedAt`, `isDeleted`, `encryptionVersion`.
4. Create `BudgetDtoMapper.kt` in `core/firestore/mapper/` — `BudgetEntity.toDto()` and `BudgetDto.toEntity()` following `AccountDtoMapper` pattern. Encrypt `monthlyLimit`. Resolve `categoryRemoteId` ↔ `categoryId`.
5. Create `RecurringTransactionDto.kt` — fields: `remoteId`, `amount` (encrypted), `type`, `categoryRemoteId`, `accountRemoteId`, `note`, `frequency`, `startDate`, `endDate`, `dayOfMonth`, `dayOfWeek`, `lastGeneratedDate`, `isActive`, `createdAt`, `updatedAt`, `isDeleted`, `encryptionVersion`.
6. Create `RecurringTransactionDtoMapper.kt` — encrypt `amount`. Resolve both `categoryRemoteId` and `accountRemoteId`.

#### 6c: FirestoreDataSource
7. Add to `FirestoreDataSource` interface: `pushBudget`, `pullBudgets`, `pushRecurringTransaction`, `pullRecurringTransactions`.
8. Implement in `FirestoreDataSourceImpl` using collections `users/{userId}/budgets` and `users/{userId}/recurring_transactions`.

#### 6d: SyncManager extensions
9. Add `BudgetDao` and `RecurringTransactionDao` to `SyncManager` constructor (Hilt injection).
10. Add `syncBudget(id: Long): Job` and `syncRecurring(id: Long): Job` — fire-and-forget, same pattern as `syncAccount`.
11. Extend `pushAllPending()`: add budget and recurring blocks after transactions.
12. Extend `clearSyncMetadata()`: add `budgetDao.clearRemoteIds()` and `recurringDao.clearRemoteIds()`.

#### 6e: PullSyncUseCase extensions
13. Add `BudgetDao`, `RecurringTransactionDao`, and `AccountDao` to `PullSyncUseCase` constructor.
14. Add `pullBudgets(userId)` — same LWW pattern. Resolve `categoryRemoteId` → local `categoryId`. Order: after `pullCategories`.
15. Add `pullRecurringTransactions(userId)` — resolve both `categoryRemoteId` and `accountRemoteId`. Order: after `pullBudgets`, before `pullTransactions`.
16. Update `invoke()` order: `pullAccounts → pullCategories → pullBudgets → pullRecurringTransactions → pullTransactions`.

#### 6f: Repository wiring
17. `BudgetRepositoryImpl`: inject `SyncManager`. Call `syncManager.syncBudget(id)` after `save()` and `delete()`.
18. `RecurringTransactionRepositoryImpl`: inject `SyncManager`. Call `syncManager.syncRecurring(id)` after `save()`, `delete()`, and `toggleActive()`.

#### 6g: Verify
19. **Verify**: Create budget → check Firestore doc appears. Create recurring template → check Firestore doc. Sign in on fresh install → pull populates budgets and recurring templates. Auto-generated transactions sync via existing `pushAllPending`.

**Files**: `BudgetDao.kt`, `RecurringTransactionDao.kt`, `BudgetDto.kt` (new), `BudgetDtoMapper.kt` (new), `RecurringTransactionDto.kt` (new), `RecurringTransactionDtoMapper.kt` (new), `FirestoreDataSource.kt`, `FirestoreDataSourceImpl.kt`, `SyncManager.kt`, `PullSyncUseCase.kt`, `BudgetRepositoryImpl.kt`, `RecurringTransactionRepositoryImpl.kt`

---

### Phase 7: Import Single Instance (FR-7) — Small manifest + lifecycle change

**Tasks**:
1. In `AndroidManifest.xml`: change `android:launchMode="singleTop"` to `android:launchMode="singleTask"`.
2. In `PendingNavigationManager`: add `launchedFromExternal: Boolean` field (or `MutableStateFlow<Boolean>`). Set to `true` when `NavigationAction.OpenImport` is enqueued from an external intent (`ACTION_SEND` or `ACTION_VIEW`).
3. In `MainActivity.kt`: in `onCreate()`, check if `intent.action` is `ACTION_SEND` or `ACTION_VIEW` — if so, mark external launch. Same in `onNewIntent()`.
4. After import completes (in the import navigation consumer in `MainActivity.kt`), if `launchedFromExternal` is true, call `activity.finish()` to return to source app. Reset the flag.
5. **Verify**: Share PDF from file manager → import → returns to file manager. Open app normally → import via in-app flow → stays in app. App already open + share PDF → reuses instance (no second Activity).

**Files**: `AndroidManifest.xml`, `PendingNavigationManager.kt`, `MainActivity.kt`

---

## Phase Dependency Graph

```
Phase 1 (rename)     ──┐
Phase 2 (chart)      ──┤── independent, can run in parallel
Phase 3 (rate widget) ─┤
Phase 4 (balance)    ──┤
Phase 7 (import)     ──┘
                        │
Phase 5 (account dedup) ── must precede Phase 6 (shared sync code)
                        │
Phase 6 (budget/recurring sync) ── depends on Phase 5
```

Phases 1–4, 7 are fully independent of each other and of Phases 5–6.
Phase 5 must complete before Phase 6 (they modify overlapping files: LoginSyncOrchestrator, PullSyncUseCase, SyncManager).

---

## Risk Mitigation

| Risk | Phase | Mitigation |
|------|-------|------------|
| `singleTask` breaks back navigation | 7 | Test: launcher → settings → share PDF → import → back button returns to settings, not launcher |
| Rename breaks serialization | 1 | `@SerialName` annotations; run all parser tests; compare JSON output before/after |
| Account dedup false-matches | 5 | Query restricted to `remoteId IS NULL AND isDeleted = 0`; only unlinked accounts eligible |
| Budget/recurring pull FK resolution fails | 6 | Pull order ensures accounts + categories exist first; skip DTOs with unresolvable FKs (same as transaction pull) |
| Balance snap-vs-animate logic edge case | 4 | `isFirstEmission` flag is per-composition lifetime; recomposition resets correctly |

---

## Verification Plan

| Phase | Verification |
|-------|-------------|
| 1 | `./gradlew assembleDebug test` — compile + all parser tests pass |
| 2 | Manual: zero-data period shows placeholder, not chart |
| 3 | Manual: USD+KZT in either order → "1 USD = X KZT" |
| 4 | Manual: open transaction list → no flicker; switch accounts → smooth animation |
| 5 | Manual: create accounts offline → sign in → no duplicates |
| 6 | Manual: create budget + recurring → check Firestore; fresh install sign-in → data appears |
| 7 | Manual: share PDF → import → returns to source app; normal launch unaffected |
| All | `./gradlew assembleDebug test lint` — full build passes |
