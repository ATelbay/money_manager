# Agent Instructions for Money Manager

## Purpose

This file is the compact, always-on contract for coding agents in this repo. Keep it under ~100 lines. Detailed rules, patterns and algorithms live in per-topic Skills under `.agents/skills/<name>/SKILL.md` — load the relevant one via Tool Search / the Skill tool.

Agent assets are stored agent-agnostically under `.agents/{skills,commands,agents}`. The `.claude/` dir is gitignored and only holds symlinks into `.agents/` — **after cloning, run `./scripts/setup-agents.sh` once** to recreate them.

## Project overview

- Android personal-finance app. Kotlin, Gradle (Version Catalogs + Convention Plugins), multi-module.
- Package: `com.atelbay.money_manager`.
- UI: Jetpack Compose + **Material 3**; DI: Hilt; DB: Room; Async: Coroutines + Flow; DataStore (Preferences).
- Architecture: MVVM + Clean Architecture, **layer-centric** modules (`domain/`, `data/`, `presentation/`, `core/`, `app/`).
- AI/parsing: Firebase AI (Gemini) + PdfBox-Android for bank-statement import. Firebase Remote Config, Firestore sync, Firebase Auth + CredentialManager.
- Developed for a master's thesis on UI automation testing — the app deliberately has diverse UI patterns to cover with automated tests.

## Critical rules

1. **Layer boundaries** (enforced): `presentation/{f}` → `domain/{f}` → `core:model`; `data/{f}` → `domain/{f}` + `core:database`.
2. **Presentation NEVER depends on `core:database`.** Domain/data modules never depend on presentation.
3. Packages follow `com.atelbay.money_manager.{domain|data|presentation}.{feature}.*`.
4. Repository **interfaces + use cases** live in `domain/`; repository **impls + mappers + Hilt bindings** live in `data/`.
5. Domain models live in `core:model`; Room entities/DAOs live in `core:database` — keep them separate, map between them in `data/`.
6. Type-safe Navigation Compose only (typed routes/destinations); pass primitive args, each screen reloads its own data.
7. New dependencies go through `gradle/libs.versions.toml` (version catalog) first; wire builds via Convention Plugins in `build-logic/`.
8. No hardcoded user-facing strings, colors, or dp — use string resources and the Material 3 theme/tokens from `core:ui`.
9. Never commit secrets, keystores, `google-services.json` secrets, or `local.properties`.
10. Don't suppress module-boundary/lint failures to “make it build” — fix the architecture.

## Compose defaults

- Material 3 components + the app theme from `core:ui` (Theme, shared components). No raw hex colors / hardcoded dp.
- Hoist state; pass navigation as lambdas, not `NavController`. ViewModels expose immutable UI `State`/`StateFlow`.
- Add stable `testTag`s on interactive/asserted nodes — UI automation testability is a first-class goal of this project.
- Screen composables stateless where possible; `hiltViewModel()` only at the route/entry, not deep in the tree.

## Visibility and module boundaries

- Public from a feature: the screen/route entry points that `app/` wires into navigation.
- Data repositories expose a public interface (`domain/`) + implementation bound via Hilt (`data/`).
- `domain:statistics` and `domain:import` use DAOs directly (not through repositories) — intentional, don't "fix".
- Prefer `implementation` over `api` in Gradle dependencies.

## Task workflow

- Before non-trivial edits, inspect the relevant feature triple (`domain` + `data` + `presentation`) and a similar existing feature.
- For a new feature, use `/clean-architecture-feature-scaffold` (generates the 3 modules).
- Ask before high-risk architecture, security, or cross-module decisions instead of guessing.
- When a UI feature has a `.pen` design, include the Design Reference (`.pen` path + node IDs) and verify visually via the pencil MCP — see `/pencil-design`.

## Common commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
./gradlew detekt
```

## Skills routing

| Task | Use skill |
|---|---|
| Layer-centric modules, Convention Plugins, Hilt DI, type-safe navigation | `/architecture-and-di` |
| New feature: domain + data + presentation modules | `/clean-architecture-feature-scaffold` |
| Compose patterns, naming, state hoisting, `testTag` | `/compose-ui-guidelines` |
| Room entities/DAOs/migrations, DataStore, Firestore sync | `/room-database` |
| Bank-statement import: RegEx → Gemini AI fallback | `/pdf-and-ai-parsing` |
| UI tests with ComposeTestRule | `/generate-ui-test` |
| Unit tests: ViewModel + UseCase (MockK + Turbine) | `/unit-testing` |
| Firebase Auth + CredentialManager (Google Sign-In, Coil 3) | `/firebase-auth` |
| Verify Android/Kotlin API (context7 MCP + web) | `/web-search-android-docs` |
| Gradle build error diagnostics | `/gradle-troubleshooting` |
| Change analysis + Conventional Commits | `/git-conventional-commits` |
| MCP tools (context7, Firebase MCP) | `/mcp-tools` |
| Reading/creating/referencing `.pen` UI designs | `/pencil-design` |
| Stitch MCP: AI screen generation, design systems | `/stitch-design` |

## When in doubt

Find a similar existing feature first, load the relevant skill, prefer the smallest safe change. If requirements are ambiguous, ask instead of guessing.
