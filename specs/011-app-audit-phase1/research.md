# Research: App Audit Phase 1

## S1. Backup Rules

**Decision**: Add explicit `<exclude>` rules to both `data_extraction_rules.xml` (API 31+) and `backup_rules.xml` (API 23-30). Exclude database and shared preferences.

**Rationale**: The current files are stock Android Studio templates with all rules commented out. `android:allowBackup="true"` in AndroidManifest.xml means the Room database (`money_manager.db`) is backed up unencrypted to Google Drive by default.

**Current state**:
- `data_extraction_rules.xml`: Empty `<cloud-backup>` section (all commented)
- `backup_rules.xml`: Empty `<full-backup-content>` (all commented)
- AndroidManifest: `allowBackup=true`, `dataExtractionRules` and `fullBackupContent` both reference the XML files

**Approach**: Keep `allowBackup="true"` (needed for other non-sensitive data) but add explicit excludes for `database` domain (covers `money_manager.db`) and `sharedpref` domain (covers DataStore and SharedPreferences).

## A2. Save Button Stuck on Error

**Decision**: Wrap coroutine body in try/catch in `AccountEditViewModel.save()` and `CategoryEditViewModel.save()`. Reset `isSaving = false` in catch, set error message. Also fix AccountEditViewModel to preserve balance/createdAt on edit.

**Rationale**: Both ViewModels set `isSaving = true` before launching coroutine, then call use case with no error handling. If the use case throws, `isSaving` stays true permanently.

**Current state**:
- `AccountEditViewModel.save()` (lines 67-89): No try/catch. Passes `balance = 0.0` and `createdAt = System.currentTimeMillis()` unconditionally — even on edit
- `CategoryEditViewModel.save()` (lines 75-97): No try/catch
- `TransactionEditViewModel.save()`: Already has try/catch but swallows error silently (no error message set)
- State classes have `isSaving: Boolean` and `nameError: String?` fields

**Approach**:
1. AccountEditViewModel: wrap in try/catch, set `nameError` on failure, reset `isSaving`; for edit, load original `balance`/`createdAt` and pass them through
2. CategoryEditViewModel: same try/catch pattern
3. TransactionEditViewModel: already has try/catch but needs to surface error — add error message to catch block

## L1+L2. Hardcoded Russian Strings

**Decision**: Add ~25 new keys to `AppStrings` data class + all three language implementations. Use `MoneyManagerTheme.strings` in composables and pass strings from presentation layer for ViewModel errors.

**Rationale**: ~15+ hardcoded Russian strings bypass the `AppStrings` localization system.

**Current state**:
- `AppStrings` is a `data class` (not interface) with ~130 keys
- Three implementations: `RussianStrings`, `EnglishStrings`, `KazakhStrings`
- Accessed via `MoneyManagerTheme.strings.keyName` in composables
- `LocalStrings = staticCompositionLocalOf<AppStrings> { RussianStrings }`
- Lambda strings supported for parameterized text: `val importedCount: (count: Int) -> String`

**Affected files** (with string counts):
| File | Hardcoded strings | Count |
|------|-------------------|-------|
| AccountEditViewModel.kt:71 | "Введите название счёта" | 1 |
| CategoryEditViewModel.kt:79 | "Введите название категории" | 1 |
| TransactionEditViewModel.kt:151,156 | "Введите корректную сумму", "Выберите категорию" | 2 |
| ImportViewModel.kt:84,95,115,166,194 | 5 error messages | 5 |
| SignInViewModel.kt:49,51 | 2 error messages | 2 |
| CategoryTransactionsViewModel.kt:37,89 | English error (not Russian) | 1 |
| ImportPreview.kt:85,90,97,103,114,133,163,207,216 | 9 UI strings (some with lambdas) | 9 |
| SettingsViewModel.kt:195 | "Не удалось обновить курс" | 1 |

**Approach for ViewModels**: ViewModels cannot access `MoneyManagerTheme.strings` (no Compose context). Two options:
1. Store error keys/enums in state → map to strings in UI layer
2. Pass `AppStrings` to ViewModel via Hilt

Option 1 is cleaner — use a generic `errorMessage: String?` field that the UI layer sets from AppStrings when displaying. But the current pattern already uses `String?` for errors (e.g., `nameError`, `errorMessage`). The simplest fix: make the ViewModel set a sentinel/key string, and map it in the UI. However, this creates a fragile contract.

**Better approach**: Use `StringResource` sealed class pattern — but that's over-engineering for this fix. Simplest: keep `String?` error fields, but have the ViewModel validation set the error from an `AppStrings` instance passed to the method or injected. Since ViewModels already exist and changing their constructor is invasive, the **pragmatic fix** is: extract error strings into `AppStrings` keys, and have the composable pass the strings to the ViewModel's validation method OR have the composable set the error message itself based on a validation result enum.

**Final decision**: For ViewModel validation errors (6 ViewModels), use the existing `String?` error field pattern but move the string resolution to the composable layer. The ViewModel returns a validation error enum/boolean, and the composable maps it to `MoneyManagerTheme.strings.xxx`. For ImportPreview.kt composable strings, replace directly with `MoneyManagerTheme.strings.xxx`.

## D1. Database Indexes

**Decision**: Add indexes to `AccountEntity`, `CategoryEntity`, and a composite index to `TransactionEntity`. Bump DB version from 3 to 4. Write `MIGRATION_3_4`.

**Rationale**: Multiple DAO queries filter on `isDeleted`, `type`, `remoteId`, and composite `(categoryId, type, date)` without indexes.

**Current state**:
- `AccountEntity`: No indexes (only PK). Queries filter on `isDeleted`, `remoteId`
- `CategoryEntity`: No indexes (only PK). Queries filter on `isDeleted`, `type`, `remoteId`
- `TransactionEntity`: 4 single-column indexes (`accountId`, `categoryId`, `date`, `uniqueHash`). Missing composite index for statistics query
- DB version: 3. One migration exists: `MIGRATION_2_3`
- Migration pattern: `val MIGRATION_X_Y = object : Migration(X, Y) { ... }`
- `DatabaseModule.kt`: `.addMigrations(MIGRATION_2_3)` — add new migration here

**Indexes to add**:
- `AccountEntity`: `Index("isDeleted")`, `Index("remoteId")`
- `CategoryEntity`: `Index("type", "isDeleted")`, `Index("remoteId")`
- `TransactionEntity`: `Index("categoryId", "type", "date")` (composite for statistics), `Index("isDeleted")`

**Migration SQL**:
```sql
CREATE INDEX IF NOT EXISTS index_accounts_isDeleted ON accounts (isDeleted)
CREATE INDEX IF NOT EXISTS index_accounts_remoteId ON accounts (remoteId)
CREATE INDEX IF NOT EXISTS index_categories_type_isDeleted ON categories (type, isDeleted)
CREATE INDEX IF NOT EXISTS index_categories_remoteId ON categories (remoteId)
CREATE INDEX IF NOT EXISTS index_transactions_categoryId_type_date ON transactions (categoryId, type, date)
CREATE INDEX IF NOT EXISTS index_transactions_isDeleted ON transactions (isDeleted)
```

## A1. SyncManager Domain Interface

**Decision**: Create `domain:sync` module with `SyncRepository` interface. Move `SyncStatus` to `core:model`. Implement interface in `data:sync`.

**Rationale**: `presentation:settings` imports `SyncManager`, `LoginSyncOrchestrator`, and `SyncStatus` directly from `data:sync` — architecture violation.

**Current state**:
- `SyncStatus` is a sealed interface in `data:sync` package
- `SettingsViewModel` uses:
  - `syncManager.syncStatus: StateFlow<SyncStatus>` (observe)
  - `loginSyncOrchestrator.retrySync()` (action)
- `presentation:settings/build.gradle.kts` has `implementation(projects.data.sync)`

**SettingsViewModel sync API surface** (what the domain interface needs):
1. `syncStatus: Flow<SyncStatus>` — observe current sync state
2. `retrySync()` — trigger sync retry

**Approach**:
1. Move `SyncStatus` to `core:model` (package `com.atelbay.money_manager.core.model`)
2. Create `domain:sync` module with `SyncRepository` interface:
   ```kotlin
   interface SyncRepository {
       val syncStatus: Flow<SyncStatus>
       fun retrySync()
   }
   ```
3. Create `SyncRepositoryImpl` in `data:sync` that delegates to `SyncManager`/`LoginSyncOrchestrator`
4. Add Hilt binding in `data:sync`
5. Update `SettingsViewModel` to inject `SyncRepository` instead of concrete classes
6. Replace `implementation(projects.data.sync)` with `implementation(projects.domain.sync)` in `presentation:settings/build.gradle.kts`
7. Register `:domain:sync` in `settings.gradle.kts`

**Alternatives considered**: Moving just `SyncStatus` to `core:model` and having the ViewModel use a use case — rejected because a simple repository interface is sufficient and matches existing patterns.
