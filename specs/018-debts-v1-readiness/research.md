# Research: 018-debts-v1-readiness

**Created**: 2026-04-09
**Status**: Complete

---

## R1: Budgets Module Pattern (Reference Architecture for Debts)

**Decision**: Follow the budgets 3-module pattern exactly.

**Findings**:
- **domain/budgets**: `BudgetRepository` interface + 3 use cases (`GetBudgetsWithSpendingUseCase`, `SaveBudgetUseCase`, `DeleteBudgetUseCase`). Convention plugins: `moneymanager.android.library` + `moneymanager.android.hilt`.
- **data/budgets**: `BudgetRepositoryImpl` (`@Singleton`, uses DAO + SyncManager), `BudgetMapper` (entity↔model), `BudgetDataModule` (`@Binds` in abstract class). Convention plugins: same as domain.
- **presentation/budgets**: Route/Screen/State/ViewModel per screen (list + edit). Convention plugin: `moneymanager.android.feature`.
- Repository save pattern: check `id == 0L` → insert vs update; preserve sync fields from existing record; call `syncManager.syncX(id)` after save.
- Repository delete pattern: `softDeleteById(id, updatedAt)` then `syncManager.syncX(id)`.

**Rationale**: Consistency with existing codebase reduces onboarding friction and ensures sync/navigation patterns work correctly.

---

## R2: Database Migration Pattern (v7 → v8)

**Decision**: Single migration `MIGRATION_7_8` creating two tables (`debts`, `debt_payments`) with indexes.

**Findings**:
- Current DB version: 7. Entities: Account, Category, Transaction, RecurringTransaction, Budget, RegexParserProfile.
- Migration pattern (from `Migration_5_6`, `Migration_6_7`): `object : Migration(N, N+1)` with `execSQL` for CREATE TABLE + CREATE INDEX.
- Entity pattern: `@Entity` with `@PrimaryKey(autoGenerate = true)`, `@ForeignKey` declarations, `@Index` on FK columns and `remoteId`.
- Standard sync fields on every entity: `remoteId: String? = null`, `updatedAt: Long = 0`, `isDeleted: Boolean = false`.

**Alternatives considered**: Two separate migrations (7→8 for debts, 8→9 for debt_payments) — rejected because both tables are part of the same feature and should ship atomically.

---

## R3: Firestore Sync Pattern for Debts

**Decision**: Mirror budgets sync exactly — per-id Mutex, encrypted numeric fields, push/pull with last-write-wins.

**Findings**:
- `SyncManager` uses `ConcurrentHashMap<Long, Mutex>` per entity type for concurrency.
- `syncX(id)` is fire-and-forget (`scope.launch`), ensures `remoteId` via `UUID.randomUUID()`, updates DAO, then pushes to Firestore.
- `pushAllPending()` handles pending (no remoteId) + soft-deleted (has remoteId) for each entity.
- `clearSyncMetadata()` calls `clearRemoteIds()` on every DAO — debts DAO needs this method.
- `PullSyncUseCase` pulls in dependency order: accounts first, then transactions/categories/budgets. Debts must come after accounts (for accountId FK), and debt_payments after debts (for debtId FK).
- DTOs encrypt sensitive numeric fields (`totalAmount`) as `String` via `FieldCipher.encryptDouble()`.
- Firestore path: `users/{userId}/debts/{remoteId}` and `users/{userId}/debt_payments/{remoteId}`.

---

## R4: Strings/Localization Pattern

**Decision**: Add debt strings to `AppStrings` data class with all three locale objects (RU, EN, KZ).

**Findings**:
- All strings are **code-based** in `core/ui/.../theme/AppStrings.kt` — NOT XML resources.
- Three locales: `RussianStrings`, `EnglishStrings`, `KazakhStrings`.
- Budget strings serve as pattern: section comment `// ── Budgets ──`, then ~9 fields.
- Default category names in `DefaultCategories.kt` are Russian-only (not localized via AppStrings). Category display names are stored in DB.

---

## R5: Crashlytics Integration

**Decision**: Add firebase-crashlytics to BOM, create CrashlyticsTree, plant in release builds only.

**Findings**:
- Firebase BOM `34.8.0` already in `libs.versions.toml` — just add `firebase-crashlytics` library entry under it.
- Need `com.google.firebase.crashlytics` Gradle plugin in `app/build.gradle.kts`.
- `MoneyManagerApp.onCreate()` currently plants `Timber.DebugTree()` only in DEBUG. Add CrashlyticsTree in `!DEBUG` branch.
- For userId: `LoginSyncOrchestrator` has `authManager.currentUser` StateFlow — when `user != null`, set `FirebaseCrashlytics.getInstance().setUserId(user.userId)`; when `null`, clear with empty string.
- `crashlyticsCollectionEnabled` set via `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)` in Application.

---

## R6: Statistics TopBar Fix

**Decision**: Replace Row with Material 3 `TopAppBar`.

**Findings**:
- Current implementation: plain `Row` at lines ~198-219 of `StatisticsScreen.kt` with `padding(horizontal = 16.dp, vertical = 12.dp)`. Contains `Text` (title) + `CalendarFilterPill`.
- Other tab screens (Transactions, Accounts, Settings) all use `TopAppBar` which auto-handles `WindowInsets.statusBars`.
- Fix: `TopAppBar(title = { Text(s.statisticsTitle, ...) }, actions = { CalendarFilterPill(...) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))`.

---

## R7: Navigation & Settings Wiring

**Decision**: Add `DebtList` + `DebtDetail(id: Long)` destinations; add settings row after budgets/recurring.

**Findings**:
- Destinations use `@Serializable` data objects/classes. Budget: `BudgetList` + `BudgetEdit(id: Long? = null)`.
- For debts: `DebtList` for list screen, `DebtDetail(id: Long)` for detail screen (NOT an edit screen — editing happens via bottom sheet within detail).
- Settings wiring: `onDebtsClick` callback threaded through `SettingsRoute` → `SettingsScreen` → `SettingRow`. In NavHost: `onDebtsClick = { navController.navigate(DebtList) }`.
- Settings row placement: General section, after Recurring row. Icon: `Icons.Default.AccountBalanceWallet`, color: amber/orange (`Color(0xFFF59E0B)`).

---

## R8: Privacy Policy Hosting

**Decision**: GitHub Pages — simpler for a static page, no Firebase Hosting setup needed.

**Rationale**: The project already uses GitHub for source control. GitHub Pages requires zero additional infrastructure. Firebase Hosting would add deployment complexity for a single static HTML page.

**Alternatives considered**: Firebase Hosting — provides custom domain and CDN but overkill for a single-page privacy policy.
