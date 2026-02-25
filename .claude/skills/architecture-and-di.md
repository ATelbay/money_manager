---
description: "Архитектура проекта Money Manager: многомодульность, Gradle Convention Plugins, Hilt DI, Type-Safe Navigation, паттерн UI State (MVVM + Clean Architecture)"
---

# Architecture & Dependency Injection

## Context

Проект использует многомодульную архитектуру (MVVM + Clean Architecture) с Hilt для DI и Convention Plugins для унификации Gradle-конфигов.

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
├── core/
│   ├── database/                    # :core:database — Room DB
│   ├── datastore/                   # :core:datastore — Preferences DataStore
│   ├── model/                       # :core:model — Domain models (чистые data class, без Android-зависимостей)
│   ├── ui/                          # :core:ui — Theme, общие Compose-компоненты
│   ├── common/                      # :core:common — Utils, extensions
│   ├── ai/                          # :core:ai — Gemini AI integration
│   ├── parser/                      # :core:parser — RegEx statement parser
│   └── remoteconfig/                # :core:remoteconfig — Firebase Remote Config
├── feature/
│   ├── onboarding/                  # :feature:onboarding
│   ├── transactions/                # :feature:transactions
│   ├── categories/                  # :feature:categories
│   ├── accounts/                    # :feature:accounts
│   ├── statistics/                  # :feature:statistics
│   ├── settings/                    # :feature:settings
│   └── import/                      # :feature:import
```

## Convention Plugins

| Plugin ID | Назначение |
|-----------|------------|
| `moneymanager.android.application` | AGP application + Kotlin, compileSdk=36, targetSdk=36, minSdk=29 |
| `moneymanager.android.library` | AGP library + Kotlin |
| `moneymanager.android.compose` | Kotlin Compose compiler + Compose BOM + bundles |
| `moneymanager.android.hilt` | Hilt + KSP |
| `moneymanager.android.feature` | library + compose + hilt + lifecycle + navigation |

## Process

### Добавление нового core-модуля
1. Создать директорию `core/{name}/` с `build.gradle.kts`
2. Применить `moneymanager.android.library` (+ `moneymanager.android.hilt` если нужен DI)
3. Зарегистрировать в `settings.gradle.kts`: `include(":core:{name}")`
4. Добавить зависимость в потребителей: `implementation(project(":core:{name}"))`

### Добавление нового feature-модуля
1. Создать директорию `feature/{name}/` с `build.gradle.kts`
2. Применить `moneymanager.android.feature` (включает library + compose + hilt + lifecycle + navigation)
3. Зарегистрировать в `settings.gradle.kts`: `include(":feature:{name}")`
4. Добавить зависимость в `:app`: `implementation(project(":feature:{name}"))`
5. Добавить destination в `app/.../navigation/Destinations.kt`
6. Добавить route в `MoneyManagerNavHost.kt`

## Навигация (Type-Safe)

12 destinations в `app/.../navigation/Destinations.kt`:
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

Settings → Категории → CategoryList → CategoryEdit
AccountList → Tap → AccountEdit(id) / FAB → AccountEdit()
```

## Паттерн UI State

- Единый `data class *State` для каждого экрана
- ViewModel с прямыми методами (не Intent/Event паттерн)
- Screen — stateless composable (принимает state + callbacks)
- Route — stateful wrapper, собирает state из ViewModel

## Quality Bar

- Feature-модули НЕ зависят друг от друга — только от core-модулей
- `:core:model` не имеет Android-зависимостей (чистый Kotlin)
- Hilt-модули (`@Module @InstallIn`) располагаются в `di/` пакете каждого модуля
- Все новые destinations должны быть `@Serializable`
- Используй version catalog (`libs.versions.toml`) для всех зависимостей, НЕ хардкодь версии

## Anti-patterns

- НЕ добавляй прямые зависимости между feature-модулями
- НЕ размещай domain-логику в `:app` модуле
- НЕ создавай `@Module` без `@InstallIn` — всегда указывай компонент (SingletonComponent, ViewModelComponent и т.д.)
- НЕ используй `kapt` — проект использует KSP
- НЕ хардкодь compileSdk/minSdk в модулях — это задаётся через convention plugins
