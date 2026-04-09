# Tasks: 018-debts-v1-readiness

**Feature**: Debts Feature + Statistics TopBar Fix + Production Readiness
**Branch**: `018-debts-v1-readiness`
**Created**: 2026-04-09

---

## User Stories

| ID | Story | Priority |
|----|-------|----------|
| US1 | Debts: Core data layer (models, entities, DAOs, migration, DTOs) | P1 |
| US2 | Debts: Domain & data modules (repository, use cases, sync) | P1 |
| US3 | Debts: Debt list screen with filtering | P1 |
| US4 | Debts: Debt detail screen with payments | P1 |
| US5 | Debts: Navigation, settings wiring, Gradle config | P1 |
| US6 | Statistics TopBar fix | P2 |
| US7 | Firebase Crashlytics integration | P2 |
| US8 | Accessibility pass | P3 |
| US9 | Privacy policy + version discipline | P3 |

---

## Phase 1: Setup

- [ ] T001 Add `:domain:debts`, `:data:debts`, `:presentation:debts` module includes in `settings.gradle.kts`
- [ ] T002 [P] Create `domain/debts/build.gradle.kts` with `moneymanager.android.library` + `moneymanager.android.hilt` convention plugins (follow `domain/budgets/build.gradle.kts`)
- [ ] T003 [P] Create `data/debts/build.gradle.kts` with `moneymanager.android.library` + `moneymanager.android.hilt` convention plugins (follow `data/budgets/build.gradle.kts`)
- [ ] T004 [P] Create `presentation/debts/build.gradle.kts` with `moneymanager.android.feature` convention plugin (follow `presentation/budgets/build.gradle.kts`)
- [ ] T005 Add `implementation(projects.data.debts)` and `implementation(projects.presentation.debts)` to `app/build.gradle.kts`

---

## Phase 2: Foundational — Core Models & Database (US1)

### Goal: Establish all data-layer infrastructure (models, entities, DAOs, migration, DTOs) so domain/data modules can build on top.

### Independent test criteria:
- Project compiles after all Phase 2 tasks
- DB migration 7→8 creates both tables with correct schema
- Default categories include "Долги" and "Возврат долга"

### Domain Models

- [ ] T006 [P] [US1] Create `DebtDirection.kt` enum (LENT, BORROWED) in `core/model/src/main/java/com/atelbay/money_manager/core/model/DebtDirection.kt`
- [ ] T007 [P] [US1] Create `DebtStatus.kt` enum (ACTIVE, PAID_OFF) in `core/model/src/main/java/com/atelbay/money_manager/core/model/DebtStatus.kt`
- [ ] T008 [P] [US1] Create `Debt.kt` data class in `core/model/src/main/java/com/atelbay/money_manager/core/model/Debt.kt` — fields: id, contactName, direction (DebtDirection), totalAmount, paidAmount, remainingAmount, currency, accountId, accountName, note, createdAt, status (DebtStatus). Follow `Budget.kt` pattern.
- [ ] T009 [P] [US1] Create `DebtPayment.kt` data class in `core/model/src/main/java/com/atelbay/money_manager/core/model/DebtPayment.kt` — fields: id, debtId, amount, date, note, transactionId (nullable), createdAt.

### Database Entities

- [ ] T010 [US1] Create `DebtEntity.kt` in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/DebtEntity.kt` — @Entity(tableName="debts"), FK to accounts ON DELETE CASCADE, indexes on accountId + remoteId, sync fields (remoteId, updatedAt, isDeleted). Follow `BudgetEntity.kt` pattern.
- [ ] T011 [US1] Create `DebtPaymentEntity.kt` in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/DebtPaymentEntity.kt` — @Entity(tableName="debt_payments"), FK to debts ON DELETE CASCADE + FK to transactions ON DELETE SET NULL, indexes on debtId + remoteId, sync fields.

### DAOs

- [ ] T012 [US1] Create `DebtDao.kt` in `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/DebtDao.kt` — CRUD: observeAll (Flow, filter isDeleted=0), getById, insert (returns Long), update, softDeleteById. Sync: getByRemoteId, getPendingSync, getDeletedWithRemoteId, upsertSync(List), clearRemoteIds. Follow `BudgetDao.kt` pattern exactly.
- [ ] T013 [US1] Create `DebtPaymentDao.kt` in `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/DebtPaymentDao.kt` — same CRUD+sync pattern as DebtDao, plus: observeByDebtId(debtId) Flow, getByDebtId(debtId) suspend, sumAmountByDebtId(debtId) returning Flow<Double?>, softDeleteByDebtId(debtId, updatedAt) for cascade soft-delete.

### Database Registration & Migration

- [ ] T014 [US1] Update `MoneyManagerDatabase.kt` — add DebtEntity + DebtPaymentEntity to entities array, bump version to 8, add abstract `debtDao(): DebtDao` and `debtPaymentDao(): DebtPaymentDao` functions.
- [ ] T015 [US1] Create `Migration_7_8.kt` in `core/database/src/main/java/com/atelbay/money_manager/core/database/migration/Migration_7_8.kt` — execSQL for CREATE TABLE debts + debt_payments + 4 CREATE INDEX statements (copy SQL from data-model.md). Also INSERT the two new default categories ("Долги" EXPENSE with icon=MoneyOff, color=0xFFEF4444, isDefault=1; "Возврат долга" INCOME with icon=Payments, color=0xFF22C55E, isDefault=1) so existing users receive them on upgrade.
- [ ] T016 [US1] Update `DatabaseModule.kt` — add `@Provides fun provideDebtDao(db): DebtDao` and `@Provides fun provideDebtPaymentDao(db): DebtPaymentDao`, add MIGRATION_7_8 to addMigrations() call.

### Default Categories

- [ ] T017 [P] [US1] Add two new default categories in `core/database/src/main/java/com/atelbay/money_manager/core/database/DefaultCategories.kt` — "Долги" (EXPENSE, icon="MoneyOff", color=0xFFEF4444) and "Возврат долга" (INCOME, icon="Payments", color=0xFF22C55E).

### Firestore DTOs & Mappers

- [ ] T018 [P] [US1] Create `DebtDto.kt` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/dto/DebtDto.kt` — fields: remoteId (@DocumentId), contactName (encrypted String), direction (String), totalAmount (encrypted String), currency, accountRemoteId, note (encrypted String), createdAt, updatedAt, isDeleted, encryptionVersion. Follow `BudgetDto.kt` pattern.
- [ ] T019 [P] [US1] Create `DebtPaymentDto.kt` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/dto/DebtPaymentDto.kt` — fields: remoteId, debtRemoteId, amount (encrypted), date, note (encrypted), transactionRemoteId, createdAt, updatedAt, isDeleted, encryptionVersion.
- [ ] T020 [US1] Create `DebtDtoMapper.kt` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/DebtDtoMapper.kt` — DebtEntity.toDto(fieldCipherHolder, accountRemoteId) encrypts contactName+totalAmount+note; DebtDto.toEntity(localId, fieldCipherHolder, localAccountId) decrypts with fallback for encryptionVersion=0. Follow `BudgetDtoMapper.kt` pattern.
- [ ] T021 [US1] Create `DebtPaymentDtoMapper.kt` in `core/firestore/src/main/java/com/atelbay/money_manager/core/firestore/mapper/DebtPaymentDtoMapper.kt` — same encrypt/decrypt pattern for amount+note fields, resolve debtRemoteId→localDebtId and transactionRemoteId→localTransactionId.
- [ ] T022 [US1] Extend `FirestoreDataSource.kt` interface with: `pushDebt(userId, DebtDto)`, `pullDebts(userId): List<DebtDto>`, `deleteDebt(userId, remoteId)`, `pushDebtPayment(userId, DebtPaymentDto)`, `pullDebtPayments(userId): List<DebtPaymentDto>`, `deleteDebtPayment(userId, remoteId)`.
- [ ] T023 [US1] Implement debt methods in `FirestoreDataSourceImpl.kt` — Firestore paths: `users/{userId}/debts/{remoteId}` and `users/{userId}/debt_payments/{remoteId}`. Follow existing budget push/pull implementation pattern (manual doc.data parsing for backwards compat).

---

## Phase 3: Domain & Data Modules (US2)

### Goal: Repository interfaces, implementations, use cases, mappers, sync integration — full business logic layer.

### Independent test criteria:
- GetDebtsUseCase returns debts with computed paidAmount/remainingAmount/status
- AddDebtPaymentUseCase creates linked transaction when checkbox enabled
- SyncManager.syncDebt pushes/pulls debt data to Firestore

### Domain Module

- [ ] T024 [US2] Create `DebtRepository.kt` interface in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/repository/DebtRepository.kt` — observeAll(): Flow<List<Debt>>, observeById(id): Flow<Debt?>, getById(id): Debt?, save(debt: Debt): Long, delete(id: Long).
- [ ] T025 [US2] Create `DebtPaymentRepository.kt` interface in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/repository/DebtPaymentRepository.kt` — observeByDebtId(debtId): Flow<List<DebtPayment>>, save(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long, delete(id: Long), deleteAllByDebtId(debtId: Long).
- [ ] T026 [P] [US2] Create `GetDebtsUseCase.kt` in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/usecase/GetDebtsUseCase.kt` — operator fun invoke(): Flow<List<Debt>>, delegates to debtRepository.observeAll().
- [ ] T027 [P] [US2] Create `GetDebtWithPaymentsUseCase.kt` in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/usecase/GetDebtWithPaymentsUseCase.kt` — operator fun invoke(id: Long): Flow<Pair<Debt?, List<DebtPayment>>>, combines debtRepository.observeById + debtPaymentRepository.observeByDebtId.
- [ ] T028 [P] [US2] Create `SaveDebtUseCase.kt` in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/usecase/SaveDebtUseCase.kt` — suspend operator fun invoke(debt: Debt): Long, validates contactName non-empty + amount > 0, delegates to repository.
- [ ] T029 [P] [US2] Create `DeleteDebtUseCase.kt` in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/usecase/DeleteDebtUseCase.kt` — suspend operator fun invoke(id: Long), delegates to debtRepository.delete(id).
- [ ] T030 [P] [US2] Create `AddDebtPaymentUseCase.kt` in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/usecase/AddDebtPaymentUseCase.kt` — suspend operator fun invoke(payment: DebtPayment, createTransaction: Boolean, debt: Debt): Long. Delegates to debtPaymentRepository.save which handles transaction creation logic.
- [ ] T031 [P] [US2] Create `DeleteDebtPaymentUseCase.kt` in `domain/debts/src/main/java/com/atelbay/money_manager/domain/debts/usecase/DeleteDebtPaymentUseCase.kt` — suspend operator fun invoke(id: Long), delegates to debtPaymentRepository.delete(id). Does NOT delete linked transaction.

### Data Module

- [ ] T032 [US2] Create `DebtMapper.kt` in `data/debts/src/main/java/com/atelbay/money_manager/data/debts/mapper/DebtMapper.kt` — DebtEntity.toDomain(paidAmount: Double, accountName: String): Debt (computes remainingAmount, derives status). Debt.toEntity(): DebtEntity.
- [ ] T033 [US2] Create `DebtPaymentMapper.kt` in `data/debts/src/main/java/com/atelbay/money_manager/data/debts/mapper/DebtPaymentMapper.kt` — DebtPaymentEntity.toDomain(): DebtPayment and reverse.
- [ ] T034 [US2] Create `DebtRepositoryImpl.kt` in `data/debts/src/main/java/com/atelbay/money_manager/data/debts/repository/DebtRepositoryImpl.kt` — @Singleton, inject DebtDao + DebtPaymentDao + AccountDao + SyncManager. observeAll(): combine debt entities with sumAmountByDebtId + account names. save(): insert/update pattern from BudgetRepositoryImpl, call syncManager.syncDebt(id). delete(): softDeleteById + syncManager.
- [ ] T035 [US2] Create `DebtPaymentRepositoryImpl.kt` in `data/debts/src/main/java/com/atelbay/money_manager/data/debts/repository/DebtPaymentRepositoryImpl.kt` — @Singleton, inject DebtPaymentDao + TransactionDao + CategoryDao + SyncManager. save(): if createTransaction=true, look up category by name ("Возврат долга" for LENT / "Долги" for BORROWED), insert Transaction, get transactionId; insert DebtPayment with transactionId; call syncManager.syncDebtPayment(id). Use @Transaction for atomicity. delete(): softDeleteById, recalculates on next observe. deleteAllByDebtId(): soft-delete all payments for a debt.
- [ ] T036 [US2] Create `DebtDataModule.kt` in `data/debts/src/main/java/com/atelbay/money_manager/data/debts/di/DebtDataModule.kt` — @Module @InstallIn(SingletonComponent::class) abstract class with @Binds for DebtRepository and DebtPaymentRepository.

### Sync Integration

- [ ] T037 [US2] Update `SyncManager.kt` in `data/sync/` — add debtMutexes + debtPaymentMutexes (ConcurrentHashMap<Long, Mutex>), add syncDebt(id) and syncDebtPayment(id) methods following syncBudget pattern. Extend pushAllPending() with debts + debt_payments. Extend clearSyncMetadata() with debtDao.clearRemoteIds() + debtPaymentDao.clearRemoteIds().
- [ ] T038 [US2] Update `PullSyncUseCase.kt` in `data/sync/` — add pullDebts(userId) after pullAccounts (resolve accountRemoteId → local accountId), add pullDebtPayments(userId) after pullDebts (resolve debtRemoteId → local debtId, transactionRemoteId → local transactionId). Follow existing pullBudgets pattern.

---

## Phase 4: Localization & Strings (US3/US4 prerequisite)

### Goal: All debt-related UI strings available before building screens.

- [ ] T039 Add ~20 debt string fields to `AppStrings` data class in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/AppStrings.kt` — debts, debtsTitle, iLent, iBorrowed, paidOff, addDebt, addPayment, contactName, totalAmount, remainingAmount, debtDirection, recordPayment, createTransaction, debtPaidOff, noDebts, totalLent, totalBorrowed, debtRepayment, debtPaymentStr, overpaymentNotice, directionChangeWarning, deletePaymentsConfirm, iGaveDebt, iTookDebt, paidXOfY. Populate in RussianStrings, EnglishStrings, KazakhStrings.

---

## Phase 5: Debt List Screen (US3)

### Goal: Users can view, filter, and delete debts from a scrollable list accessed via Settings.

### Independent test criteria:
- DebtListScreen renders debts with direction badges and progress bars
- Filter chips correctly partition debts by direction and status
- Summary card shows correct totals for lent/borrowed
- Swipe-to-dismiss triggers soft delete with confirmation

- [ ] T040 [US3] Create `DebtFilter.kt` enum in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/list/DebtFilter.kt` — ALL, LENT, BORROWED, PAID_OFF.
- [ ] T041 [US3] Create `DebtListState.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/list/DebtListState.kt` — debts: ImmutableList<Debt>, isLoading: Boolean, selectedFilter: DebtFilter, totalLent: Double, totalBorrowed: Double.
- [ ] T042 [US3] Create `DebtListViewModel.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/list/DebtListViewModel.kt` — @HiltViewModel, inject GetDebtsUseCase + DeleteDebtUseCase. Observe debts, compute filtered list based on selectedFilter, compute totalLent/totalBorrowed from active debts. Expose state: StateFlow<DebtListState>. Methods: setFilter(DebtFilter), deleteDebt(id: Long).
- [ ] T043 [US3] Create `DebtListScreen.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/list/DebtListScreen.kt` — Scaffold with TopAppBar ("Долги", back arrow), summary card (total lent green / total borrowed red), FilterChip row (Все/Мне должны/Я должен/Погашенные), LazyColumn of debt cards (contactName, direction badge, totalAmount, LinearProgressIndicator, remaining), SwipeToDismissBox with AlertDialog confirmation, FAB → onAddClick, empty state. Green for LENT, red for BORROWED. testTags for UI testing.
- [ ] T044 [US3] Create `DebtEditBottomSheet.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/edit/DebtEditBottomSheet.kt` — ModalBottomSheet with: contactName OutlinedTextField, direction selector (two FilterChips: "Я дал в долг" / "Я взял в долг"), amount OutlinedTextField (number input), account selector dropdown (list accounts from ViewModel), date picker (default today), note OutlinedTextField, Save button. Supports create + edit modes. Validation: name non-empty, amount > 0, account selected. Direction change warning dialog when editing debt with existing payments (per clarification: offer to delete all payments).
- [ ] T045 [US3] Create `DebtListRoute.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/list/DebtListRoute.kt` — hiltViewModel(), collectAsStateWithLifecycle, call DebtListScreen. Parameters: onDebtClick: (Long) -> Unit, onBack: () -> Unit.

---

## Phase 6: Debt Detail Screen (US4)

### Goal: Users can view debt details, record payments (with optional transaction creation), and manage the debt lifecycle.

### Independent test criteria:
- DebtDetailScreen shows debt info, progress bar, and payment history
- Recording a payment updates progress and optionally creates a linked transaction
- Overpayment shows informational notice but allows saving
- "Погашен" badge appears when fully paid

- [ ] T046 [US4] Create `DebtDetailState.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/detail/DebtDetailState.kt` — debt: Debt?, payments: ImmutableList<DebtPayment>, isLoading: Boolean, showPaymentSheet: Boolean, showEditSheet: Boolean.
- [ ] T047 [US4] Create `DebtDetailViewModel.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/detail/DebtDetailViewModel.kt` — @HiltViewModel, inject GetDebtWithPaymentsUseCase + AddDebtPaymentUseCase + DeleteDebtPaymentUseCase + DeleteDebtUseCase + SaveDebtUseCase. Read debt id from SavedStateHandle. Observe debt + payments. Methods: addPayment(amount, date, note, createTransaction), deletePayment(id), deleteDebt(), saveDebt(debt), togglePaymentSheet(), toggleEditSheet().
- [ ] T048 [US4] Create `PaymentBottomSheet.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/detail/PaymentBottomSheet.kt` — ModalBottomSheet with: amount OutlinedTextField (pre-filled with remainingAmount), date picker, note OutlinedTextField, "Создать транзакцию" Checkbox (default checked), overpayment notice Text (shown when amount > remainingAmount), Save button. Validates amount > 0.
- [ ] T049 [US4] Create `DebtDetailScreen.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/detail/DebtDetailScreen.kt` — Scaffold with TopAppBar (contactName, edit IconButton, delete IconButton). Content: debt info Card (direction badge, totalAmount, accountName, createdAt formatted, note), progress section (LinearProgressIndicator + "Выплачено X из Y" text), "Погашен" badge when status=PAID_OFF, payments LazyColumn (amount, date, note per item, swipe to delete payment), "Записать платёж" Button → show PaymentBottomSheet. Include DebtEditBottomSheet for edit mode. testTags for key elements.
- [ ] T050 [US4] Create `DebtDetailRoute.kt` in `presentation/debts/src/main/java/com/atelbay/money_manager/presentation/debts/ui/detail/DebtDetailRoute.kt` — hiltViewModel(), collectAsStateWithLifecycle, call DebtDetailScreen. Parameters: onBack: () -> Unit.

---

## Phase 7: Navigation & Settings Wiring (US5)

### Goal: Debts screens accessible end-to-end from Settings → DebtList → DebtDetail.

### Independent test criteria:
- Tapping "Долги" in Settings navigates to DebtListScreen
- Tapping a debt card navigates to DebtDetailScreen
- Back navigation works correctly throughout

- [ ] T051 [US5] Add `@Serializable data object DebtList` and `@Serializable data class DebtDetail(val id: Long)` to `app/src/main/java/com/atelbay/money_manager/navigation/Destinations.kt`.
- [ ] T052 [US5] Wire debt screens in `MoneyManagerNavHost.kt` — add `composable<DebtList>` → DebtListRoute(onDebtClick = navigate to DebtDetail(id), onBack = popBackStack), `composable<DebtDetail>` → DebtDetailRoute(onBack = popBackStack). Add `onDebtsClick = { navController.navigate(DebtList) }` to settings composable block.
- [ ] T053 [US5] Add `onDebtsClick: () -> Unit` parameter to `SettingsRoute.kt` and `SettingsScreen.kt` in `presentation/settings/`. Add "Долги" SettingRow in General section (after Recurring row): icon = Icons.Default.AccountBalanceWallet, iconColor = Color(0xFFF59E0B), title = s.debts, hasChevron = true, onClick = onDebtsClick. Add HorizontalDivider.

---

## Phase 8: Statistics TopBar Fix (US6)

### Goal: Statistics screen header renders below system status bar, matching all other tab screens.

### Independent test criteria:
- Title "Статистика" fully visible below status bar on edge-to-edge devices
- CalendarFilterPill still functional in actions slot
- testTag "statistics:header" preserved

- [ ] T054 [P] [US6] Replace Row topBar with TopAppBar in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/StatisticsScreen.kt` — replace the Row (lines ~198-219) with `TopAppBar(title = { Text(s.statisticsTitle, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp, color = colors.textPrimary) }, actions = { CalendarFilterPill(label = pillLabel, onClick = { showMonthPicker = true }) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent), modifier = Modifier.testTag("statistics:header"))`.

---

## Phase 9: Firebase Crashlytics (US7)

### Goal: Production crashes reported to Firebase Crashlytics with user attribution.

### Independent test criteria:
- Release build sends crash reports to Crashlytics console
- Debug build does NOT collect crash data
- Signed-in user crashes include userId

- [ ] T055 [P] [US7] Add firebase-crashlytics library and Gradle plugin to `gradle/libs.versions.toml` — add `firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics" }` under [libraries] (BOM manages version), add `firebase-crashlytics = { id = "com.google.firebase.crashlytics" }` under [plugins] with appropriate version.
- [ ] T056 [US7] Apply crashlytics plugin in `app/build.gradle.kts` — add `alias(libs.plugins.firebase.crashlytics)` to plugins block, add `implementation(libs.firebase.crashlytics)` to dependencies.
- [ ] T057 [US7] Create `CrashlyticsTree.kt` in `app/src/main/java/com/atelbay/money_manager/CrashlyticsTree.kt` — extends Timber.Tree, override log(): for priority >= Log.WARN → FirebaseCrashlytics.getInstance().log(message); for priority >= Log.ERROR → also recordException(throwable ?: RuntimeException(message)).
- [ ] T058 [US7] Update `MoneyManagerApp.kt` — in onCreate(), add else branch: if (!BuildConfig.DEBUG) { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true); Timber.plant(CrashlyticsTree()) }. Also add setCrashlyticsCollectionEnabled(false) in the DEBUG branch.
- [ ] T059 [US7] Update `LoginSyncOrchestrator.kt` in `data/sync/` — when user != null: call `FirebaseCrashlytics.getInstance().setUserId(user.userId)`. When user == null: call `FirebaseCrashlytics.getInstance().setUserId("")`.

---

## Phase 10: Accessibility Pass (US8)

### Goal: All screens navigable via TalkBack with labeled interactive elements.

### Independent test criteria:
- All icon-only buttons have contentDescription
- Section headers in Settings announced as headings
- All interactive elements have ≥48dp touch targets
- Currency names available in EN/KZ

- [ ] T060 [US8] Audit and add contentDescription to all icon-only IconButtons and FloatingActionButtons across all presentation modules — transaction list (add, filter, sort), account list (add), settings (chevrons), statistics (filter), import (upload), budgets (add, delete), recurring (add, delete), categories (add, edit), onboarding, auth, AND new debts screens. Add Role.Button semantics where missing.
- [ ] T061 [US8] Add `semantics { heading() }` modifier to section headers in `presentation/settings/src/main/java/.../SettingsScreen.kt` — "Основные", "Оформление", "О приложении" section titles.
- [ ] T062 [US8] Ensure minimum 48dp touch targets on all interactive elements — audit custom clickable composables that may have smaller hit areas (check Modifier.size or Modifier.clickable without minimum constraints). Material 3 components generally meet this by default.
- [ ] T063 [US8] Localize SupportedCurrencies display names to EN and KZ — find the currency model/enum, add English and Kazakh display names alongside existing Russian names.

---

## Phase 11: Privacy Policy & Version Discipline (US9)

### Goal: Play Store requirements met — privacy policy accessible, version set to 1.0.0.

### Independent test criteria:
- Privacy policy page loads and covers required topics
- "Политика конфиденциальности" link in Settings opens the page
- versionCode=1, versionName="1.0.0" in APK metadata

- [ ] T064 [P] [US9] Create `docs/privacy-policy/index.html` — static HTML page covering: data collected (transactions, categories, accounts), Firestore encryption (Tink AES-256-GCM), Google Sign-In usage (email + display name only), no third-party data sharing, data deletion process (sign out deletes local sync metadata, Firestore data can be deleted on request). Include RU/EN/KZ language sections. Style with minimal CSS.
- [ ] T065 [P] [US9] Update `versionName` from current "1.1" to "1.0.0" in `app/build.gradle.kts` defaultConfig block (versionCode already 1; existing "1.1" and git tags v1.0.0–v1.1.9 were internal dev milestones with no Play Store upload). Add a comment documenting versioning strategy: semantic versioning, versionCode increments monotonically with each release.
- [ ] T066 [US9] Add "Политика конфиденциальности" link in Settings → About section in `presentation/settings/src/main/java/.../SettingsScreen.kt` — SettingRow with icon (Policy or Description), onClick opens browser via Intent(ACTION_VIEW, Uri.parse(privacyPolicyUrl)). Add privacyPolicyUrl and privacyPolicy string to AppStrings (all 3 locales).

---

## Phase 12: Polish & Cross-Cutting

- [ ] T067 Verify full end-to-end flow: Settings → DebtList → create debt → DebtDetail → record payment → verify transaction created → filter by paid off. Run on device/emulator.
- [ ] T068 Run `./gradlew assembleDebug` and fix any compilation errors across all modules.
- [ ] T069 Run `./gradlew lint` and `./gradlew detekt` — fix any new warnings introduced by debt feature code.

---

## Dependencies

```
T001-T005 (Setup) → T006-T023 (Phase 2: US1)
T006-T023 (US1) → T024-T038 (Phase 3: US2)
T024-T038 (US2) → T039 (Phase 4: Strings)
T039 (Strings) → T040-T045 (Phase 5: US3) + T046-T050 (Phase 6: US4)
T040-T050 (US3+US4) → T051-T053 (Phase 7: US5)

Independent streams (can run in parallel with US1-US5):
  T054 (US6: Statistics fix)
  T055-T059 (US7: Crashlytics)
  T064-T065 (US9: Privacy policy + version — partial)

After Phase 7 (all debt screens exist):
  T060-T063 (US8: Accessibility)
  T066 (US9: Privacy link in Settings)

After all phases:
  T067-T069 (Phase 12: Polish)
```

---

## Parallel Execution Opportunities

### Stream A: Debts Feature (sequential)
T001-T005 → T006-T023 → T024-T038 → T039 → T040-T050 → T051-T053

### Stream B: Statistics Fix (independent)
T054 — can start immediately, no dependencies

### Stream C: Crashlytics (independent)
T055 → T056-T058 → T059 — can start immediately

### Stream D: Privacy + Version (partially independent)
T064 + T065 — can start immediately
T066 — after Stream A completes (needs Settings screen changes from T053)

### Stream E: Accessibility (after Stream A Phase 7)
T060-T063 — after debt screens exist

---

## Implementation Strategy

**MVP (minimum shippable increment)**: Phases 1-7 (US1-US5) = complete debts feature end-to-end. This is the critical path.

**Quick wins (parallel)**: US6 (Statistics fix, 1 task) and US7 (Crashlytics, 5 tasks) can ship independently.

**Polish pass**: US8 (Accessibility) and US9 (Privacy + Version) complete production readiness.

**Suggested execution order for single developer**:
1. Phase 1 (Setup) + Phase 2 (US1) + Phase 3 (US2) — data layer foundation
2. Phase 4 (Strings) + Phase 5 (US3) + Phase 6 (US4) — UI
3. Phase 7 (US5) — wire everything together, test E2E
4. Phase 8 (US6) + Phase 9 (US7) — quick independent fixes
5. Phase 10 (US8) + Phase 11 (US9) — production polish
6. Phase 12 — final verification
