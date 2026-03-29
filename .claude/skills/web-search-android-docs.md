---
description: "Verifying Android APIs via documentation search: Jetpack Compose, Navigation 2.9+, Hilt, Material 3, Room — preventing hallucinations with outdated methods"
---

# Web Search Android Docs

## Context

The project stack evolves quickly (Compose BOM 2026.01.01, Navigation 2.9.7, Kotlin 2.3.0). Always verify APIs via web search before using them to avoid suggesting outdated or non-existent code.

## When to Use

**ALWAYS** verify via WebSearch/WebFetch in the following cases:

| Trigger | Example |
|---------|---------|
| Navigation Compose API | `composable<T>`, `toRoute<T>`, `NavType` for custom types |
| Compose API you're unsure about | New modifiers, `AnimatedContent`, `Modifier.animateItem()` |
| Material 3 components | `ExposedDropdownMenuBox`, `SegmentedButton`, `DatePickerDialog` |
| Hilt + Compose integration | `hiltViewModel()`, `@HiltViewModel`, navigation-hilt |
| Room new features | `@Upsert`, `@MapColumn`, auto-migrations |
| Kotlin 2.x specifics | context receivers, explicit backing fields, KSP 2 |
| Vico charts API | Version 2.x has significant breaking changes vs 1.x |
| Firebase AI SDK | API may differ between versions |
| Firebase Auth + CredentialManager | `BeginSignInRequest`, `GetGoogleIdOption`, `CredentialManager.getCredential()` |
| Coil 3 | `AsyncImage`, `ImageRequest` — group changed to `io.coil-kt.coil3` |
| Turbine (Flow testing) | `app.cash.turbine.test {}`, `awaitItem()`, `awaitComplete()` |

## Process

### context7 MCP (preferred method)

For library documentation lookups, use the context7 MCP — faster and more precise than WebSearch:

1. `mcp__context7__resolve-library-id` — find the library ID by name
2. `mcp__context7__query-docs` — query documentation by libraryId

Examples:
```
resolve("navigation-compose") → query("type-safe destinations composable")
resolve("androidx room")      → query("@Upsert @MapColumn")
resolve("hilt android")       → query("hiltViewModel ViewModelComponent")
resolve("vico charts")        → query("CartesianChartHost rememberCartesianChartModel")
resolve("turbine")            → query("test awaitItem awaitComplete")
```

Use WebSearch/WebFetch only if context7 did not find the needed information.

### Step 1: Identify what to verify
Formulate a specific question: which API, which class/method, which version.

### Step 2: Search
Use `WebSearch` with a query that includes:
- API name + "android" or "jetpack"
- Library version from `libs.versions.toml`
- Current year (2026) for relevance

Example queries:
```
"Jetpack Compose Navigation type-safe 2.9 site:developer.android.com"
"Material 3 ExposedDropdownMenuBox Compose 2026"
"Room @Upsert annotation android"
"Vico chart 2.x compose migration"
```

### Step 3: Read the documentation
Use `WebFetch` to read:
- `https://developer.android.com/reference/...` — API reference
- `https://developer.android.com/develop/ui/compose/...` — Compose guides
- `https://developer.android.com/training/data-storage/room/...` — Room guides
- `https://kotlinlang.org/docs/...` — Kotlin docs
- `https://dagger.dev/hilt/...` — Hilt docs

### Step 4: Apply
- If the found API differs from what you were about to write — use the documented version
- If the API is deprecated — find the replacement in the same docs
- If nothing was found — warn the user about the uncertainty

## Project Versions (from libs.versions.toml)

| Library | Version |
|---------|---------|
| Compose BOM | 2026.01.01 |
| Navigation | 2.9.7 |
| Hilt | 2.58 |
| Room | 2.8.4 |
| Kotlin | 2.3.0 |
| KSP | 2.3.1 |
| AGP | 8.13.2 |
| Coroutines | 1.10.2 |
| Vico | 2.4.3 |
| Firebase BOM | 34.8.0 |
| Coil 3 | 3.3.0 |
| Turbine | 1.2.1 |
| MockK | 1.14.9 |
| Credentials | 1.3.0 |

## Quality Bar

- Do not suggest code with APIs you are unsure about without verifying first
- Cite the source (link to documentation) when proposing a non-obvious API
- If documentation conflicts — the official Android reference takes priority

## Anti-patterns

- Do NOT rely on memory for APIs that change frequently (Compose, Navigation, Material 3)
- Do NOT use Stack Overflow examples without verifying the version
- Do NOT suggest `kapt` for Hilt — the project uses KSP
- Do NOT use the deprecated navigation API (`composable(route = "...")`) — the project uses type-safe navigation
- Do NOT mix Material 2 and Material 3 APIs (e.g. `TopAppBar` from different packages)
