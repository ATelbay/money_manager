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
| AI | Firebase AI (Gemini 2.5 Flash) | — |
| PDF Parsing | PdfBox-Android | 2.0.27.0 |
| Remote Config | Firebase Remote Config | — |
| CI/CD | GitHub Actions → Firebase App Distribution → Play Store | — |

## Архитектура (Layer-Centric Modules)

~36 Gradle-модулей с enforced layer boundaries:

```
MoneyManager/
├── domain/                    # Repository interfaces + Use Cases
│   ├── transactions/          # TransactionRepository + CRUD use cases
│   ├── categories/            # CategoryRepository + CRUD use cases
│   ├── accounts/              # AccountRepository + CRUD use cases
│   ├── statistics/            # GetPeriodSummaryUseCase + models
│   ├── import/                # ParseStatement + ImportTransactions use cases
│   ├── auth/                  # AuthRepository + SignIn/SignOut use cases
│   └── exchangerate/          # ExchangeRateRepository + use cases
├── data/                      # Repository implementations + Mappers + DI
│   ├── transactions/
│   ├── categories/
│   ├── accounts/
│   ├── auth/                  # FirebaseAuthRepositoryImpl
│   ├── exchangerate/          # Exchange rate API client
│   └── sync/                  # SyncManager: Room ↔ Firestore
├── presentation/              # Screens, ViewModels, States, Routes
│   ├── transactions/
│   ├── categories/
│   ├── accounts/
│   ├── statistics/
│   ├── import/
│   ├── settings/
│   ├── onboarding/
│   └── auth/                  # SignInScreen, SignInViewModel
├── core/                      # Shared infrastructure
│   ├── model/                 # Domain models (Account, Transaction, Category...)
│   ├── database/              # Room DB, Entities, DAOs
│   ├── datastore/             # Preferences DataStore
│   ├── ui/                    # Theme, shared Compose components
│   ├── common/                # Utils, extensions
│   ├── ai/                    # Gemini service
│   ├── parser/                # PDF parsing, bank detection
│   ├── remoteconfig/          # Firebase Remote Config
│   ├── auth/                  # CredentialManager wrapper
│   └── firestore/             # Firestore SDK wrapper
├── components/                # Design system
│   ├── design-system/
│   ├── ui/
│   └── showcase/
├── build-logic/convention/
└── app/                       # Navigation, DI wiring
```

**Правила зависимостей:**
- `presentation/{name}` → `domain/{name}` → `core:model`
- `data/{name}` → `domain/{name}` + `core:database`
- Presentation **НИКОГДА** не зависит от `core:database`
- Domain/data модули НЕ зависят от presentation

**Пакеты:** `com.atelbay.money_manager.{domain|data|presentation}.{feature}.*`

## Skills (технические гайдлайны)

Все технические правила, паттерны и алгоритмы разбиты на модульные Skills в `.claude/skills/`. Используй Tool Search для поиска нужного скилла.

| Скилл | Описание |
|-------|----------|
| `architecture-and-di.md` | Layer-centric многомодульность, Convention Plugins, Hilt DI, Type-Safe Navigation |
| `clean-architecture-feature-scaffold.md` | Генератор новой фичи: 3 модуля (domain/data/presentation) |
| `compose-ui-guidelines.md` | Compose паттерны, naming, State Hoisting, testTag |
| `room-database.md` | Room entities, DAO, миграции, DataStore, Firestore sync |
| `pdf-and-ai-parsing.md` | Импорт выписок: RegEx → Gemini AI fallback |
| `generate-ui-test.md` | Генерация UI-тестов с ComposeTestRule |
| `unit-testing.md` | Unit-тесты: ViewModel + UseCase с MockK и Turbine |
| `web-search-android-docs.md` | Верификация API через context7 MCP и веб-поиск |
| `gradle-troubleshooting.md` | Диагностика ошибок сборки Gradle |
| `git-conventional-commits.md` | Анализ изменений и коммиты в формате Conventional Commits |
| `firebase-auth.md` | Firebase Auth + CredentialManager: Google Sign-In, Coil 3 |
| `mcp-tools.md` | MCP-инструменты: context7 для документации, Firebase MCP |

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
