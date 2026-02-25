---
description: "Верификация Android API через поиск документации: Jetpack Compose, Navigation 2.9+, Hilt, Material 3, Room — предотвращение галлюцинаций устаревшими методами"
---

# Web Search Android Docs

## Context

Стек проекта быстро обновляется (Compose BOM 2026.01.01, Navigation 2.9.7, Kotlin 2.3.0). Агент должен верифицировать API через веб-поиск перед использованием, чтобы не предлагать устаревший или несуществующий код.

## Когда использовать

**ОБЯЗАТЕЛЬНО** проверяй через WebSearch/WebFetch в следующих случаях:

| Триггер | Пример |
|---------|--------|
| Navigation Compose API | `composable<T>`, `toRoute<T>`, `NavType` для custom types |
| Compose API, в котором не уверен | Новые модификаторы, `AnimatedContent`, `Modifier.animateItem()` |
| Material 3 компоненты | `ExposedDropdownMenuBox`, `SegmentedButton`, `DatePickerDialog` |
| Hilt + Compose интеграция | `hiltViewModel()`, `@HiltViewModel`, navigation-hilt |
| Room новые фичи | `@Upsert`, `@MapColumn`, auto-migrations |
| Kotlin 2.x специфика | context receivers, explicit backing fields, KSP 2 |
| Vico charts API | Версия 2.x имеет значительные breaking changes vs 1.x |
| Firebase AI SDK | API может отличаться между версиями |

## Process

### Шаг 1: Определи что нужно проверить
Сформулируй конкретный вопрос: какой API, какой класс/метод, какая версия.

### Шаг 2: Поиск
Используй `WebSearch` с запросом, включающим:
- Название API + "android" или "jetpack"
- Версию библиотеки из `libs.versions.toml`
- Текущий год (2026) для актуальности

Примеры запросов:
```
"Jetpack Compose Navigation type-safe 2.9 site:developer.android.com"
"Material 3 ExposedDropdownMenuBox Compose 2026"
"Room @Upsert annotation android"
"Vico chart 2.x compose migration"
```

### Шаг 3: Чтение документации
Используй `WebFetch` для чтения:
- `https://developer.android.com/reference/...` — справочник API
- `https://developer.android.com/develop/ui/compose/...` — гайды Compose
- `https://developer.android.com/training/data-storage/room/...` — гайды Room
- `https://kotlinlang.org/docs/...` — Kotlin docs
- `https://dagger.dev/hilt/...` — Hilt docs

### Шаг 4: Применение
- Если найденный API отличается от того что ты собирался написать — используй документированную версию
- Если API deprecated — найди замену в тех же docs
- Если не нашёл — предупреди пользователя о неуверенности

## Версии проекта (из libs.versions.toml)

| Библиотека | Версия |
|------------|--------|
| Compose BOM | 2026.01.01 |
| Navigation | 2.9.7 |
| Hilt | 2.58 |
| Room | 2.8.4 |
| Kotlin | 2.3.0 |
| KSP | 2.3.1 |
| AGP | 8.13.2 |
| Coroutines | 1.10.2 |
| Vico | 2.4.3 |

## Quality Bar

- Не предлагай код с API, в котором не уверен, без предварительной проверки
- Указывай источник (ссылку на документацию) когда предлагаешь неочевидный API
- Если документация противоречит — приоритет у official Android reference

## Anti-patterns

- НЕ полагайся на память для API, которые часто меняются (Compose, Navigation, Material 3)
- НЕ используй примеры из Stack Overflow без проверки версии
- НЕ предлагай `kapt` для Hilt — проект на KSP
- НЕ используй deprecated navigation API (`composable(route = "...")`) — проект на type-safe navigation
- НЕ смешивай Material 2 и Material 3 API (например `TopAppBar` из разных пакетов)
