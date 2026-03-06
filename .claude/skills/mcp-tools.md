---
description: "MCP-инструменты в Money Manager: context7 для поиска документации, Firebase MCP для операций с проектом, Playwright — не применимо для Android"
---

# MCP Tools

## Context

В проекте доступны несколько MCP-серверов. Используй нужный инструмент для нужной задачи — это быстрее и точнее чем универсальные подходы.

## context7 — Документация библиотек (предпочтительный метод)

**Когда использовать:** поиск API-документации, примеров кода для любой библиотеки в проекте. Быстрее и точнее чем WebSearch для библиотечных docs.

**Два шага:**

### Шаг 1: Найти libraryId
```
mcp__context7__resolve-library-id
  query: "jetpack compose navigation"
  → возвращает libraryId, например "/androidx/navigation"
```

### Шаг 2: Запросить документацию
```
mcp__context7__query-docs
  libraryId: "/androidx/navigation"
  query: "type-safe destinations composable"
  → возвращает актуальную документацию с примерами кода
```

**Примеры запросов для этого проекта:**

| Задача | resolve query | docs query |
|--------|--------------|------------|
| Type-safe Navigation | `"navigation-compose"` | `"type-safe composable destinations"` |
| Room новые аннотации | `"androidx room"` | `"@Upsert @MapColumn"` |
| Hilt + Compose | `"hilt android"` | `"hiltViewModel ViewModelComponent"` |
| Vico 2.x charts | `"vico charts compose"` | `"CartesianChartHost rememberCartesianChartModel"` |
| Turbine Flow testing | `"turbine"` | `"test awaitItem awaitComplete"` |
| Coil 3 AsyncImage | `"coil"` | `"AsyncImage ImageRequest compose"` |
| Material 3 компоненты | `"compose material3"` | `"ExposedDropdownMenuBox SegmentedButton"` |
| Firebase AI Gemini | `"firebase-ai-logic"` | `"generateContent GenerativeModel"` |
| CredentialManager | `"androidx credentials"` | `"GetCredentialRequest GetGoogleIdOption"` |

**context7 vs WebSearch:**
- context7: API docs, сигнатуры методов, официальные примеры
- WebSearch: Stack Overflow, GitHub Issues, блог-посты, changelog

## Firebase MCP — Операции с Firebase проектом

**Когда использовать:** настройка Firebase проекта, добавление SHA, проверка конфигурации, чтение security rules.

### Доступные инструменты

| Tool | Назначение |
|------|-----------|
| `mcp__plugin_firebase_firebase__firebase_list_projects` | Список Firebase проектов |
| `mcp__plugin_firebase_firebase__firebase_get_project` | Детали проекта |
| `mcp__plugin_firebase_firebase__firebase_list_apps` | Список приложений в проекте |
| `mcp__plugin_firebase_firebase__firebase_get_sdk_config` | Получить google-services.json конфиг |
| `mcp__plugin_firebase_firebase__firebase_get_security_rules` | Прочитать правила Firestore/Storage |
| `mcp__plugin_firebase_firebase__firebase_create_android_sha` | Добавить SHA fingerprint |
| `mcp__plugin_firebase_firebase__firebase_get_environment` | Информация о Firebase окружении |
| `mcp__plugin_firebase_firebase__firebase_read_resources` | Прочитать ресурсы проекта |

### Пример: добавление debug SHA fingerprint

```bash
# Шаг 1: Получить SHA из Gradle
./gradlew signingReport
# Найди SHA-1 и SHA-256 для debug variant

# Шаг 2: Добавить через Firebase MCP
mcp__plugin_firebase_firebase__firebase_create_android_sha
  appId: "your-app-id"
  shaHash: "SHA-1 или SHA-256 из signingReport"
  hashType: "SHA_1"  # или "SHA_256"
```

### Пример: проверка SDK конфигурации

```
mcp__plugin_firebase_firebase__firebase_get_sdk_config
  appId: "android-app-id"
  → возвращает содержимое google-services.json
```

## Playwright MCP — НЕ применимо для Android

**Playwright инструменты предназначены для веб-браузеров. НЕ используй для Android.**

| Нужна задача | Правильный инструмент |
|-------------|----------------------|
| Запустить UI-тесты | `./gradlew connectedAndroidTest` |
| Проверить верстку | Android Studio Layout Inspector или `adb shell screencap` |
| Взаимодействие с эмулятором | `adb shell input tap X Y` или `./gradlew installDebug` |
| Screenshot | `adb exec-out screencap -p > screen.png` |

## IDE MCP — Диагностика

```
mcp__ide__getDiagnostics
  → возвращает ошибки/предупреждения компилятора без полного Gradle build
```

Полезно для быстрой проверки типов перед запуском `./gradlew assembleDebug`.

## Быстрая шпаргалка

| Задача | Инструмент |
|--------|-----------|
| API документация библиотеки | context7: resolve → query |
| Пример кода для Compose API | context7 → resolve "jetpack compose" |
| Добавить SHA fingerprint | `firebase_create_android_sha` |
| Проверить Firebase конфиг | `firebase_get_sdk_config` |
| Прочитать security rules | `firebase_get_security_rules` |
| Ошибки компилятора | `mcp__ide__getDiagnostics` |
| Android UI тестирование | `./gradlew connectedAndroidTest` |
| Stack Overflow / GitHub Issues | WebSearch |

## Anti-patterns

- НЕ используй Playwright для Android — эти инструменты для веб-браузера
- НЕ пропускай `resolve-library-id` перед `query-docs` — без libraryId запрос не работает
- НЕ ищи Android документацию через WebSearch если context7 справится быстрее
- НЕ используй Firebase MCP для изменения production security rules без review
