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

## Skills (технические гайдлайны)

Все технические правила, паттерны и алгоритмы разбиты на модульные Skills в `.claude/skills/`. Используй Tool Search для поиска нужного скилла.

| Скилл | Описание |
|-------|----------|
| `architecture-and-di.md` | Многомодульность, Convention Plugins, Hilt DI, Type-Safe Navigation |
| `compose-ui-guidelines.md` | Compose паттерны, naming, State Hoisting, testTag |
| `room-database.md` | Room entities, DAO, миграции, DataStore |
| `pdf-and-ai-parsing.md` | Импорт выписок: RegEx → Gemini AI fallback |
| `generate-ui-test.md` | Генерация UI-тестов с ComposeTestRule |
| `web-search-android-docs.md` | Верификация API через поиск документации |
| `clean-architecture-feature-scaffold.md` | Генератор новой фичи (domain/data/ui/di) |

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
