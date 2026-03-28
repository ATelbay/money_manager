# Tasks: Pre-iOS Port Feature Bundle

**Input**: Design documents from `/specs/016-pre-ios-features/`
**Prerequisites**: plan.md, spec.md, data-model.md, research.md, quickstart.md

**Tests**: Not explicitly requested — test tasks omitted.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create new Gradle modules and register them

- [ ] T001 [P] Create domain/recurring module: `domain/recurring/build.gradle.kts` (plugins: `moneymanager.android.library` + `moneymanager.android.hilt`, dependencies on `projects.core.model`, `projects.core.database`, `projects.domain.transactions`, `projects.domain.accounts`), create `src/main/java/com/atelbay/money_manager/domain/recurring/` directory structure with `repository/` and `usecase/` packages
- [ ] T002 [P] Create data/recurring module: `data/recurring/build.gradle.kts` (plugins: `moneymanager.android.library` + `moneymanager.android.hilt`, dependencies on `projects.domain.recurring`, `projects.core.database`, `projects.core.model`, `projects.data.sync`, `libs.room.ktx`), create `src/main/java/com/atelbay/money_manager/data/recurring/` directory structure with `di/`, `mapper/`, `repository/` packages
- [ ] T003 [P] Create presentation/recurring module: `presentation/recurring/build.gradle.kts` (plugin: `moneymanager.android.feature`, dependencies on `projects.domain.recurring`, `projects.domain.categories`, `projects.domain.accounts`, `projects.core.model`, `projects.core.ui`, `projects.core.common`), create `src/main/java/com/atelbay/money_manager/presentation/recurring/ui/` directory structure with `list/` and `edit/` packages
- [ ] T004 [P] Create domain/budgets module: `domain/budgets/build.gradle.kts` (plugins: `moneymanager.android.library` + `moneymanager.android.hilt`, dependencies on `projects.core.model`, `projects.core.database`), create `src/main/java/com/atelbay/money_manager/domain/budgets/` directory structure with `repository/` and `usecase/` packages
- [ ] T005 [P] Create data/budgets module: `data/budgets/build.gradle.kts` (plugins: `moneymanager.android.library` + `moneymanager.android.hilt`, dependencies on `projects.domain.budgets`, `projects.core.database`, `projects.core.model`, `projects.data.sync`, `libs.room.ktx`), create `src/main/java/com/atelbay/money_manager/data/budgets/` directory structure with `di/`, `mapper/`, `repository/` packages
- [ ] T006 [P] Create presentation/budgets module: `presentation/budgets/build.gradle.kts` (plugin: `moneymanager.android.feature`, dependencies on `projects.domain.budgets`, `projects.domain.categories`, `projects.core.model`, `projects.core.ui`, `projects.core.common`), create `src/main/java/com/atelbay/money_manager/presentation/budgets/ui/` directory structure with `list/` and `edit/` packages
- [ ] T007 Register all 6 new modules in `settings.gradle.kts`: add `include(":domain:recurring")`, `include(":data:recurring")`, `include(":presentation:recurring")`, `include(":domain:budgets")`, `include(":data:budgets")`, `include(":presentation:budgets")`
- [ ] T008 Add new module dependencies in `app/build.gradle.kts`: add implementation for `projects.domain.recurring`, `projects.data.recurring`, `projects.presentation.recurring`, `projects.domain.budgets`, `projects.data.budgets`, `projects.presentation.budgets`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Room entities, DAOs, migration, domain models, and localization strings that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T009 [P] Create Frequency enum in `core/model/src/main/java/com/atelbay/money_manager/core/model/Frequency.kt`: enum with values DAILY, WEEKLY, MONTHLY, YEARLY
- [ ] T010 [P] Create RecurringTransaction domain model in `core/model/src/main/java/com/atelbay/money_manager/core/model/RecurringTransaction.kt`: data class with fields per data-model.md (id, amount, type as TransactionType, categoryId/Name/Icon/Color, accountId/Name, note, frequency as Frequency, startDate, endDate, dayOfMonth, dayOfWeek, lastGeneratedDate, isActive, createdAt)
- [ ] T011 [P] Create Budget domain model in `core/model/src/main/java/com/atelbay/money_manager/core/model/Budget.kt`: data class with fields per data-model.md (id, categoryId/Name/Icon/Color, monthlyLimit, spent, remaining, percentage)
- [ ] T012 [P] Create RecurringTransactionEntity in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/RecurringTransactionEntity.kt`: @Entity with tableName="recurring_transactions", all fields per data-model.md, foreign keys to categories and accounts with CASCADE, indices on accountId, categoryId, and composite (isActive, isDeleted)
- [ ] T013 [P] Create BudgetEntity in `core/database/src/main/java/com/atelbay/money_manager/core/database/entity/BudgetEntity.kt`: @Entity with tableName="budgets", all fields per data-model.md, foreign key to categories with CASCADE, unique index on categoryId
- [ ] T014 [P] Create RecurringTransactionDao in `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/RecurringTransactionDao.kt`: @Dao interface with observeAll() returning Flow (isDeleted=false), getById(id), getActiveRecurrings() (isActive=true, isDeleted=false), insert, update, softDelete(id, updatedAt), updateLastGeneratedDate(id, date, updatedAt)
- [ ] T015 [P] Create BudgetDao in `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/BudgetDao.kt`: @Dao interface with observeAll() returning Flow (isDeleted=false), getById(id), getByCategoryId(categoryId), insert, update, softDelete(id, updatedAt)
- [ ] T016 Add observeExpenseSumByCategory query to `core/database/src/main/java/com/atelbay/money_manager/core/database/dao/TransactionDao.kt`: `@Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE categoryId = :categoryId AND type = 'expense' AND isDeleted = 0 AND date >= :startDate AND date < :endDate") fun observeExpenseSumByCategory(categoryId: Long, startDate: Long, endDate: Long): Flow<Double>`
- [ ] T017 Create Migration_4_5 in `core/database/src/main/java/com/atelbay/money_manager/core/database/migration/Migration_4_5.kt`: top-level val MIGRATION_4_5 with CREATE TABLE for recurring_transactions and budgets, plus CREATE INDEX statements per data-model.md
- [ ] T018 Update MoneyManagerDatabase in `core/database/src/main/java/com/atelbay/money_manager/core/database/MoneyManagerDatabase.kt`: bump version to 5, add RecurringTransactionEntity and BudgetEntity to @Database entities array, add abstract fun recurringTransactionDao() and budgetDao(), register MIGRATION_4_5 in DatabaseModule
- [ ] T019 Add all new localization strings to `core/ui/src/main/java/com/atelbay/money_manager/core/ui/theme/AppStrings.kt`: add ~30 new properties to the AppStrings data class and provide values in RussianStrings, EnglishStrings, KazakhStrings. Strings needed: currentMonth (ru:"Тек. месяц"/en:"This month"/kk:"Ағымдағы ай"), thirtyDays (ru:"30 дней"/en:"30 days"/kk:"30 күн"), selectDates/month/range, recurring/daily/weekly/monthly/yearly/startDate/endDate/nextDate/recurringTransactions, budgets/limit/spent/remaining/overBudget/newBudget, exportCsv/exportComplete/exportError — full list per spec.md Localization sections

**Checkpoint**: Foundation ready — Room migration, entities, DAOs, domain models, and strings are in place. User story implementation can now begin.

---

## Phase 3: User Story 1 — Current Month as Default Period (Priority: P1) 🎯 MVP

**Goal**: Users see current calendar month transactions by default; "30 days" remains as separate option

**Independent Test**: Open the app → "This Month" chip is selected → only transactions from 1st of current month through today shown

### Implementation for User Story 1

- [ ] T020 [US1] Update Period enum in `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListState.kt`: add CURRENT_MONTH before TODAY in the enum. Change default in TransactionListState from `Period.MONTH` to `Period.CURRENT_MONTH`
- [ ] T021 [US1] Update periodToRange() in `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListViewModel.kt`: add CURRENT_MONTH case that returns range from 1st day of current month (LocalDate.now().withDayOfMonth(1)) to today. Update _selectedPeriod default to Period.CURRENT_MONTH
- [ ] T022 [US1] Update period chips in `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListScreen.kt`: change chips order to CURRENT_MONTH, TODAY, WEEK, MONTH, YEAR (exclude ALL from visible chips). Update the label mapping to use MoneyManagerTheme.strings.currentMonth for CURRENT_MONTH and strings.thirtyDays for MONTH

**Checkpoint**: User Story 1 complete — app defaults to current calendar month, period chips show correct order with localized labels.

---

## Phase 4: User Story 2 — Custom Date Filtering via DatePicker (Priority: P1)

**Goal**: Users can filter transactions by a specific month or custom date range via a calendar icon and dialog

**Independent Test**: Tap calendar icon → select a month or range → transaction list and summary update, period chips deselect

### Implementation for User Story 2

- [ ] T023 [US2] Add custom date range fields to `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListState.kt`: add `customDateRange: Pair<Long, Long>? = null`, `showDatePickerDialog: Boolean = false`, `customDateLabel: String? = null`
- [ ] T024 [US2] Add custom date filtering logic to `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListViewModel.kt`: add _customDateRange MutableStateFlow, include it in the combine block (when non-null, use it instead of periodToRange), add functions setCustomDateRange(startMillis, endMillis), setCustomMonth(year, month), clearCustomDateRange(), toggleDatePickerDialog(). When selectPeriod() is called, also clear _customDateRange. When custom range is set, update state with customDateLabel showing formatted range
- [ ] T025 [P] [US2] Create DatePickerDialog composable in `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/DatePickerDialog.kt`: dialog with two tabs ("Month" and "Range" using localized strings). Month tab: year navigation (arrows + year label) + 3x4 grid of month buttons. Range tab: Material 3 DateRangePicker. On month select: call onMonthSelected(year, month). On range confirm: call onRangeSelected(startMillis, endMillis). Add testTag "transactionList:datePickerDialog"
- [ ] T026 [US2] Integrate DatePicker into `presentation/transactions/src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/TransactionListScreen.kt`: add calendar IconButton (Icons.Default.DateRange) at end of period chips row with testTag "transactionList:calendarButton". Show DatePickerDialog when state.showDatePickerDialog is true. When customDateRange is active, show a label/chip with state.customDateLabel and testTag "transactionList:customRangeLabel". Ensure no period chip is selected when customDateRange is non-null

**Checkpoint**: User Stories 1 AND 2 complete — full date filtering with period chips, month picker, and range picker all working.

---

## Phase 5: User Story 3 — Recurring Transactions (Priority: P2)

**Goal**: Users can create recurring transaction templates that auto-generate real transactions on schedule

**Independent Test**: Create a monthly recurring → verify it appears in list → on next app startup, pending transactions are generated in the transaction list

### Implementation for User Story 3

- [ ] T027 [P] [US3] Create RecurringTransactionRepository interface in `domain/recurring/src/main/java/com/atelbay/money_manager/domain/recurring/repository/RecurringTransactionRepository.kt`: observeAll(): Flow<List<RecurringTransaction>>, getById(id: Long): RecurringTransaction?, getActiveRecurrings(): List<RecurringTransaction>, save(recurring: RecurringTransaction): Long, delete(id: Long), toggleActive(id: Long, isActive: Boolean), updateLastGeneratedDate(id: Long, date: Long)
- [ ] T028 [P] [US3] Create GetRecurringTransactionsUseCase in `domain/recurring/src/main/java/com/atelbay/money_manager/domain/recurring/usecase/GetRecurringTransactionsUseCase.kt`: @Inject constructor with RecurringTransactionRepository, operator fun invoke(): Flow<List<RecurringTransaction>>
- [ ] T029 [P] [US3] Create SaveRecurringTransactionUseCase in `domain/recurring/src/main/java/com/atelbay/money_manager/domain/recurring/usecase/SaveRecurringTransactionUseCase.kt`: @Inject constructor, suspend operator fun invoke(recurring: RecurringTransaction): Long
- [ ] T030 [P] [US3] Create DeleteRecurringTransactionUseCase in `domain/recurring/src/main/java/com/atelbay/money_manager/domain/recurring/usecase/DeleteRecurringTransactionUseCase.kt`: @Inject constructor, suspend operator fun invoke(id: Long)
- [ ] T031 [US3] Create GeneratePendingTransactionsUseCase in `domain/recurring/src/main/java/com/atelbay/money_manager/domain/recurring/usecase/GeneratePendingTransactionsUseCase.kt`: @Inject constructor with RecurringTransactionRepository + TransactionRepository + AccountRepository. Core logic: get active recurrings, for each calculate missed dates between lastGeneratedDate (or startDate) and today, handle day-of-month overflow (clamp to month length), create TransactionEntity for each missed date, update account balances, update lastGeneratedDate, deactivate if endDate passed. Must run inside Room withTransaction for atomicity
- [ ] T032 [P] [US3] Create RecurringTransactionMapper in `data/recurring/src/main/java/com/atelbay/money_manager/data/recurring/mapper/RecurringTransactionMapper.kt`: extension functions toEntity(RecurringTransaction) → RecurringTransactionEntity and toDomain(RecurringTransactionEntity, categoryName, categoryIcon, categoryColor, accountName) → RecurringTransaction. Map Frequency enum ↔ String, TransactionType ↔ String
- [ ] T033 [US3] Create RecurringTransactionRepositoryImpl in `data/recurring/src/main/java/com/atelbay/money_manager/data/recurring/repository/RecurringTransactionRepositoryImpl.kt`: @Singleton with @Inject constructor(RecurringTransactionDao, CategoryDao, AccountDao, MoneyManagerDatabase). Implement all repository methods. For observeAll(), join with category/account data to provide enriched domain models. Use MoneyManagerDatabase.withTransaction() for atomic generation
- [ ] T034 [US3] Create RecurringDataModule in `data/recurring/src/main/java/com/atelbay/money_manager/data/recurring/di/RecurringDataModule.kt`: @Module @InstallIn(SingletonComponent) abstract class with @Binds to bind RecurringTransactionRepositoryImpl to RecurringTransactionRepository
- [ ] T035 [P] [US3] Create RecurringListState in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/list/RecurringListState.kt`: data class with recurrings: ImmutableList<RecurringTransaction> = persistentListOf(), isLoading: Boolean = true
- [ ] T036 [US3] Create RecurringListViewModel in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/list/RecurringListViewModel.kt`: @HiltViewModel with GetRecurringTransactionsUseCase, DeleteRecurringTransactionUseCase, SaveRecurringTransactionUseCase. Collect recurring list into state. Functions: deleteRecurring(id), toggleActive(id, isActive)
- [ ] T037 [US3] Create RecurringListScreen in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/list/RecurringListScreen.kt`: Scaffold with TopAppBar ("Recurring"), LazyColumn of recurring items (each showing amount, category icon/name, frequency label, next date, active toggle switch), FAB to navigate to edit, swipe-to-delete. Use GlassCard for items, MoneyManagerTheme.strings for labels, testTags on all interactive elements
- [ ] T038 [US3] Create RecurringListRoute in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/list/RecurringListRoute.kt`: @Composable with hiltViewModel<RecurringListViewModel>(), collect state, delegate to RecurringListScreen with callbacks for onAddClick, onEditClick, onBackClick
- [ ] T039 [P] [US3] Create RecurringEditState in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/edit/RecurringEditState.kt`: data class with amount: String = "", type: TransactionType = EXPENSE, categoryId/Name/Icon/Color, accountId/Name, frequency: Frequency = MONTHLY, startDate: Long, endDate: Long?, dayOfMonth: Int = 1, dayOfWeek: Int = 1, note: String = "", isSaving: Boolean = false, showCategoryPicker: Boolean = false, showAccountPicker: Boolean = false, categories/accounts lists
- [ ] T040 [US3] Create RecurringEditViewModel in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/edit/RecurringEditViewModel.kt`: @HiltViewModel with SavedStateHandle (extract id arg), SaveRecurringTransactionUseCase, GetCategoriesUseCase, GetAccountsUseCase. Load existing recurring if editing. Functions: updateAmount, selectType, selectCategory, selectAccount, selectFrequency, setStartDate, setEndDate, setDayOfMonth, setDayOfWeek, updateNote, save()
- [ ] T041 [US3] Create RecurringEditScreen in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/edit/RecurringEditScreen.kt`: Scaffold with TopAppBar, scrollable Column with: amount TextField, type toggle (income/expense), category row (tap opens CategoryBottomSheet pattern), account row, frequency picker (4 chips: Daily/Weekly/Monthly/Yearly), conditional day-of-month picker (1-31 grid) for MONTHLY, conditional day-of-week picker (Mon-Sun) for WEEKLY, start date picker, optional end date picker, note TextField, save Button. Use GlassCard containers, testTags on all fields
- [ ] T042 [US3] Create RecurringEditRoute in `presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/edit/RecurringEditRoute.kt`: @Composable with hiltViewModel<RecurringEditViewModel>(), collect state, delegate to RecurringEditScreen with callbacks for onSaveComplete, onBackClick
- [ ] T043 [US3] Add RecurringList and RecurringEdit destinations to `app/src/main/java/com/atelbay/money_manager/navigation/Destinations.kt`: `@Serializable data object RecurringList` and `@Serializable data class RecurringEdit(val id: Long? = null)`
- [ ] T044 [US3] Add recurring composable routes in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`: add composable<RecurringList> and composable<RecurringEdit> blocks with navigation callbacks, using existing transition patterns
- [ ] T045 [US3] Add "Recurring transactions" row to Settings screen in `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsScreen.kt`: add a new SettingRow in the General section with recurring icon, localized label (strings.recurringTransactions), chevron. Add onRecurringClick callback parameter. Update `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsRoute.kt` to accept and wire the callback
- [ ] T046 [US3] Wire Settings→Recurring navigation in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`: pass onRecurringClick = { navController.navigate(RecurringList) } to SettingsRoute
- [ ] T047 [US3] Add startup generation hook: call GeneratePendingTransactionsUseCase on app startup in `app/src/main/java/com/atelbay/money_manager/MoneyManagerApp.kt` (or MainActivity). Launch coroutine on Dispatchers.IO in Application.onCreate or via an @Inject initializer. Ensure it runs after database is initialized but does not block UI

**Checkpoint**: User Story 3 complete — recurring transactions can be created, edited, deleted, paused/resumed, and auto-generated on app startup.

---

## Phase 6: User Story 4 — Category Budgets (Priority: P2)

**Goal**: Users can set monthly spending limits per category and see color-coded progress

**Independent Test**: Create a budget for "Food" with 100k limit → add food expenses → budget list shows correct percentage with color coding

### Implementation for User Story 4

- [ ] T048 [P] [US4] Create BudgetRepository interface in `domain/budgets/src/main/java/com/atelbay/money_manager/domain/budgets/repository/BudgetRepository.kt`: observeAll(): Flow<List<Budget>>, getById(id: Long): Budget?, getByCategoryId(categoryId: Long): Budget?, save(budget: Budget): Long, delete(id: Long)
- [ ] T049 [P] [US4] Create GetBudgetsWithSpendingUseCase in `domain/budgets/src/main/java/com/atelbay/money_manager/domain/budgets/usecase/GetBudgetsWithSpendingUseCase.kt`: @Inject constructor with BudgetRepository, TransactionDao (or a dedicated query interface), CategoryDao. For each budget: get category info, compute current month start/end millis, use observeExpenseSumByCategory to get spent amount, derive remaining and percentage. Return Flow<List<Budget>> sorted by percentage descending
- [ ] T050 [P] [US4] Create SaveBudgetUseCase in `domain/budgets/src/main/java/com/atelbay/money_manager/domain/budgets/usecase/SaveBudgetUseCase.kt`: @Inject constructor, suspend invoke(categoryId, monthlyLimit). Check if budget exists for category (getByCategoryId) → update existing or insert new
- [ ] T051 [P] [US4] Create DeleteBudgetUseCase in `domain/budgets/src/main/java/com/atelbay/money_manager/domain/budgets/usecase/DeleteBudgetUseCase.kt`: @Inject constructor, suspend invoke(id: Long) → soft delete
- [ ] T052 [P] [US4] Create BudgetMapper in `data/budgets/src/main/java/com/atelbay/money_manager/data/budgets/mapper/BudgetMapper.kt`: extension functions for entity ↔ domain mapping
- [ ] T053 [US4] Create BudgetRepositoryImpl in `data/budgets/src/main/java/com/atelbay/money_manager/data/budgets/repository/BudgetRepositoryImpl.kt`: @Singleton with @Inject constructor(BudgetDao, CategoryDao), implement all repository methods. Map between BudgetEntity and Budget domain model using BudgetMapper. For observeAll(), join with category data to provide enriched domain models (categoryName, categoryIcon, categoryColor). For save(), map Budget domain model to BudgetEntity before persisting
- [ ] T054 [US4] Create BudgetDataModule in `data/budgets/src/main/java/com/atelbay/money_manager/data/budgets/di/BudgetDataModule.kt`: @Module @InstallIn(SingletonComponent) with @Binds for BudgetRepositoryImpl → BudgetRepository
- [ ] T055 [P] [US4] Create BudgetListState in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/list/BudgetListState.kt`: data class with budgets: ImmutableList<Budget> = persistentListOf(), isLoading: Boolean = true
- [ ] T056 [US4] Create BudgetListViewModel in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/list/BudgetListViewModel.kt`: @HiltViewModel with GetBudgetsWithSpendingUseCase, DeleteBudgetUseCase. Collect budgets into state. Function: deleteBudget(id)
- [ ] T057 [US4] Create BudgetListScreen in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/list/BudgetListScreen.kt`: Scaffold with TopAppBar ("Budgets"), LazyColumn of budget items. Each item in GlassCard: category icon + name, LinearProgressIndicator (progress = percentage), spent/limit label formatted with MoneyDisplayFormatter, color coding (green <70%, yellow 70-90%, red >90%), "Over budget" text when exceeded. FAB to add new. Sort by percentage descending. TestTags on all interactive elements
- [ ] T058 [US4] Create BudgetListRoute in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/list/BudgetListRoute.kt`: @Composable with hiltViewModel, collect state, delegate to BudgetListScreen
- [ ] T059 [P] [US4] Create BudgetEditState in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/edit/BudgetEditState.kt`: data class with categoryId/Name/Icon/Color, monthlyLimit: String = "", isSaving: Boolean = false, showCategoryPicker: Boolean = false, expenseCategories list
- [ ] T060 [US4] Create BudgetEditViewModel in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/edit/BudgetEditViewModel.kt`: @HiltViewModel with SavedStateHandle (extract id arg), SaveBudgetUseCase, GetCategoriesUseCase. Load existing budget if editing. Filter categories to expense-only. Functions: selectCategory, updateLimit, save()
- [ ] T061 [US4] Create BudgetEditScreen in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/edit/BudgetEditScreen.kt`: Scaffold with TopAppBar, Column with: category picker row (tap opens bottom sheet, filtered to expense categories), monthly limit amount TextField, save Button. Use GlassCard containers, testTags
- [ ] T062 [US4] Create BudgetEditRoute in `presentation/budgets/src/main/java/com/atelbay/money_manager/presentation/budgets/ui/edit/BudgetEditRoute.kt`: @Composable with hiltViewModel, collect state, delegate to BudgetEditScreen
- [ ] T063 [US4] Add BudgetList and BudgetEdit destinations to `app/src/main/java/com/atelbay/money_manager/navigation/Destinations.kt`: `@Serializable data object BudgetList` and `@Serializable data class BudgetEdit(val id: Long? = null)`
- [ ] T064 [US4] Add budget composable routes in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`: add composable<BudgetList> and composable<BudgetEdit> blocks with navigation callbacks
- [ ] T065 [US4] Add "Budgets" row to Settings screen in `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsScreen.kt`: add SettingRow in General section with budget icon, localized label (strings.budgets), chevron. Add onBudgetsClick callback. Update SettingsRoute to wire callback
- [ ] T066 [US4] Wire Settings→Budgets navigation in `app/src/main/java/com/atelbay/money_manager/navigation/MoneyManagerNavHost.kt`: pass onBudgetsClick = { navController.navigate(BudgetList) } to SettingsRoute

**Checkpoint**: User Story 4 complete — budgets can be created/edited/deleted, progress bars show spending vs. limit with color coding.

---

## Phase 7: User Story 5 — CSV Export (Priority: P3)

**Goal**: Users can export all transactions to a CSV file and share it

**Independent Test**: Tap "Export to CSV" in Settings → share sheet opens → CSV file has correct columns and data

### Implementation for User Story 5

- [ ] T067 [P] [US5] Create ExportTransactionsToCsvUseCase in `domain/transactions/src/main/java/com/atelbay/money_manager/domain/transactions/usecase/ExportTransactionsToCsvUseCase.kt`: @Inject constructor with TransactionRepository, AccountRepository. suspend operator fun invoke(): String. Generate CSV with header "Date,Type,Amount,Category,Account,Note", format date as yyyy-MM-dd, quote fields containing commas/quotes/newlines using RFC 4180 rules. Get all transactions (non-deleted) via TransactionRepository, query AccountRepository.observeAll() to build an accountId-to-accountName map (Transaction domain model has accountId but not accountName), and include the resolved account name in each CSV row. NOTE: requires adding `projects.domain.accounts` to `domain/transactions/build.gradle.kts` dependencies
- [ ] T068 [P] [US5] Add FileProvider to `app/src/main/AndroidManifest.xml`: add `<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true"><meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths"/></provider>`. Create `app/src/main/res/xml/file_paths.xml` with `<paths><cache-path name="exports" path="."/></paths>`
- [ ] T069 [US5] Add CSV export action to Settings: add "Export to CSV" row in `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsScreen.kt` with export icon, localized label (strings.exportCsv). Add onExportCsvClick callback. In `presentation/settings/src/main/java/com/atelbay/money_manager/presentation/settings/ui/SettingsViewModel.kt` (or SettingsRoute): add exportCsv() function that calls ExportTransactionsToCsvUseCase on Dispatchers.IO, writes result to context.cacheDir/money_manager_export_YYYY-MM-DD.csv, creates a share Intent with FileProvider URI (MIME type "text/csv"), handles loading state and error with user-friendly messages. Wire the callback in SettingsRoute and NavHost

**Checkpoint**: User Story 5 complete — CSV export works from Settings with share sheet.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and cleanup

- [ ] T070 Run `./gradlew assembleDebug` to verify full compilation with all 42 modules
- [ ] T071 Verify all new interactive UI elements have testTag modifiers: scan all new Screen composables for missing testTags on buttons, text fields, chips, toggles, FABs, list items
- [ ] T072 Verify all localization strings are provided in all 3 languages (ru/en/kk): check that no AppStrings property uses a hardcoded fallback or placeholder

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T007, T008 must complete for module resolution)
- **US1 (Phase 3)**: Depends on Foundational (T019 for strings)
- **US2 (Phase 4)**: Depends on US1 (shares TransactionListState and ViewModel)
- **US3 (Phase 5)**: Depends on Foundational (T009-T018 for entities/DAOs/models)
- **US4 (Phase 6)**: Depends on Foundational (T009-T018 for entities/DAOs/models)
- **US5 (Phase 7)**: Depends on Foundational (T019 for strings) — no dependency on US3/US4
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Foundational only — no cross-story dependencies
- **US2 (P1)**: Depends on US1 (extends the same State/ViewModel/Screen files)
- **US3 (P2)**: Foundational only — independent of US1/US2
- **US4 (P2)**: Foundational only — independent of US1/US2/US3
- **US5 (P3)**: Foundational only — independent of US1/US2/US3/US4

### Within Each User Story

- Domain models/interfaces before implementations
- Repository impl before use cases that depend on it
- Use cases before ViewModels
- ViewModels before Screens
- Screens before Routes
- Navigation destinations before NavHost wiring

### Parallel Opportunities

- **Phase 1**: All 6 module creation tasks (T001-T006) can run in parallel
- **Phase 2**: Entity, DAO, and model tasks (T009-T016) can run in parallel
- **After Foundational**: US3, US4, and US5 can run in parallel (different modules, no file conflicts)
- **Within US3**: T027-T030 (repo + use cases) can run in parallel; T035, T039 (states) can run in parallel
- **Within US4**: T048-T052 (repo + use cases + mapper) can run in parallel; T055, T059 (states) can run in parallel
- **Within US5**: T067, T068 can run in parallel

---

## Parallel Example: After Foundational

```
# Three independent streams after Phase 2:

# Stream A (US1 → US2): sequential, same files
T020 → T021 → T022 → T023 → T024 → T025 → T026

# Stream B (US3): can run alongside Stream A
T027+T028+T029+T030 (parallel) → T031 → T032+T033 → T034 → T035+T039 (parallel) → T036 → T037 → T038 → T040 → T041 → T042 → T043 → T044 → T045 → T046 → T047

# Stream C (US4): can run alongside Streams A and B
T048+T049+T050+T051+T052 (parallel) → T053 → T054 → T055+T059 (parallel) → T056 → T057 → T058 → T060 → T061 → T062 → T063 → T064 → T065 → T066

# Stream D (US5): can run alongside all above
T067+T068 (parallel) → T069
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 (Current Month period)
4. **STOP and VALIDATE**: Verify default period change works
5. Build passes: `./gradlew assembleDebug`

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → MVP (default period works)
3. US2 → Enhanced filtering (date picker complete)
4. US3 → Recurring transactions (new feature)
5. US4 → Category budgets (new feature)
6. US5 → CSV export (utility feature)
7. Polish → Final verification

### Parallel Team Strategy

With 3 developers after Foundational:
- Developer A: US1 → US2 (sequential, same files)
- Developer B: US3 (recurring — largest scope)
- Developer C: US4 → US5 (budgets then export)

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after completion
- Commit after each task or logical group
- US3 is the largest story (~21 tasks) — consider splitting across sessions
- Room migration (T017-T018) creates BOTH tables at once — must be done before US3 or US4
- AppStrings (T019) adds ALL strings at once to avoid merge conflicts across stories
