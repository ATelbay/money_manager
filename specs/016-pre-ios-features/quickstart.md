# Quickstart: Pre-iOS Port Feature Bundle

**Branch**: `016-pre-ios-features` | **Date**: 2026-03-27

## Prerequisites

- Android Studio with Kotlin 2.3.0
- Project builds successfully: `./gradlew assembleDebug`

## Implementation Order

Features should be implemented in this order due to dependencies:

1. **Feature 1: CURRENT_MONTH period** — No dependencies, touches existing files only
2. **Feature 2: DatePicker** — Depends on Feature 1 (shared state/ViewModel changes)
3. **Feature 5: CSV Export** — No dependencies on 3/4, simple scope
4. **Feature 3: Recurring Transactions** — Room migration, new modules
5. **Feature 4: Category Budgets** — Room migration (same as #3), new modules

Features 3 and 4 share a single Room migration (4→5) and must be coordinated.

## New Module Setup

For each new module pair, create these files:

### 1. Module directories and build.gradle.kts

```bash
# Domain module
mkdir -p domain/recurring/src/main/java/com/atelbay/money_manager/domain/recurring/{repository,usecase}

# Data module
mkdir -p data/recurring/src/main/java/com/atelbay/money_manager/data/recurring/{di,mapper,repository}

# Presentation module
mkdir -p presentation/recurring/src/main/java/com/atelbay/money_manager/presentation/recurring/ui/{list,edit}
```

Repeat for `budgets`.

### 2. Register in settings.gradle.kts

```kotlin
include(":domain:recurring")
include(":data:recurring")
include(":presentation:recurring")
include(":domain:budgets")
include(":data:budgets")
include(":presentation:budgets")
```

### 3. Add dependencies in app/build.gradle.kts

```kotlin
implementation(projects.domain.recurring)
implementation(projects.data.recurring)
implementation(projects.presentation.recurring)
implementation(projects.domain.budgets)
implementation(projects.data.budgets)
implementation(projects.presentation.budgets)
```

## Key Patterns to Follow

| Pattern | Reference File |
|---------|---------------|
| Domain build.gradle.kts | `domain/transactions/build.gradle.kts` |
| Data build.gradle.kts | `data/transactions/build.gradle.kts` |
| Presentation build.gradle.kts | `presentation/transactions/build.gradle.kts` |
| Repository interface | `domain/transactions/repository/TransactionRepository.kt` |
| Repository impl + DI | `data/transactions/di/TransactionDataModule.kt` |
| Entity + sync fields | `core/database/entity/TransactionEntity.kt` |
| DAO with Flow | `core/database/dao/TransactionDao.kt` |
| Domain model (enriched) | `core/model/Transaction.kt` |
| Migration file | `core/database/migration/Migration_3_4.kt` |
| ViewModel combine | `presentation/transactions/ui/list/TransactionListViewModel.kt` |
| State class + ImmutableList | `presentation/transactions/ui/list/TransactionListState.kt` |
| Route/Screen split | `presentation/transactions/ui/edit/TransactionEditRoute.kt` |
| Settings rows | `presentation/settings/ui/SettingsScreen.kt` |
| Navigation destination | `app/navigation/Destinations.kt` |
| AppStrings localization | `core/ui/theme/AppStrings.kt` |

## Verification

After each feature:
```bash
./gradlew assembleDebug
```

After all features:
```bash
./gradlew assembleDebug test lint detekt
```
