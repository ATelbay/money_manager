# Implementation Plan: 018-debts-v1-readiness

**Created**: 2026-04-09
**Spec**: [spec.md](spec.md)
**Research**: [research.md](research.md)
**Data Model**: [data-model.md](data-model.md)
**Branch**: `018-debts-v1-readiness`

---

## Technical Context

| Concern | Resolution |
|---------|-----------|
| Module pattern | Follow budgets exactly: domain + data + presentation, convention plugins |
| DB migration | Single MIGRATION_7_8 with two CREATE TABLE statements |
| Sync pattern | Per-id Mutex, encrypted DTOs, push/pull with last-write-wins |
| Strings | Code-based AppStrings in core/ui, three locales (RU/EN/KZ) |
| Navigation | Type-safe @Serializable destinations, composable<> blocks in NavHost |
| Crashlytics | Add to Firebase BOM, CrashlyticsTree for release, userId via LoginSyncOrchestrator |
| Privacy policy | Static HTML on GitHub Pages |
| Statistics fix | Replace Row with TopAppBar in StatisticsScreen.kt |

---

## Implementation Phases

### Phase 1: Core Infrastructure (Data Layer)

Foundation that all other phases depend on.

#### 1.1 Domain Models (`core/model`)
- Create `Debt.kt`, `DebtPayment.kt`, `DebtDirection.kt`, `DebtStatus.kt`
- Follow `Budget.kt` pattern: pure data classes, computed display fields

#### 1.2 Database Entities & DAOs (`core/database`)
- Create `DebtEntity.kt` with sync fields, FK to accounts, indexes on accountId + remoteId
- Create `DebtPaymentEntity.kt` with sync fields, FK to debts + transactions, indexes on debtId + remoteId
- Create `DebtDao.kt` — full CRUD + sync methods (observeAll, getById, insert, update, softDeleteById, getPendingSync, getDeletedWithRemoteId, upsertSync, clearRemoteIds, getByRemoteId)
- Create `DebtPaymentDao.kt` — same pattern, plus `observeByDebtId(debtId)`, `sumAmountByDebtId(debtId)` (for computing paidAmount)
- Register both entities in `MoneyManagerDatabase` (version → 8), add abstract DAO functions
- Add DAO `@Provides` functions in `DatabaseModule.kt`

#### 1.3 Database Migration
- Create `Migration_7_8.kt` — two CREATE TABLE + four CREATE INDEX statements (see data-model.md)
- Also INSERT the two new default categories ("Долги" and "Возврат долга") so existing users receive them on upgrade (DefaultCategories.all() only runs on fresh install)
- Register in `DatabaseModule.kt` migration list

#### 1.4 Default Categories
- Add "Долги" (Expense) and "Возврат долга" (Income) to `DefaultCategories.kt`
- Icon: `MoneyOff` for Долги, `Payments` for Возврат долга
- Color: red (0xFFEF4444) for expense, green (0xFF22C55E) for income

#### 1.5 Firestore DTOs & Mappers (`core/firestore`)
- Create `DebtDto.kt`, `DebtPaymentDto.kt` — encrypt sensitive fields (contactName, totalAmount, amount, note)
- Create `DebtDtoMapper.kt`, `DebtPaymentDtoMapper.kt` — entity↔dto with encryption/decryption
- Extend `FirestoreDataSource` interface: `pushDebt`, `pullDebts`, `pushDebtPayment`, `pullDebtPayments`, `deleteDebt`, `deleteDebtPayment`
- Implement in `FirestoreDataSourceImpl` — Firestore paths: `users/{userId}/debts/{remoteId}`, `users/{userId}/debt_payments/{remoteId}`

---

### Phase 2: Domain & Data Modules (Debts Business Logic)

#### 2.1 Domain Module (`domain/debts`)
- Create module: `build.gradle.kts` with `moneymanager.android.library` + `moneymanager.android.hilt`
- `DebtRepository` interface:
  - `observeAll(): Flow<List<Debt>>`
  - `observeById(id: Long): Flow<Debt?>`
  - `getById(id: Long): Debt?`
  - `save(debt: Debt): Long`
  - `delete(id: Long)`
- `DebtPaymentRepository` interface (or include in DebtRepository):
  - `observeByDebtId(debtId: Long): Flow<List<DebtPayment>>`
  - `save(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long`
  - `delete(id: Long)`
- Use cases:
  - `GetDebtsUseCase` — observe all debts with computed paidAmount/remainingAmount/status
  - `GetDebtWithPaymentsUseCase` — observe single debt + its payments
  - `SaveDebtUseCase` — validate + delegate to repository
  - `DeleteDebtUseCase` — soft delete + sync
  - `AddDebtPaymentUseCase` — save payment, optionally create transaction (Income or Expense based on direction), link transactionId
  - `DeleteDebtPaymentUseCase` — soft delete payment + sync (does NOT delete linked transaction)

#### 2.2 Data Module (`data/debts`)
- Create module: `build.gradle.kts` with `moneymanager.android.library` + `moneymanager.android.hilt`
- `DebtRepositoryImpl` (`@Singleton`):
  - Inject: `DebtDao`, `DebtPaymentDao`, `AccountDao`, `SyncManager`
  - `observeAll()`: combine debt entities with payment sums + account names
  - `save()`: insert/update pattern from BudgetRepositoryImpl, call `syncManager.syncDebt(id)`
  - `delete()`: softDeleteById + syncManager
- `DebtMapper.kt` — DebtEntity + payments sum + account → Debt domain model
- `DebtPaymentMapper.kt` — DebtPaymentEntity ↔ DebtPayment
- `DebtDataModule.kt` — `@Binds` for repository interfaces
- Transaction creation logic lives in `DebtPaymentRepositoryImpl.save()`:
  - LENT payment → Income transaction, category "Возврат долга", description "Возврат долга: {contactName}"
  - BORROWED payment → Expense transaction, category "Долги", description "Погашение долга: {contactName}"
  - Look up category by name from CategoryDao
  - Insert transaction via TransactionDao, get transactionId, store in DebtPayment

#### 2.3 Sync Integration (`data/sync`)
- `SyncManager`: add `debtMutexes`, `debtPaymentMutexes` (ConcurrentHashMap<Long, Mutex>)
- Add `syncDebt(id: Long)`, `syncDebtPayment(id: Long)` — same pattern as `syncBudget`
- Extend `pushAllPending()` with debts + debt_payments sections
- Extend `clearSyncMetadata()` with `debtDao.clearRemoteIds()` + `debtPaymentDao.clearRemoteIds()`
- `PullSyncUseCase`: add `pullDebts(userId)` after `pullAccounts`, `pullDebtPayments(userId)` after `pullDebts`
  - Debts: resolve `accountRemoteId` → local accountId via accountDao.getByRemoteId
  - DebtPayments: resolve `debtRemoteId` → local debtId, `transactionRemoteId` → local transactionId

---

### Phase 3: Presentation (Debts UI)

#### 3.1 Module Setup (`presentation/debts`)
- Create module: `build.gradle.kts` with `moneymanager.android.feature`
- Dependencies: `domain:debts`, `core:model`, `core:ui`

#### 3.2 Debt List Screen
- `DebtListState`: debts (ImmutableList<Debt>), isLoading, selectedFilter (ALL/LENT/BORROWED/PAID_OFF), totalLent, totalBorrowed
- `DebtListViewModel`: observe debts via GetDebtsUseCase, compute filtered list + summary totals, handle delete
- `DebtListScreen`:
  - TopAppBar with "Долги" title + back arrow
  - Summary card (total lent vs total borrowed)
  - Filter chips row (Все / Мне должны / Я должен / Погашенные)
  - LazyColumn of debt cards: contactName, direction badge (green/red), totalAmount, LinearProgressIndicator, remaining
  - SwipeToDismissBox for delete with confirmation dialog
  - FAB → show DebtEditBottomSheet
  - Empty state composable
- `DebtListRoute`: hiltViewModel + collectAsStateWithLifecycle

#### 3.3 Debt Detail Screen
- `DebtDetailState`: debt (Debt?), payments (ImmutableList<DebtPayment>), isLoading, showPaymentSheet, showEditSheet
- `DebtDetailViewModel`:
  - Observe debt + payments via GetDebtWithPaymentsUseCase
  - `addPayment(amount, date, note, createTransaction)` via AddDebtPaymentUseCase
  - `deletePayment(id)` via DeleteDebtPaymentUseCase
  - `deleteDebt()` via DeleteDebtUseCase
  - `saveDebt(debt)` via SaveDebtUseCase (for edits, with direction-change warning logic)
- `DebtDetailScreen`:
  - TopAppBar with contactName, edit + delete actions
  - Debt info card (direction, amount, account, date, note)
  - Progress section (LinearProgressIndicator + "Выплачено X из Y")
  - "Погашен" badge when fully paid
  - Payments LazyColumn (amount, date, note per item, swipe to delete)
  - "Записать платёж" button → PaymentBottomSheet
  - PaymentBottomSheet: amount field (pre-filled remainingAmount), date picker, note, "Создать транзакцию" checkbox, overpayment notice
- `DebtEditBottomSheet` (shared between list + detail):
  - contactName field, direction chips, amount field, account selector, date picker, note
  - Direction change warning dialog (when editing debt with payments)
  - Validation: non-empty name, amount > 0, account selected
- `DebtDetailRoute`: hiltViewModel with SavedStateHandle for debt id

#### 3.4 Localization (`core/ui`)
- Add debt string fields to `AppStrings` data class:
  - debts, debtsTitle, iLent, iBorrowed, paidOff, addDebt, addPayment, contactName, totalAmount, remainingAmount, debtDirection, recordPayment, createTransaction, debtPaidOff, noDebts, totalLent, totalBorrowed, debtRepayment, debtPaymentStr, overpaymentNotice, directionChangeWarning, deletePaymentsConfirm
- Populate in `RussianStrings`, `EnglishStrings`, `KazakhStrings`

---

### Phase 4: Navigation & Settings Wiring

#### 4.1 Gradle Configuration
- `settings.gradle.kts`: include `:domain:debts`, `:data:debts`, `:presentation:debts`
- `app/build.gradle.kts`: add `implementation(projects.data.debts)`, `implementation(projects.presentation.debts)`

#### 4.2 Navigation (`app`)
- `Destinations.kt`: add `@Serializable data object DebtList`, `@Serializable data class DebtDetail(val id: Long)`
- `MoneyManagerNavHost.kt`: wire `composable<DebtList>` → `DebtListRoute(onDebtClick, onBack)`, `composable<DebtDetail>` → `DebtDetailRoute(onBack)`
- Settings: add `onDebtsClick` callback in SettingsRoute → SettingsScreen, wire to `navController.navigate(DebtList)`

#### 4.3 Settings Row (`presentation/settings`)
- Add "Долги" `SettingRow` in General section (after Recurring row)
- Icon: `Icons.Default.AccountBalanceWallet`, color: `Color(0xFFF59E0B)` (amber)
- Thread `onDebtsClick` through SettingsRoute → SettingsScreen

---

### Phase 5: Statistics TopBar Fix

#### 5.1 Replace Row with TopAppBar
- File: `presentation/statistics/.../StatisticsScreen.kt`
- Remove the plain `Row` topBar (lines ~198-219)
- Replace with: `TopAppBar(title = { Text(s.statisticsTitle, ...) }, actions = { CalendarFilterPill(label = pillLabel, onClick = { showMonthPicker = true }) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))`
- Preserve testTag `"statistics:header"` via Modifier on TopAppBar

---

### Phase 6: Production Readiness

#### 6.1 Firebase Crashlytics
- `gradle/libs.versions.toml`: add `firebase-crashlytics` library under BOM, add crashlytics Gradle plugin
- `app/build.gradle.kts`: apply crashlytics plugin, add dependency
- Create `CrashlyticsTree.kt` (in app module or core/common):
  - Extends `Timber.Tree`, overrides `log()` — for priority >= WARN, log to `FirebaseCrashlytics.getInstance().log()` and `recordException()` for ERROR/WTF
- `MoneyManagerApp.onCreate()`: add `else` branch for `!BuildConfig.DEBUG` → plant CrashlyticsTree, set `setCrashlyticsCollectionEnabled(true)` (false for debug)
- `LoginSyncOrchestrator`: set `FirebaseCrashlytics.getInstance().setUserId(user.userId)` on sign-in, `setUserId("")` on sign-out

#### 6.2 Accessibility Pass
- Audit all screens for missing `contentDescription` on icon-only buttons (IconButton, FloatingActionButton)
- Add `Role.Button` semantics where missing on interactive elements
- Ensure 48dp minimum touch targets (Material 3 defaults handle most cases)
- Add `semantics { heading() }` to section headers in SettingsScreen
- Localize `SupportedCurrencies` display names to EN/KZ in the currency model/enum

#### 6.3 Privacy Policy
- Create `docs/privacy-policy.html` (or `docs/privacy-policy/index.html` for GitHub Pages)
- Content: data collected (transactions, categories, accounts), Firestore encryption (Tink), Google Sign-In usage, no third-party sharing, data deletion process on sign-out
- Three languages: RU primary, EN, KZ
- Add "Политика конфиденциальности" link in Settings → About section (open via `Intent(Intent.ACTION_VIEW, Uri.parse(url))`)
- Configure GitHub Pages deployment if not already set up

#### 6.4 Version Discipline
- `app/build.gradle.kts`: set `versionCode = 1`, `versionName = "1.0.0"`
- Document versioning strategy: semantic versioning (MAJOR.MINOR.PATCH), versionCode increments with each release

---

## Dependency Graph

```
Phase 1 (Core Infrastructure)
  ├── 1.1 Domain Models
  ├── 1.2 DB Entities & DAOs (depends on 1.1)
  ├── 1.3 Migration (depends on 1.2)
  ├── 1.4 Default Categories
  └── 1.5 Firestore DTOs (depends on 1.1)

Phase 2 (Domain & Data) — depends on Phase 1
  ├── 2.1 Domain Module (depends on 1.1)
  ├── 2.2 Data Module (depends on 1.2, 2.1)
  └── 2.3 Sync Integration (depends on 1.5, 2.2)

Phase 3 (Presentation) — depends on Phase 2
  ├── 3.1 Module Setup
  ├── 3.2 Debt List Screen (depends on 2.1, 3.4)
  ├── 3.3 Debt Detail Screen (depends on 2.1, 3.4)
  └── 3.4 Localization

Phase 4 (Navigation & Settings) — depends on Phase 3
  ├── 4.1 Gradle Config
  ├── 4.2 Navigation
  └── 4.3 Settings Row

Phase 5 (Statistics Fix) — INDEPENDENT, can run in parallel with any phase

Phase 6 (Production Readiness) — INDEPENDENT of Phases 1-4
  ├── 6.1 Crashlytics — independent
  ├── 6.2 Accessibility — best after Phase 3 (to include debt screens)
  ├── 6.3 Privacy Policy — independent
  └── 6.4 Version Discipline — independent
```

---

## Parallelization Opportunities

- **Phase 5** (Statistics fix) can run fully in parallel with everything else
- **Phase 6.1** (Crashlytics), **6.3** (Privacy Policy), **6.4** (Version) can run in parallel with Phases 1-4
- **Phase 6.2** (Accessibility) should run after Phase 3 to cover new debt screens
- Within Phase 1: steps 1.1, 1.4 are independent; 1.2 depends on 1.1; 1.3 depends on 1.2; 1.5 depends on 1.1
- Within Phase 3: 3.4 (localization) is independent of 3.2/3.3 and should come first

---

## Critical Files to Modify

| File | Phase | Changes |
|------|-------|---------|
| `core/model/src/.../Debt.kt` (NEW) | 1.1 | Domain model |
| `core/model/src/.../DebtPayment.kt` (NEW) | 1.1 | Domain model |
| `core/model/src/.../DebtDirection.kt` (NEW) | 1.1 | Enum |
| `core/model/src/.../DebtStatus.kt` (NEW) | 1.1 | Enum |
| `core/database/.../entity/DebtEntity.kt` (NEW) | 1.2 | Room entity |
| `core/database/.../entity/DebtPaymentEntity.kt` (NEW) | 1.2 | Room entity |
| `core/database/.../dao/DebtDao.kt` (NEW) | 1.2 | DAO |
| `core/database/.../dao/DebtPaymentDao.kt` (NEW) | 1.2 | DAO |
| `core/database/.../MoneyManagerDatabase.kt` | 1.2 | Add entities + DAOs, bump version to 8 |
| `core/database/.../di/DatabaseModule.kt` | 1.2 | Add DAO providers + migration |
| `core/database/.../migration/Migration_7_8.kt` (NEW) | 1.3 | CREATE TABLE statements |
| `core/database/.../DefaultCategories.kt` | 1.4 | Add 2 new categories |
| `core/firestore/.../dto/DebtDto.kt` (NEW) | 1.5 | Firestore DTO |
| `core/firestore/.../dto/DebtPaymentDto.kt` (NEW) | 1.5 | Firestore DTO |
| `core/firestore/.../mapper/DebtDtoMapper.kt` (NEW) | 1.5 | DTO mapper |
| `core/firestore/.../mapper/DebtPaymentDtoMapper.kt` (NEW) | 1.5 | DTO mapper |
| `core/firestore/.../FirestoreDataSource.kt` | 1.5 | Add push/pull methods |
| `core/firestore/.../FirestoreDataSourceImpl.kt` | 1.5 | Implement push/pull |
| `domain/debts/` (NEW MODULE) | 2.1 | Repository interface + 6 use cases |
| `data/debts/` (NEW MODULE) | 2.2 | Repository impl + mappers + DI |
| `data/sync/.../SyncManager.kt` | 2.3 | Add debt sync methods |
| `data/sync/.../PullSyncUseCase.kt` | 2.3 | Add pullDebts + pullDebtPayments |
| `presentation/debts/` (NEW MODULE) | 3.x | List + Detail screens, ViewModels, States |
| `core/ui/.../theme/AppStrings.kt` | 3.4 | Add ~20 debt string fields |
| `settings.gradle.kts` | 4.1 | Include 3 new modules |
| `app/build.gradle.kts` | 4.1 | Add dependencies + crashlytics plugin |
| `app/.../navigation/Destinations.kt` | 4.2 | Add DebtList + DebtDetail |
| `app/.../navigation/MoneyManagerNavHost.kt` | 4.2 | Wire debt composables + settings callback |
| `presentation/settings/.../SettingsScreen.kt` | 4.3 | Add "Долги" row |
| `presentation/settings/.../SettingsRoute.kt` | 4.3 | Add onDebtsClick callback |
| `presentation/statistics/.../StatisticsScreen.kt` | 5.1 | Replace Row with TopAppBar |
| `gradle/libs.versions.toml` | 6.1 | Add firebase-crashlytics |
| `app/.../MoneyManagerApp.kt` | 6.1 | Plant CrashlyticsTree |
| `app/.../CrashlyticsTree.kt` (NEW) | 6.1 | Timber.Tree subclass |
| `data/sync/.../LoginSyncOrchestrator.kt` | 6.1 | Set/clear Crashlytics userId |
| Multiple presentation modules | 6.2 | contentDescription, semantics, touch targets |
| `docs/privacy-policy.html` (NEW) | 6.3 | Privacy policy page |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Migration_7_8 fails on existing installs | HIGH | Test migration on real device with existing data before merge |
| Transaction creation in payment flow creates orphaned transactions if payment fails | MEDIUM | Use Room @Transaction annotation to wrap payment + transaction creation atomically |
| Firestore sync ordering (debts before payments) breaks if pull is interrupted | LOW | Each pull is idempotent; re-run resolves missing FKs |
| Accessibility audit scope creep across all screens | MEDIUM | Focus on icon buttons + FABs first; defer contrast/layout issues |
| Privacy policy content may not meet Play Store requirements | MEDIUM | Review against Play Store Developer Program Policies checklist before submission |
