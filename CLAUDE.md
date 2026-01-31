# Money Manager — Personal Finance Tracker

## Обзор проекта

Money Manager — Android-приложение для учёта личных финансов. Разрабатывается как часть магистерской диссертации, посвящённой UI-автоматизации тестирования.

**Цель:** создать приложение с разнообразными UI-паттернами для последующего покрытия автоматизированными тестами.

**Package:** `com.atelbay.money_manager`

## Технический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| UI | Jetpack Compose + Material 3 | BOM 2026.01.01 |
| DI | Hilt | 2.58 |
| Database | Room | 2.8.4 |
| Navigation | Navigation Compose (type-safe) | 2.9.7 |
| Architecture | MVVM + Clean Architecture | — |
| Async | Coroutines + Flow | 1.10.2 |
| DataStore | Preferences DataStore | 1.1.7 |
| Build | Version Catalogs + Convention Plugins | AGP 8.13.2, Kotlin 2.3.0, KSP 2.3.1 |
| Charts | Vico | 2.4.3 |
| CI/CD | GitHub Actions → Firebase App Distribution → Play Store | — |

## Архитектура

### Gradle-модули

```
MoneyManager/
├── build-logic/                    # Convention plugins
│   └── convention/
│       └── src/main/kotlin/
│           ├── AndroidApplicationConventionPlugin.kt
│           ├── AndroidLibraryConventionPlugin.kt
│           ├── AndroidComposeConventionPlugin.kt
│           ├── AndroidHiltConventionPlugin.kt
│           ├── AndroidFeatureConventionPlugin.kt
│           └── com/atelbay/money_manager/KotlinAndroid.kt
│
├── app/                            # :app — application module
│   └── src/main/java/com/atelbay/money_manager/
│       ├── MainActivity.kt
│       ├── MoneyManagerApp.kt      # @HiltAndroidApp
│       └── navigation/
│           ├── Destinations.kt     # @Serializable destinations
│           └── MoneyManagerNavHost.kt
│
├── core/
│   ├── database/                   # :core:database — Room DB
│   │   └── src/main/java/com/atelbay/money_manager/core/database/
│   │       ├── MoneyManagerDatabase.kt
│   │       ├── DefaultCategories.kt
│   │       ├── entity/
│   │       │   ├── AccountEntity.kt
│   │       │   ├── CategoryEntity.kt
│   │       │   └── TransactionEntity.kt
│   │       ├── dao/
│   │       │   ├── AccountDao.kt
│   │       │   ├── CategoryDao.kt
│   │       │   └── TransactionDao.kt
│   │       └── di/
│   │           └── DatabaseModule.kt
│   │
│   ├── datastore/                  # :core:datastore — Preferences
│   │   └── src/main/java/com/atelbay/money_manager/core/datastore/
│   │       ├── UserPreferences.kt
│   │       └── di/
│   │           └── DataStoreModule.kt
│   │
│   ├── ui/                         # :core:ui — Theme & Components
│   │   └── src/main/java/com/atelbay/money_manager/core/ui/
│   │       ├── theme/
│   │       │   ├── Color.kt
│   │       │   ├── Type.kt
│   │       │   └── Theme.kt
│   │       └── components/
│   │           ├── MoneyManagerButton.kt
│   │           ├── MoneyManagerTextField.kt
│   │           └── MoneyManagerCard.kt
│   │
│   └── common/                     # :core:common — Utils, extensions
│
├── feature/
│   ├── onboarding/                 # :feature:onboarding
│   │   └── src/main/java/com/atelbay/money_manager/feature/onboarding/ui/
│   │       ├── OnboardingScreen.kt
│   │       ├── OnboardingRoute.kt
│   │       ├── OnboardingViewModel.kt
│   │       ├── OnboardingState.kt
│   │       ├── CreateAccountScreen.kt
│   │       ├── CreateAccountRoute.kt
│   │       ├── CreateAccountViewModel.kt
│   │       └── CreateAccountState.kt
│   │
│   └── transactions/               # :feature:transactions
│       └── src/main/java/com/atelbay/money_manager/feature/transactions/
│           ├── domain/
│           │   ├── model/
│           │   │   ├── Transaction.kt
│           │   │   └── Category.kt
│           │   ├── repository/
│           │   │   └── TransactionRepository.kt
│           │   └── usecase/
│           │       ├── GetTransactionsUseCase.kt
│           │       ├── GetTransactionByIdUseCase.kt
│           │       ├── GetCategoriesUseCase.kt
│           │       ├── SaveTransactionUseCase.kt
│           │       └── DeleteTransactionUseCase.kt
│           ├── data/
│           │   ├── mapper/TransactionMapper.kt
│           │   └── repository/TransactionRepositoryImpl.kt
│           ├── di/
│           │   └── TransactionModule.kt
│           └── ui/
│               ├── list/
│               │   ├── TransactionListScreen.kt
│               │   ├── TransactionListRoute.kt
│               │   ├── TransactionListViewModel.kt
│               │   └── TransactionListState.kt
│               └── edit/
│                   ├── TransactionEditScreen.kt
│                   ├── TransactionEditRoute.kt
│                   ├── TransactionEditViewModel.kt
│                   └── TransactionEditState.kt
│
├── gradle/libs.versions.toml      # Version catalog
└── settings.gradle.kts
```

### Convention Plugins

| Plugin ID | Назначение |
|-----------|------------|
| `moneymanager.android.application` | AGP application + Kotlin, compileSdk=36, targetSdk=36, minSdk=29 |
| `moneymanager.android.library` | AGP library + Kotlin |
| `moneymanager.android.compose` | Kotlin Compose compiler + Compose BOM + bundles |
| `moneymanager.android.hilt` | Hilt + KSP |
| `moneymanager.android.feature` | library + compose + hilt + lifecycle + navigation |

### Навигация (Type-Safe)

4 destinations в `app/.../navigation/Destinations.kt`:
```kotlin
@Serializable data object Onboarding
@Serializable data object CreateAccount
@Serializable data object Home
@Serializable data class TransactionEdit(val id: Long? = null)
```

Flow: `Onboarding → CreateAccount → Home ↔ TransactionEdit`

При запуске `MainActivity` проверяет `UserPreferences.isOnboardingCompleted` и выбирает `startDestination`.

### Паттерн UI State

- Единый `data class *State` для каждого экрана
- ViewModel с прямыми методами (не Intent/Event)
- Screen — stateless composable (принимает state + callbacks)
- Route — stateful wrapper, собирает state из ViewModel

## База данных (Room)

3 entities: `AccountEntity`, `CategoryEntity`, `TransactionEntity` (с ForeignKeys).
15 предустановленных категорий (10 расход + 5 доход) в `DefaultCategories.kt`.
Prepopulation через `RoomDatabase.Callback.onCreate`.

## Приоритеты реализации

### P0 — Критический путь ✅
1. ~~**Core**: DI setup, Room database, Theme~~
2. ~~**Onboarding**: Welcome screens, Create account, Currency selection~~
3. ~~**Transactions**: List (главный экран), Add/Edit form~~

### P1 — Основной функционал
4. **Categories**: List, Add/Edit with icon & color picker
5. **Statistics**: Pie chart по категориям, Line chart динамики

### P2 — Расширение
6. **Accounts**: Manage multiple accounts, Switch between them

## UI-сценарии для тестирования

| Сценарий | Экран | Элементы |
|----------|-------|----------|
| Multi-step flow | Onboarding | HorizontalPager, кнопки навигации |
| Form validation | Transaction Edit, Create Account | TextField, валидация, error states |
| List operations | Transaction List | LazyColumn, swipe-to-delete |
| Modal selection | Category picker | ModalBottomSheet с LazyVerticalGrid |
| Date picking | Transaction Edit | DatePickerDialog |
| Dropdown | Create Account | ExposedDropdownMenuBox для валюты |
| Chart interaction | Statistics | Vico charts, touch feedback |
| CRUD | Categories | Create, Read, Update, Delete flow |
| Navigation | Все | Type-safe routes, back stack |

## Code Style

### Naming
- Screens: `{Feature}Screen.kt` — stateless composable
- Routes: `{Feature}Route.kt` — stateful wrapper с ViewModel
- ViewModels: `{Feature}ViewModel.kt`
- States: `{Feature}State.kt`
- UseCases: `{Action}{Entity}UseCase.kt` (e.g., `GetTransactionsUseCase`)
- Repositories: `{Entity}Repository.kt` (interface), `{Entity}RepositoryImpl.kt`

### Compose
- Modifier как первый необязательный параметр
- State hoisting: Screen не знает о ViewModel
- `ImmutableList` для стабильных коллекций
- `testTag` naming: `"screen:element"` (e.g., `"transactionList:fab"`, `"transactionEdit:amountField"`)

## Полезные команды

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
./gradlew detekt
```

## TODO / Не в MVP

- [ ] Бюджеты и лимиты по категориям
- [ ] Повторяющиеся транзакции
- [ ] Экспорт в CSV/PDF
- [ ] PIN-код / биометрия
- [ ] Мультивалютность с конвертацией
- [ ] Облачная синхронизация
- [ ] Widgets
