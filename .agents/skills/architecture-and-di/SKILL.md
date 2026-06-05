---
name: architecture-and-di
description: "Covers Money Manager's layer-centric multi-module architecture (43 Gradle modules across app/domain/data/presentation/core), Convention Plugins (moneymanager.android.library/feature/hilt), Hilt DI setup, Type-Safe Navigation with 21 @Serializable destinations, and the MVVM UI State pattern. Use when: adding a new feature module or core module, wiring Hilt DI, defining navigation destinations in Destinations.kt or MoneyManagerNavHost.kt, enforcing layer dependency rules (presentation never depends on core:database), troubleshooting 'cannot find repository binding' Hilt errors, or referencing the full module map."
user-invocable: true
---

# Architecture & Dependency Injection

## Context

The project uses a **layer-centric** multi-module architecture (MVVM + Clean Architecture) with Hilt for DI and Convention Plugins to unify Gradle configs. Each feature is split into **3 separate modules**: `domain/{name}`, `data/{name}`, `presentation/{name}`.

**The `feature/` directory was removed** — do not use it as a reference.

**Key files:**
- `build-logic/convention/src/main/kotlin/` — convention plugins
- `app/src/main/java/.../navigation/` — Destinations.kt, MoneyManagerNavHost.kt, TopLevelDestination.kt
- `settings.gradle.kts` — list of all modules
- `gradle/libs.versions.toml` — version catalog

## Gradle Modules

```
MoneyManager/
├── build-logic/convention/          # Convention plugins
├── app/                             # :app — application module
├── domain/                          # Domain layer
│   ├── transactions/                # :domain:transactions — TransactionRepository + UseCases
│   ├── categories/                  # :domain:categories — CategoryRepository + UseCases
│   ├── accounts/                    # :domain:accounts — AccountRepository + UseCases
│   ├── statistics/                  # :domain:statistics — GetPeriodSummaryUseCase
│   ├── import/                      # :domain:import — ParseStatement + ImportTransactions
│   ├── auth/                        # :domain:auth — AuthRepository + SignIn/SignOut UseCases
│   ├── exchangerate/                # :domain:exchangerate — ExchangeRateRepository + UseCases
│   ├── sync/                        # :domain:sync
│   ├── recurring/                   # :domain:recurring
│   ├── budgets/                     # :domain:budgets
│   └── debts/                       # :domain:debts
├── data/                            # Data layer
│   ├── transactions/                # :data:transactions — TransactionRepositoryImpl + mapper
│   ├── categories/                  # :data:categories — CategoryRepositoryImpl + mapper
│   ├── accounts/                    # :data:accounts — AccountRepositoryImpl + mapper
│   ├── auth/                        # :data:auth — FirebaseAuthRepositoryImpl
│   ├── exchangerate/                # :data:exchangerate — Exchange rate API client
│   ├── sync/                        # :data:sync — SyncManager: Room ↔ Firestore
│   ├── recurring/                   # :data:recurring
│   ├── budgets/                     # :data:budgets
│   └── debts/                       # :data:debts
├── presentation/                    # Presentation layer
│   ├── transactions/                # :presentation:transactions — TransactionList + Edit screens
│   ├── categories/                  # :presentation:categories — CategoryList + Edit screens
│   ├── accounts/                    # :presentation:accounts — AccountList + Edit screens
│   ├── statistics/                  # :presentation:statistics — Statistics screen
│   ├── import/                      # :presentation:import — Import + Preview screens
│   ├── settings/                    # :presentation:settings — Settings screen
│   ├── onboarding/                  # :presentation:onboarding — Onboarding screen
│   ├── auth/                        # :presentation:auth — SignIn screen
│   ├── recurring/                   # :presentation:recurring
│   ├── budgets/                     # :presentation:budgets
│   └── debts/                       # :presentation:debts
├── core/
│   ├── model/                       # :core:model — Domain models (pure data classes, no Android)
│   ├── database/                    # :core:database — Room DB, Entities, DAOs
│   ├── datastore/                   # :core:datastore — Preferences DataStore
│   ├── ui/                          # :core:ui — Theme, shared Compose components
│   ├── common/                      # :core:common — Utils, extensions
│   ├── ai/                          # :core:ai — Gemini AI integration
│   ├── parser/                      # :core:parser — RegEx statement parser
│   ├── remoteconfig/                # :core:remoteconfig — Firebase Remote Config
│   ├── auth/                        # :core:auth — CredentialManager wrapper
│   ├── firestore/                   # :core:firestore — Firestore SDK wrapper
│   └── crypto/                      # :core:crypto — Encryption (Tink)
```

## Dependency Rules

- `presentation/{name}` → `domain/{name}` → `core:model`
- `data/{name}` → `domain/{name}` + `core:database`
- Presentation **NEVER** depends on `core:database`
- Domain/data modules do NOT depend on presentation
- Presentation modules do not depend on each other — only on domain and core

## Convention Plugins

| Plugin ID | Purpose | Layer |
|-----------|---------|-------|
| `moneymanager.android.application` | AGP application + Kotlin, compileSdk=36, targetSdk=36, minSdk=29 | :app |
| `moneymanager.android.library` | AGP library + Kotlin | domain/, data/, core/ |
| `moneymanager.android.compose` | Kotlin Compose compiler + Compose BOM + bundles | — |
| `moneymanager.android.hilt` | Hilt + KSP | data/ (required), domain/ (if needed) |
| `moneymanager.android.feature` | library + compose + hilt + lifecycle + navigation | **presentation/ only** |

## Process

### Adding a new core module
1. Create directory `core/{name}/` with `build.gradle.kts`
2. Apply `moneymanager.android.library` (+ `moneymanager.android.hilt` if DI is needed)
3. Register in `settings.gradle.kts`: `include(":core:{name}")`
4. Add dependency in consumers: `implementation(project(":core:{name}"))`

### Adding a new feature (layer-centric: 3 modules)

**Step 1: `domain/{name}/`**
- Plugin: `moneymanager.android.library`
- Dependencies: only `project(":core:model")`
- Contains: repository interface + use cases
- Registration: `include(":domain:{name}")` in `settings.gradle.kts`

**Step 2: `data/{name}/`**
- Plugins: `moneymanager.android.library` + `moneymanager.android.hilt`
- Dependencies: `project(":domain:{name}")` + `project(":core:database")`
- Contains: RepositoryImpl + mapper + Hilt DI module
- Registration: `include(":data:{name}")` in `settings.gradle.kts`

**Step 3: `presentation/{name}/`**
- Plugin: `moneymanager.android.feature`
- Dependencies: `project(":domain:{name}")` + `project(":core:model")`
- Contains: State, ViewModel, Screen, Route
- Registration: `include(":presentation:{name}")` in `settings.gradle.kts`
- Add to `app/build.gradle.kts`: `implementation(project(":presentation:{name}"))`
- Add destination in `Destinations.kt` and route in `MoneyManagerNavHost.kt`

## Navigation (Type-Safe)

21 destinations in `app/.../navigation/Destinations.kt`:
```kotlin
@Serializable data object Onboarding
@Serializable data object OnboardingSetup
@Serializable data object CreateAccount
@Serializable data object Home
@Serializable data class TransactionEdit(val id: Long? = null)
@Serializable data object CategoryList
@Serializable data class CategoryEdit(val id: Long? = null)
@Serializable data object Statistics
@Serializable data class StatisticsCategoryTransactions(val categoryId: Long, val categoryName: String, val categoryIcon: String, val categoryColor: Long, val transactionType: String, val period: String, val startMillis: Long, val endMillis: Long)
@Serializable data object AccountList
@Serializable data class AccountEdit(val id: Long? = null)
@Serializable data object Settings
@Serializable data class Import(val pdfUri: String? = null)
@Serializable data class CurrencyPicker(val activeSide: CurrencyPickerSide = CurrencyPickerSide.FIRST)
@Serializable data object SignIn
@Serializable data object RecurringList
@Serializable data class RecurringEdit(val id: Long? = null)
@Serializable data object BudgetList
@Serializable data class BudgetEdit(val id: Long? = null)
@Serializable data object DebtList
@Serializable data class DebtDetail(val id: Long)
```

4 top-level destinations (Bottom Nav):
```kotlin
enum class TopLevelDestination {
    HOME,        // → Home
    STATISTICS,  // → Statistics
    ACCOUNTS,    // → AccountList
    SETTINGS,    // → Settings
}
```

Navigation flow:
```
App Launch → check isOnboardingCompleted
  ├─ false → Onboarding → OnboardingSetup → CreateAccount → Home
  └─ true  → Home

Home (TransactionList)
  ├─ Tap → TransactionEdit(id)
  ├─ FAB → TransactionEdit()
  ├─ Import icon → Import
  └─ Bottom Nav → Statistics | AccountList | Settings

Settings
  ├─ Categories → CategoryList → CategoryEdit
  └─ Account → SignIn (optional Google Sign-In)

AccountList → Tap → AccountEdit(id) / FAB → AccountEdit()
Statistics → CurrencyPicker (base/quote currency selection)
           → StatisticsCategoryTransactions (drill-down by category)

RecurringList → RecurringEdit(id) / FAB → RecurringEdit()
BudgetList → BudgetEdit(id) / FAB → BudgetEdit()
DebtList → DebtDetail(id)
```

## UI State Pattern

- Single `data class *State` per screen
- ViewModel with direct methods (not Intent/Event pattern)
- Screen — stateless composable (receives state + callbacks)
- Route — stateful wrapper, collects state from ViewModel via `collectAsStateWithLifecycle()`

## Quality Bar

- Presentation modules do NOT depend on each other — only on domain and core
- `:core:model` has no Android dependencies (pure Kotlin)
- Hilt modules (`@Module @InstallIn`) are placed in the `di/` package of each data module
- All new destinations must be `@Serializable`
- Use version catalog (`libs.versions.toml`) for all dependencies — never hardcode versions

## Anti-patterns

- Do NOT create a `feature/` directory — it does not exist; use the layer-centric structure
- Do NOT add `core:database` to a presentation module
- Do NOT add direct dependencies between presentation modules
- Do NOT place domain logic in the `:app` module
- Do NOT create `@Module` without `@InstallIn` — always specify the component (SingletonComponent, ViewModelComponent, etc.)
- Do NOT use `kapt` — the project uses KSP
- Do NOT hardcode compileSdk/minSdk in modules — these are set via convention plugins
