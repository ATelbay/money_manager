---
name: gradle-troubleshooting
description: "Covers Gradle build error diagnosis for the Money Manager multi-module project: Convention Plugins, Version Catalogs, KSP/Hilt wiring, Type-Safe Navigation serialization, and assembleDebug/test failures. Use when: a Gradle build fails, a module is missing a plugin, dependencies are incompatible, or KSP/Hilt annotation processing errors appear."
user-invocable: true
---

# Gradle Troubleshooting

## Context

The project uses Convention Plugins (`build-logic/`), Version Catalogs (`gradle/libs.versions.toml`), KSP (not kapt), Hilt, and Type-Safe Navigation. Build errors are most commonly caused by version incompatibilities or missing plugins.

**Key files:**
- `gradle/libs.versions.toml` — versions of all dependencies
- `build-logic/convention/src/main/kotlin/` — convention plugins
- `domain/*/build.gradle.kts` — domain module configs
- `data/*/build.gradle.kts` — data module configs
- `presentation/*/build.gradle.kts` — presentation module configs
- `core/*/build.gradle.kts` — core module configs

## Process

1. Read the error stacktrace carefully. If the cause is unclear, re-run the failed command with the `--info` or `--stacktrace` flag (e.g., `./gradlew assembleDebug --stacktrace`).
2. If the error is related to **Version Catalogs** or dependencies: check the `gradle/libs.versions.toml` file. Make sure library versions are compatible (e.g., Compose Compiler and Kotlin version).
3. If the error is related to **Hilt / KSP**: check the plugins in the `build-logic/convention` module. The `moneymanager.android.hilt` plugin may be missing from the feature module's `build.gradle.kts`.
4. If the error is related to **Navigation Compose (Type-safe)**: make sure route classes are annotated with `@Serializable` from `kotlinx.serialization`.
5. If the error is unfamiliar, use the `WebSearch` tool to search for the error text on StackOverflow or GitHub Issues.

## Anti-patterns

- Do NOT suggest downgrading Kotlin or AGP unless absolutely necessary.
- Do NOT suggest removing plugins from `build-logic` when they break the build — find the root cause in the configuration instead.
- Do NOT run `./gradlew clean` as the first step — understand the error first.
- Do NOT use `kapt` — the project uses KSP.
