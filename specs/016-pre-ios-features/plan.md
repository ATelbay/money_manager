# Implementation Plan: Pre-iOS Port Feature Bundle

**Branch**: `016-pre-ios-features` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/016-pre-ios-features/spec.md`

## Summary

Implement 5 features before iOS port: (1) add `CURRENT_MONTH` period as default, (2) date picker with month/range modes on Home screen, (3) recurring transactions with auto-generation, (4) category budgets with progress bars, (5) CSV export via ShareSheet. This requires 6 new Gradle modules (`domain/recurring`, `data/recurring`, `presentation/recurring`, `domain/budgets`, `data/budgets`, `presentation/budgets`), 2 new Room entities with migration 4→5, modifications to the transaction list ViewModel/Screen/State, new Settings rows, new navigation destinations, and AppStrings localization updates.

## Technical Context

**Language/Version**: Kotlin 2.3.0, KSP 2.3.1, AGP 8.13.2
**Primary Dependencies**: Jetpack Compose (BOM 2026.01.01), Material 3, Hilt 2.58, Room 2.8.4, Navigation Compose 2.9.7, kotlinx-collections-immutable, Coroutines 1.10.2
**Storage**: Room (SQLite) — `money_manager.db` (currently version 4, 3 entities). Preferences DataStore for settings.
**Testing**: JUnit 4, MockK 1.14.9, Turbine 1.2.1, kotlinx-coroutines-test, ComposeTestRule (instrumented)
**Target Platform**: Android (minSdk per project config)
**Project Type**: Mobile app (Android)
**Performance Goals**: Recurring transaction generation < 2s on startup; CSV export < 10s for 5,000 transactions; 60 fps UI
**Constraints**: Offline-capable, Room as source of truth, no XML layouts, all Compose
**Scale/Scope**: ~36→42 Gradle modules, 4 new screens, single-user local app

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | New features follow 3-module pattern (domain/data/presentation). Features 1, 2, 5 modify existing modules only. |
| II. Kotlin-First & Jetpack Compose | PASS | All new UI in Compose, state hoisting, testTags required. |
| III. Material 3 Design System | PASS | DateRangePicker, FilterChip, LinearProgressIndicator from M3. GlassCard for containers. |
| IV. Animation & Motion | PASS | New screens use existing navigation transitions from MoneyManagerNavHost. No custom animations needed. |
| V. Hilt Dependency Injection | PASS | @HiltViewModel for new VMs, @Binds for new repos, @Inject for use cases. |
| VI. Room Database | PASS | New entities in core:database, explicit migration 4→5, exportSchema=true, Flow DAOs. |
| VII. Testing Architecture | DEFERRED | Tests will be added per constitution requirements but are not the focus of this plan. |
| VIII. Firebase Ecosystem | PASS | No Firebase changes. Recurring/Budget entities include remoteId for future sync. |
| IX. Type-Safe Navigation | PASS | New @Serializable destinations for recurring and budget screens. |
| X. Statement Import Pipeline | N/A | No import changes. |
| XI. Preferences DataStore | N/A | No new preferences. |

**Gate result: PASS** — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/016-pre-ios-features/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# New modules (6 total)
domain/recurring/
└── src/main/java/com/atelbay/money_manager/domain/recurring/
    ├── repository/RecurringTransactionRepository.kt
    └── usecase/
        ├── GetRecurringTransactionsUseCase.kt
        ├── SaveRecurringTransactionUseCase.kt
        ├── DeleteRecurringTransactionUseCase.kt
        └── GeneratePendingTransactionsUseCase.kt

data/recurring/
└── src/main/java/com/atelbay/money_manager/data/recurring/
    ├── di/RecurringDataModule.kt
    ├── mapper/RecurringTransactionMapper.kt
    └── repository/RecurringTransactionRepositoryImpl.kt

presentation/recurring/
└── src/main/java/com/atelbay/money_manager/presentation/recurring/ui/
    ├── list/
    │   ├── RecurringListRoute.kt
    │   ├── RecurringListScreen.kt
    │   ├── RecurringListState.kt
    │   └── RecurringListViewModel.kt
    └── edit/
        ├── RecurringEditRoute.kt
        ├── RecurringEditScreen.kt
        ├── RecurringEditState.kt
        └── RecurringEditViewModel.kt

domain/budgets/
└── src/main/java/com/atelbay/money_manager/domain/budgets/
    ├── repository/BudgetRepository.kt
    └── usecase/
        ├── GetBudgetsWithSpendingUseCase.kt
        ├── SaveBudgetUseCase.kt
        └── DeleteBudgetUseCase.kt

data/budgets/
└── src/main/java/com/atelbay/money_manager/data/budgets/
    ├── di/BudgetDataModule.kt
    ├── mapper/BudgetMapper.kt
    └── repository/BudgetRepositoryImpl.kt

presentation/budgets/
└── src/main/java/com/atelbay/money_manager/presentation/budgets/ui/
    ├── list/
    │   ├── BudgetListRoute.kt
    │   ├── BudgetListScreen.kt
    │   ├── BudgetListState.kt
    │   └── BudgetListViewModel.kt
    └── edit/
        ├── BudgetEditRoute.kt
        ├── BudgetEditScreen.kt
        ├── BudgetEditState.kt
        └── BudgetEditViewModel.kt

# Modified modules
core/database/
└── src/main/java/com/atelbay/money_manager/core/database/
    ├── MoneyManagerDatabase.kt           # version 4→5, add entities + DAOs
    ├── entity/RecurringTransactionEntity.kt  # NEW
    ├── entity/BudgetEntity.kt                # NEW
    ├── dao/RecurringTransactionDao.kt        # NEW
    ├── dao/BudgetDao.kt                      # NEW
    └── migration/Migration_4_5.kt            # NEW

core/model/
└── src/main/java/com/atelbay/money_manager/core/model/
    ├── RecurringTransaction.kt               # NEW
    ├── Budget.kt                             # NEW
    └── Frequency.kt                          # NEW (enum: DAILY, WEEKLY, MONTHLY, YEARLY)

core/ui/
└── src/main/java/com/atelbay/money_manager/core/ui/
    └── theme/AppStrings.kt                   # MODIFIED — add ~30 new strings

domain/transactions/
└── src/main/java/com/atelbay/money_manager/domain/transactions/usecase/
    └── ExportTransactionsToCsvUseCase.kt     # NEW

presentation/transactions/
└── src/main/java/com/atelbay/money_manager/presentation/transactions/ui/list/
    ├── TransactionListState.kt               # MODIFIED — add CURRENT_MONTH, customDateRange
    ├── TransactionListViewModel.kt           # MODIFIED — add periodToRange(CURRENT_MONTH), custom date flow
    └── TransactionListScreen.kt              # MODIFIED — add calendar icon, DatePicker dialog

presentation/settings/
└── src/main/java/com/atelbay/money_manager/presentation/settings/ui/
    ├── SettingsScreen.kt                     # MODIFIED — add Recurring, Budgets, Export rows
    ├── SettingsRoute.kt                      # MODIFIED — add navigation callbacks
    └── SettingsViewModel.kt                  # MODIFIED — add CSV export action

app/
└── src/main/java/com/atelbay/money_manager/
    ├── navigation/Destinations.kt            # MODIFIED — add 4 new destinations
    ├── navigation/MoneyManagerNavHost.kt     # MODIFIED — add 4 new composable<T> blocks
    └── MoneyManagerApp.kt                    # MODIFIED — call GeneratePendingTransactionsUseCase on startup

app/src/main/
    ├── AndroidManifest.xml                   # MODIFIED — add FileProvider
    └── res/xml/file_paths.xml                # NEW — FileProvider paths config
```

**Structure Decision**: Follows existing layer-centric multi-module architecture. Features 3 (Recurring) and 4 (Budgets) each get the standard 3-module scaffold (domain/data/presentation). Features 1, 2, 5 modify existing modules only. All new entities go in `core:database`, all new domain models in `core:model`.
