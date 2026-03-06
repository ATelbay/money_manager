---
description: "Архитектура проекта Money Manager: layer-centric многомодульность, Gradle Convention Plugins, Hilt DI, Type-Safe Navigation, паттерн UI State (MVVM + Clean Architecture)"
---

# Architecture & Dependency Injection

## Context

Проект использует **layer-centric** многомодульную архитектуру (MVVM + Clean Architecture) с Hilt для DI и Convention Plugins для унификации Gradle-конфигов. Каждая фича разбита на **3 отдельных модуля**: `domain/{name}`, `data/{name}`, `presentation/{name}`.

**Директория `feature/` была удалена** — не используй её как образец.

**Ключевые файлы:**
- `build-logic/convention/src/main/kotlin/` — convention plugins
- `app/src/main/java/.../navigation/` — Destinations.kt, MoneyManagerNavHost.kt, TopLevelDestination.kt
- `settings.gradle.kts` — список всех модулей
- `gradle/libs.versions.toml` — version catalog

## Gradle-модули

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
│   └── exchangerate/                # :domain:exchangerate — ExchangeRateRepository + UseCases
├── data/                            # Data layer
│   ├── transactions/                # :data:transactions — TransactionRepositoryImpl + mapper
│   ├── categories/                  # :data:categories — CategoryRepositoryImpl + mapper
│   ├── accounts/                    # :data:accounts — AccountRepositoryImpl + mapper
│   ├── auth/                        # :data:auth — FirebaseAuthRepositoryImpl
│   ├── exchangerate/                # :data:exchangerate — Exchange rate API client
│   └── sync/                        # :data:sync — SyncManager: Room ↔ Firestore
├── presentation/                    # Presentation layer
│   ├── transactions/                # :presentation:transactions — TransactionList + Edit screens
│   ├── categories/                  # :presentation:categories — CategoryList + Edit screens
│   ├── accounts/                    # :presentation:accounts — AccountList + Edit screens
│   ├── statistics/                  # :presentation:statistics — Statistics screen
│   ├── import/                      # :presentation:import — Import + Preview screens
│   ├── settings/                    # :presentation:settings — Settings screen
│   ├── onboarding/                  # :presentation:onboarding — Onboarding screen
│   └── auth/                        # :presentation:auth — SignIn screen
├── core/
│   ├── model/                       # :core:model — Domain models (чистые data class, без Android)
│   ├── database/                    # :core:database — Room DB, Entities, DAOs
│   ├── datastore/                   # :core:datastore — Preferences DataStore
│   ├── ui/                          # :core:ui — Theme, общие Compose-компоненты
│   ├── common/                      # :core:common — Utils, extensions
│   ├── ai/                          # :core:ai — Gemini AI integration
│   ├── parser/                      # :core:parser — RegEx statement parser
│   ├── remoteconfig/                # :core:remoteconfig — Firebase Remote Config
│   ├── auth/                        # :core:auth — CredentialManager wrapper
│   └── firestore/                   # :core:firestore — Firestore SDK wrapper
└── components/                      # Design system
    ├── design-system/               # :components:design-system
    ├── ui/                          # :components:ui
    └── showcase/                    # :components:showcase
```

## Правила зависимостей

- `presentation/{name}` → `domain/{name}` → `core:model`
- `data/{name}` → `domain/{name}` + `core:database`
- Presentation **НИКОГДА** не зависит от `core:database`
- Domain/data модули НЕ зависят от presentation
- Presentation-модули не зависят друг от друга — только от domain и core

## Convention Plugins

| Plugin ID | Назначение | Слой |
|-----------|------------|------|
| `moneymanager.android.application` | AGP application + Kotlin, compileSdk=36, targetSdk=36, minSdk=29 | :app |
| `moneymanager.android.library` | AGP library + Kotlin | domain/, data/, core/ |
| `moneymanager.android.compose` | Kotlin Compose compiler + Compose BOM + bundles | — |
| `moneymanager.android.hilt` | Hilt + KSP | data/ (обязательно), domain/ (если нужен) |
| `moneymanager.android.feature` | library + compose + hilt + lifecycle + navigation | **только presentation/** |

## Process

### Добавление нового core-модуля
1. Создать директорию `core/{name}/` с `build.gradle.kts`
2. Применить `moneymanager.android.library` (+ `moneymanager.android.hilt` если нужен DI)
3. Зарегистрировать в `settings.gradle.kts`: `include(":core:{name}")`
4. Добавить зависимость в потребителей: `implementation(project(":core:{name}"))`

### Добавление новой фичи (layer-centric: 3 модуля)

**Шаг 1: `domain/{name}/`**
- Plugin: `moneymanager.android.library`
- Зависимости: только `project(":core:model")`
- Содержит: repository interface + use cases
- Регистрация: `include(":domain:{name}")` в `settings.gradle.kts`

**Шаг 2: `data/{name}/`**
- Plugins: `moneymanager.android.library` + `moneymanager.android.hilt`
- Зависимости: `project(":domain:{name}")` + `project(":core:database")`
- Содержит: RepositoryImpl + mapper + Hilt DI module
- Регистрация: `include(":data:{name}")` в `settings.gradle.kts`

**Шаг 3: `presentation/{name}/`**
- Plugin: `moneymanager.android.feature`
- Зависимости: `project(":domain:{name}")` + `project(":core:model")`
- Содержит: State, ViewModel, Screen, Route
- Регистрация: `include(":presentation:{name}")` в `settings.gradle.kts`
- Добавить в `app/build.gradle.kts`: `implementation(project(":presentation:{name}"))`
- Добавить destination в `Destinations.kt` и route в `MoneyManagerNavHost.kt`

## Навигация (Type-Safe)

13 destinations в `app/.../navigation/Destinations.kt`:
```kotlin
@Serializable data object Onboarding
@Serializable data object CreateAccount
@Serializable data object Home
@Serializable data class TransactionEdit(val id: Long? = null)
@Serializable data object CategoryList
@Serializable data class CategoryEdit(val id: Long? = null)
@Serializable data object Statistics
@Serializable data object AccountList
@Serializable data class AccountEdit(val id: Long? = null)
@Serializable data object Settings
@Serializable data object Import
@Serializable data class CurrencyPicker(val activeSide: CurrencyPickerSide = CurrencyPickerSide.BASE)
@Serializable data object SignIn
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
App Launch → проверка isOnboardingCompleted
  ├─ false → Onboarding → CreateAccount → Home
  └─ true  → Home

Home (TransactionList)
  ├─ Tap → TransactionEdit(id)
  ├─ FAB → TransactionEdit()
  ├─ Import icon → Import
  └─ Bottom Nav → Statistics | AccountList | Settings

Settings
  ├─ Категории → CategoryList → CategoryEdit
  └─ Аккаунт → SignIn (опциональный Google Sign-In)

AccountList → Tap → AccountEdit(id) / FAB → AccountEdit()
Statistics → CurrencyPicker (выбор базовой/котируемой валюты)
```

## Паттерн UI State

- Единый `data class *State` для каждого экрана
- ViewModel с прямыми методами (не Intent/Event паттерн)
- Screen — stateless composable (принимает state + callbacks)
- Route — stateful wrapper, собирает state из ViewModel через `collectAsStateWithLifecycle()`

## Quality Bar

- Presentation-модули НЕ зависят друг от друга — только от domain и core
- `:core:model` не имеет Android-зависимостей (чистый Kotlin)
- Hilt-модули (`@Module @InstallIn`) располагаются в `di/` пакете каждого data-модуля
- Все новые destinations должны быть `@Serializable`
- Используй version catalog (`libs.versions.toml`) для всех зависимостей, НЕ хардкодь версии

## Anti-patterns

- НЕ создавай `feature/` директорию — её не существует, используй layer-centric структуру
- НЕ добавляй `core:database` в presentation-модуль
- НЕ добавляй прямые зависимости между presentation-модулями
- НЕ размещай domain-логику в `:app` модуле
- НЕ создавай `@Module` без `@InstallIn` — всегда указывай компонент (SingletonComponent, ViewModelComponent и т.д.)
- НЕ используй `kapt` — проект использует KSP
- НЕ хардкодь compileSdk/minSdk в модулях — это задаётся через convention plugins
