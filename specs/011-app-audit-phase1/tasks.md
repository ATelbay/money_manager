# Tasks: App Audit Phase 1 — Critical & High Priority Fixes

**Input**: Design documents from `/specs/011-app-audit-phase1/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/sync-repository.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US5)
- Exact file paths included in descriptions

---

## Phase 1: Foundational (AppStrings Keys)

**Purpose**: Add all new localization keys to AppStrings that are needed by US2 and US3. This MUST be complete before ViewModel error handling or string replacement tasks.

- [ ] T001 Add ~22 new keys to `AppStrings` data class, `RussianStrings`, `EnglishStrings`, and `KazakhStrings` in `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/AppStrings.kt`. Add all keys from the data-model.md "AppStrings Keys" tables: 13 validation error keys (`errorEnterAccountName`, `errorEnterCategoryName`, `errorEnterValidAmount`, `errorSelectCategory`, `errorReadingPdf`, `errorUnknown`, `errorNoTransactionsFound`, `errorSelectAccountForImport`, `errorImport`, `errorNoInternet`, `errorSignInFailed`, `errorLoadCategoryTransactions`, `errorUpdateRate`) and 9 import preview keys (`importFound` lambda(Int), `importDuplicates` lambda(Int), `importReadyCount` lambda(Int, Int), `importNoCategory` lambda(Int), `importButton` lambda(Int), `importNeedsReview` lambda(Int), `importRecognized` lambda(Int), `importAccountLabel`, `importSelectAccount`). Use exact Russian/English/Kazakh translations from data-model.md. Follow existing patterns — simple keys as `val name: String`, parameterized keys as `val name: (params) -> String` lambdas.

**Checkpoint**: `./gradlew :core:ui:compileDebugKotlin` passes. All three language implementations compile.

---

## Phase 2: User Story 1 — Financial Data Protected from Cloud Backup (Priority: P1)

**Goal**: Exclude Room database and shared preferences from Android cloud backup.

**Independent Test**: Run `adb shell bmgr backupnow com.atelbay.money_manager` and verify database is excluded.

### Implementation

- [ ] T002 [P] [US1] Configure `data_extraction_rules.xml` (API 31+) in `app/src/main/res/xml/data_extraction_rules.xml`. Replace the commented-out `<cloud-backup>` section with active exclude rules: `<exclude domain="database" path="money_manager.db"/>` and `<exclude domain="sharedpref" path="."/>`. Also add a `<device-transfer>` section with the same excludes. Keep the XML well-formed.

- [ ] T003 [P] [US1] Configure `backup_rules.xml` (API 23–30) in `app/src/main/res/xml/backup_rules.xml`. Replace the commented-out content inside `<full-backup-content>` with active exclude rules: `<exclude domain="database" path="money_manager.db"/>` and `<exclude domain="sharedpref" path="."/>`.

**Checkpoint**: `./gradlew assembleDebug` passes. XML files are well-formed. Backup rules exclude database and shared preferences.

---

## Phase 3: User Story 2 — Save Button Recovers After Error (Priority: P1)

**Goal**: Fix stuck `isSaving` state in AccountEdit and CategoryEdit ViewModels. Surface error messages. Preserve balance/createdAt on account edit.

**Independent Test**: Trigger a save error → verify button re-enables with error message.

**Depends on**: T001 (AppStrings keys for error messages)

### Implementation

- [ ] T004 [P] [US2] Fix `AccountEditViewModel.save()` in `presentation/accounts/src/main/java/com/atelbay/money_manager/presentation/accounts/ui/edit/AccountEditViewModel.kt`. Three changes: (1) Change the `save()` method signature to accept an `AppStrings` parameter so the composable passes localized strings. (2) Wrap the coroutine body (the `viewModelScope.launch` block) in try/catch — in catch, reset `isSaving = false` and set `nameError` to the localized generic error string (`strings.errorUnknown`). Rethrow `CancellationException`. (3) Fix the edit flow: when loading an existing account (in the `init` block where account is loaded), store the original `balance` and `createdAt` values in the state (add `originalBalance: Double = 0.0` and `originalCreatedAt: Long = 0` fields to `AccountEditState`). In `save()`, use these stored values instead of hardcoded `balance = 0.0` and `createdAt = System.currentTimeMillis()` when `accountId != null`. For new accounts (accountId == null), keep `balance = 0.0` and `createdAt = System.currentTimeMillis()`. Also replace the hardcoded Russian validation string `"Введите название счёта"` with `strings.errorEnterAccountName`.

- [ ] T005 [P] [US2] Fix `CategoryEditViewModel.save()` in `presentation/categories/src/main/java/com/atelbay/money_manager/presentation/categories/ui/edit/CategoryEditViewModel.kt`. Two changes: (1) Change `save()` signature to accept `AppStrings` parameter. (2) Wrap the coroutine body in try/catch — in catch, reset `isSaving = false` and set `nameError` to `strings.errorUnknown`. Rethrow `CancellationException`. Also replace hardcoded `"Введите название категории"` with `strings.errorEnterCategoryName`.

- [ ] T006 [US2] Fix `TransactionEditViewModel.save()` in `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/edit/TransactionEditViewModel.kt`. The method already has try/catch but swallows the error silently. (1) Change `save()` signature to accept `AppStrings` parameter. (2) In the catch block, set an error message on the state (use `amountError` or add a generic `saveError: String?` field to `TransactionEditState`) with `strings.errorUnknown`. (3) Replace hardcoded Russian validation strings: `"Введите корректную сумму"` → `strings.errorEnterValidAmount`, `"Выберите категорию"` → `strings.errorSelectCategory`.

- [ ] T007 [US2] Update composable screens that call `save()` to pass `MoneyManagerTheme.strings`. Update `AccountEditScreen` composable to pass `MoneyManagerTheme.strings` when calling `viewModel.save(strings, onComplete)`. Same for `CategoryEditScreen` and `TransactionEditScreen`. Find these composable files by searching for usages of `viewModel.save(` in the corresponding presentation modules.

**Checkpoint**: `./gradlew :presentation:accounts:compileDebugKotlin :presentation:categories:compileDebugKotlin :presentation:transactions:compileDebugKotlin` passes. Save error → button re-enables.

---

## Phase 4: User Story 4 — Fast Query Performance on Growing Data (Priority: P2)

**Goal**: Add database indexes to AccountEntity, CategoryEntity, TransactionEntity. Create Room migration 3→4.

**Independent Test**: Fresh install has indexes in schema. Upgrade from v3 creates indexes via migration.

### Implementation

- [ ] T008 [P] [US4] Add `@Index` annotations to `AccountEntity` in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/AccountEntity.kt`. Add `indices` parameter to the `@Entity` annotation: `Index("isDeleted")` and `Index("remoteId")`. See data-model.md for exact format.

- [ ] T009 [P] [US4] Add `@Index` annotations to `CategoryEntity` in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/CategoryEntity.kt`. Add `indices` parameter: `Index("type", "isDeleted")` (composite) and `Index("remoteId")`.

- [ ] T010 [P] [US4] Add composite index to `TransactionEntity` in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/TransactionEntity.kt`. Add two new entries to the existing `indices` array: `Index("categoryId", "type", "date")` for the statistics query and `Index("isDeleted")`. Keep all 4 existing indexes unchanged.

- [ ] T011 [US4] Create migration file `Migration_3_4.kt` at `core/database/src/main/java/com/atelbay/money_manager/core/database/migration/Migration_3_4.kt`. Follow the exact pattern from `Migration_2_3.kt`: `val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) { ... } }`. Execute 6 `CREATE INDEX IF NOT EXISTS` SQL statements from data-model.md. Import `androidx.room.migration.Migration` and `androidx.sqlite.db.SupportSQLiteDatabase`.

- [ ] T012 [US4] Bump database version and wire migration. In `core/database/src/main/java/com/atelbay/money_manager/core/database/MoneyManagerDatabase.kt`, change `version = 3` to `version = 4`. In `core/database/src/main/java/com/atelbay/money_manager/core/database/di/DatabaseModule.kt`, add `MIGRATION_3_4` to the `.addMigrations(MIGRATION_2_3)` call → `.addMigrations(MIGRATION_2_3, MIGRATION_3_4)`. Add the import for `MIGRATION_3_4`.

**Checkpoint**: `./gradlew :core:database:compileDebugKotlin` passes. Room schema export (version 4) includes all new indexes.

---

## Phase 5: User Story 5 — Clean Architecture for Sync Feature (Priority: P3)

**Goal**: Extract SyncManager behind `SyncRepository` domain interface. Remove `presentation:settings` → `data:sync` dependency.

**Independent Test**: `grep -r "data.sync" presentation/settings/build.gradle.kts` returns empty. App builds and sync works.

### Implementation

- [ ] T013 [P] [US5] Move `SyncStatus` to `core:model`. Create `core/model/src/main/java/com/atelbay/money_manager/core/model/SyncStatus.kt` with the exact sealed interface from contracts/sync-repository.md (package `com.atelbay.money_manager.core.model`). Then delete the original `data/sync/src/main/java/com/atelbay/money_manager/data/sync/SyncStatus.kt`.

- [ ] T014 [P] [US5] Create `domain:sync` module. Create `domain/sync/build.gradle.kts` following the pattern from `domain/transactions/build.gradle.kts`: plugins `moneymanager.android.library` + `moneymanager.android.hilt`, namespace `com.atelbay.money_manager.domain.sync`, dependency on `projects.core.model`. Add `libs.bundles.coroutines` for Flow. Create `domain/sync/src/main/java/com/atelbay/money_manager/domain/sync/SyncRepository.kt` with the interface from contracts/sync-repository.md. Register `:domain:sync` in `settings.gradle.kts` (add `include(":domain:sync")` alongside other domain modules).

- [ ] T015 [US5] Create `SyncRepositoryImpl` and Hilt binding in `data:sync`. Create `data/sync/src/main/java/com/atelbay/money_manager/data/sync/SyncRepositoryImpl.kt` per contracts/sync-repository.md — `@Inject constructor(syncManager, loginSyncOrchestrator)`, implements `SyncRepository`, delegates `syncStatus` to `syncManager.syncStatus` and `retrySync()` to `loginSyncOrchestrator.retrySync()`. Create `data/sync/src/main/java/com/atelbay/money_manager/data/sync/di/SyncModule.kt` with `@Module @InstallIn(SingletonComponent::class)` and `@Binds` method. Add `implementation(projects.domain.sync)` to `data/sync/build.gradle.kts`. Update `SyncManager.kt` and `LoginSyncOrchestrator.kt` imports from `com.atelbay.money_manager.data.sync.SyncStatus` to `com.atelbay.money_manager.core.model.SyncStatus`.

- [ ] T016 [US5] Update `SettingsViewModel` to use `SyncRepository`. In `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsViewModel.kt`: (1) Replace imports of `SyncManager`, `LoginSyncOrchestrator`, and `SyncStatus` from `data.sync` with imports from `domain.sync.SyncRepository` and `core.model.SyncStatus`. (2) Replace constructor parameters `syncManager: SyncManager` and `loginSyncOrchestrator: LoginSyncOrchestrator` with single `syncRepository: SyncRepository`. (3) Update `syncManager.syncStatus` → `syncRepository.syncStatus` and `loginSyncOrchestrator.retrySync()` → `syncRepository.retrySync()`. (4) In `presentation/settings/build.gradle.kts`: replace `implementation(projects.data.sync)` with `implementation(projects.domain.sync)`.

- [ ] T016b [US5] Update `SettingsViewModelTest` in `presentation/settings/src/test/java/com/atelbay/money_manager/presentation/settings/ui/SettingsViewModelTest.kt`. Replace `import com.atelbay.money_manager.data.sync.SyncManager` and `import com.atelbay.money_manager.data.sync.LoginSyncOrchestrator` with `import com.atelbay.money_manager.domain.sync.SyncRepository`. Replace `mockk<SyncManager>()` and `mockk<LoginSyncOrchestrator>()` with a single `mockk<SyncRepository>(relaxed = true)`. Update the ViewModel constructor call to pass `syncRepository` instead of `syncManager` + `loginSyncOrchestrator`. Ensure `syncRepository.syncStatus` returns a `flowOf(SyncStatus.Idle)` stub.

**Checkpoint**: `./gradlew :presentation:settings:compileDebugKotlin :presentation:settings:testDebugUnitTest` passes. No `data.sync` imports in `presentation:settings`. `./gradlew assembleDebug` passes.

---

## Phase 6: User Story 3 — App Displays Correct Language for All Text (Priority: P2)

**Goal**: Replace all remaining hardcoded Russian/English error strings with AppStrings keys across all affected ViewModels and composables.

**Independent Test**: Switch to English → navigate all affected screens → no Russian text.

**Depends on**: T001 (AppStrings keys), T004-T006 (ViewModel save() already accepts AppStrings), T016 (SettingsVM uses SyncRepository)

### Implementation

- [ ] T017 [P] [US3] Localize `ImportViewModel` errors in `presentation/import/src/main/java/com/atelbay/money_manager/presentation/importstatement/ui/ImportViewModel.kt`. Change the method that triggers parsing/import to accept `AppStrings` parameter (or inject via the composable call). Replace 5 hardcoded Russian strings: `"Ошибка чтения PDF"` → `strings.errorReadingPdf`, `"Неизвестная ошибка"` → `strings.errorUnknown`, `"Не удалось найти транзакции в документе"` → `strings.errorNoTransactionsFound`, `"Выберите счёт для импорта"` → `strings.errorSelectAccountForImport`, `"Ошибка импорта"` → `strings.errorImport`. Update the composable that calls these methods to pass `MoneyManagerTheme.strings`.

- [ ] T017b [US3] Update `ImportViewModelTest` in `presentation/import/src/test/java/com/atelbay/money_manager/presentation/importstatement/ui/ImportViewModelTest.kt`. The 3 calls to `viewModel.importTransactions()` (lines ~149, ~178, ~207) need to pass a test `AppStrings` instance. Create a `testStrings` val at the top of the test class using `RussianStrings` (the default) from `core:ui`. Update all `importTransactions()` calls to `importTransactions(testStrings)`. If other methods also changed signature (e.g., parsing methods), update those call sites too. Verify with `./gradlew :presentation:import:testDebugUnitTest`.

- [ ] T018 [P] [US3] Localize `SignInViewModel` errors in `presentation/auth/src/main/java/com/atelbay/money_manager/presentation/auth/ui/SignInViewModel.kt`. Change `signIn()` to accept `AppStrings` parameter. Replace `"Нет подключения к интернету"` → `strings.errorNoInternet` and `"Не удалось войти. Попробуйте снова"` → `strings.errorSignInFailed`. Update the composable caller to pass `MoneyManagerTheme.strings`.

- [ ] T019 [P] [US3] Localize `CategoryTransactionsViewModel` errors in `presentation/statistics/src/main/java/com/atelbay/money_manager/presentation/statistics/ui/CategoryTransactionsViewModel.kt`. The error strings here (`"Unable to load category transactions"`) are set in init and `.catch` blocks — these run without composable context. Use `AppStrings` parameter on the function or use a fixed English-neutral key. Replace both occurrences at lines ~37 and ~89 with the `errorLoadCategoryTransactions` key. Since this ViewModel initializes in `init` (no composable context available), use an error-key approach: store a nullable error sealed type (e.g., `sealed interface UiError { data object LoadFailed : UiError }`) in the state instead of a raw string. In the composable (`CategoryTransactionsScreen`), map the error to a localized string: `when (state.error) { is UiError.LoadFailed -> strings.errorLoadCategoryTransactions; null -> null }`. This avoids threading `AppStrings` through ViewModel init and keeps the ViewModel testable without locale dependencies.

- [ ] T020 [P] [US3] Localize `SettingsViewModel` rate error in `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsViewModel.kt`. In `refreshExchangeRate()`, replace `"Не удалось обновить курс"` with a localized string. Since this runs in a coroutine, change `refreshExchangeRate()` to accept `AppStrings` parameter and replace the hardcoded string with `strings.errorUpdateRate`. Update the composable that calls `refreshExchangeRate()` to pass `MoneyManagerTheme.strings`.

- [ ] T021 [US3] Localize `ImportPreview.kt` composable strings in `presentation/import/src/main/java/com/atelbay/money_manager/presentation/importstatement/ui/components/ImportPreview.kt`. Replace all 9 hardcoded Russian strings with `MoneyManagerTheme.strings` calls: `"Найдено: ${result.total}"` → `strings.importFound(result.total)`, `"Дубликаты: ${result.duplicates}"` → `strings.importDuplicates(result.duplicates)`, `"К импорту: $readyCount из ${result.newTransactions.size}"` → `strings.importReadyCount(readyCount, result.newTransactions.size)`, `"Без категории: $noCategoryCount"` → `strings.importNoCategory(noCategoryCount)`, `"Импорт ($readyCount)"` → `strings.importButton(readyCount)`, `"Требуют проверки (${needsReview.size})"` → `strings.importNeedsReview(needsReview.size)`, `"Распознаны (${confident.size})"` → `strings.importRecognized(confident.size)`, `"Счёт для импорта"` → `strings.importAccountLabel`, `"Выберите счёт"` → `strings.importSelectAccount`. Get strings via `val strings = MoneyManagerTheme.strings` at top of composable function.

**Checkpoint**: `./gradlew assembleDebug` passes. Switch to English locale → all affected screens show English text.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verify everything works together, run full test suite.

- [ ] T022 Run `./gradlew assembleDebug` to verify full app builds with all changes
- [ ] T023 Run `./gradlew test` to verify all existing unit tests pass (zero regressions)
- [ ] T024 Run `./gradlew lint` to verify no new lint warnings introduced
- [ ] T025 Verify architecture: confirm no `data.*` imports exist in any `presentation/*/src/` files by running a grep across all presentation modules for `import com.atelbay.money_manager.data.` — only `presentation:settings` had the violation, and it should now be gone

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Foundational)**: No dependencies — start immediately
- **Phase 2 (US1 — Backup)**: No dependencies — can start immediately, parallel with Phase 1
- **Phase 3 (US2 — Save buttons)**: Depends on Phase 1 (needs AppStrings keys)
- **Phase 4 (US4 — Indexes)**: No dependencies — can start immediately, parallel with all phases
- **Phase 5 (US5 — Sync arch)**: No dependencies — can start immediately, parallel with all phases
- **Phase 6 (US3 — Localization)**: Depends on Phase 1 (keys), Phase 3 (ViewModel pattern), Phase 5 (SettingsVM refactored)
- **Phase 7 (Polish)**: Depends on all previous phases

### User Story Dependencies

- **US1 (P1 — Backup)**: Completely standalone. No dependencies on other stories.
- **US2 (P1 — Save buttons)**: Depends on T001 (AppStrings keys). No dependencies on other stories.
- **US4 (P2 — Indexes)**: Completely standalone. No dependencies on other stories.
- **US5 (P3 — Sync arch)**: Completely standalone. No dependencies on other stories.
- **US3 (P2 — Localization)**: Depends on T001 (keys), US2 (ViewModel pattern established), US5 (SettingsVM uses domain interface)

### Within Each User Story

- Entity/model changes before service/ViewModel changes
- ViewModel changes before composable call-site updates
- Core module changes before consumer module changes

### Parallel Opportunities

**Maximum parallelism (5 concurrent tracks after T001):**

```
Track A: T002, T003          (US1 — backup rules)
Track B: T004, T005, T006    (US2 — save buttons, after T001)
Track C: T008, T009, T010    (US4 — entity indexes)
Track D: T013, T014          (US5 — SyncStatus + domain:sync module)
Track E: T011, T012          (US4 — migration, after T008-T010)
```

Then sequentially:
```
T007  (US2 — composable callers, after T004-T006)
T015  (US5 — SyncRepositoryImpl, after T013-T014)
T016  (US5 — SettingsVM, after T015)
T017-T021 parallel (US3 — localization, after T001+T007+T016)
T022-T025 sequential (Polish, after all)
```

---

## Parallel Example: Phase 4 (US4 — Database Indexes)

```
# Launch all entity index tasks in parallel:
Task T008: "Add @Index to AccountEntity in core/database/.../entity/AccountEntity.kt"
Task T009: "Add @Index to CategoryEntity in core/database/.../entity/CategoryEntity.kt"
Task T010: "Add composite index to TransactionEntity in core/database/.../entity/TransactionEntity.kt"

# Then sequentially:
Task T011: "Create Migration_3_4.kt" (needs to know exact index names from T008-T010)
Task T012: "Bump version + wire migration in DatabaseModule"
```

## Parallel Example: Phase 5 (US5 — Sync Architecture)

```
# Launch in parallel:
Task T013: "Move SyncStatus to core:model"
Task T014: "Create domain:sync module + SyncRepository interface"

# Then sequentially:
Task T015: "Create SyncRepositoryImpl + Hilt binding in data:sync"
Task T016: "Update SettingsViewModel + presentation:settings build.gradle"
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete T001 (AppStrings keys — foundational)
2. Complete T002-T003 (US1 — backup, parallel with T001)
3. Complete T004-T007 (US2 — save buttons)
4. **STOP and VALIDATE**: Backup rules active, save buttons recover from errors
5. This covers both P1 critical items

### Incremental Delivery

1. T001 → Foundation ready
2. T002-T003 → US1 complete (backup protected)
3. T004-T007 → US2 complete (save buttons fixed)
4. T008-T012 → US4 complete (indexes added)
5. T013-T016 → US5 complete (architecture fixed)
6. T017-T021 → US3 complete (localization done)
7. T022-T025 → All verified

### Full Parallel Strategy

With 3+ agents:
1. Agent A: T001 (foundational) → T004-T007 (US2) → T017-T021 (US3)
2. Agent B: T002-T003 (US1) → T008-T012 (US4)
3. Agent C: T013-T016 (US5) → T022-T025 (Polish)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All tasks reference exact file paths from plan.md
- Commit after each phase or logical group
- `./gradlew assembleDebug` should pass after each phase checkpoint
- No new tests required per spec scope — only existing tests must pass
