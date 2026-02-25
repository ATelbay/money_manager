---
description: "Траблшутинг сборки Gradle в Money Manager: диагностика ошибок assembleDebug, test, Version Catalogs, Hilt/KSP, Navigation Compose"
---

# Gradle Troubleshooting

## Context

Проект использует Convention Plugins (`build-logic/`), Version Catalogs (`gradle/libs.versions.toml`), KSP (не kapt), Hilt и Type-Safe Navigation. Ошибки сборки чаще всего связаны с несовместимостью версий или забытыми плагинами.

**Ключевые файлы:**
- `gradle/libs.versions.toml` — версии всех зависимостей
- `build-logic/convention/src/main/kotlin/` — convention plugins
- `feature/*/build.gradle.kts` — конфиги feature-модулей
- `core/*/build.gradle.kts` — конфиги core-модулей

## Process

1. Внимательно прочитай stacktrace ошибки. Если причина неясна, перезапусти упавшую команду с флагом `--info` или `--stacktrace` (например, `./gradlew assembleDebug --stacktrace`).
2. Если ошибка связана с **Version Catalogs** или зависимостями: проверь файл `gradle/libs.versions.toml`. Убедись, что версии библиотек совместимы (например, Compose Compiler и версия Kotlin).
3. Если ошибка связана с **Hilt / KSP**: проверь плагины в модуле `build-logic/convention`. Возможно, забыт плагин `moneymanager.android.hilt` в `build.gradle.kts` фича-модуля.
4. Если ошибка связана с **Navigation Compose (Type-safe)**: убедись, что классы маршрутов помечены аннотацией `@Serializable` из `kotlinx.serialization`.
5. Если ошибка тебе неизвестна, используй инструмент `WebSearch` для поиска текста ошибки на StackOverflow или в GitHub Issues.

## Anti-patterns

- НЕ предлагай даунгрейдить версию Kotlin или AGP без крайней необходимости.
- НЕ предлагай удалять плагины из `build-logic`, если они ломают сборку — ищи первопричину в конфигурации.
- НЕ запускай `./gradlew clean` как первый шаг — сначала разберись в ошибке.
- НЕ используй `kapt` — проект на KSP.
