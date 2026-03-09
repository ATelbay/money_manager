<!--
Sync Impact Report
===================
Version change: 1.0.0 → 2.0.0
Bump rationale: MAJOR — principles restructured (IV+V merged),
  5 new principles added (Testing, Firebase, Navigation, Import,
  DataStore promoted). Project context section added.
Modified principles:
  - I. Clean Architecture Multi-Module (updated module count, added data:sync)
  - II. Kotlin-First & Jetpack Compose (unchanged)
  - III. Material 3 Design System (unchanged)
  - IV. Animation & Motion (merged old IV + V into one)
  - V. Hilt Dependency Injection (renumbered from VI)
  - VI. Room Database (renumbered from VII, DataStore removed)
Added principles:
  - VII. Testing Architecture
  - VIII. Firebase Ecosystem
  - IX. Type-Safe Navigation
  - X. Statement Import Pipeline
  - XI. Preferences DataStore
Added sections:
  - Project Context (preamble)
Removed principles:
  - Old V (Performance-First Animations) — merged into IV
Removed sections: none
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ generic, no changes needed
  - .specify/templates/spec-template.md ✅ generic, no changes needed
  - .specify/templates/tasks-template.md ✅ generic, no changes needed
Follow-up TODOs: none
-->

# Money Manager Constitution

## Project Context

Money Manager is a personal finance tracker for Android, developed
as part of a master's thesis on **UI test automation**. The primary
goal is to create an app with diverse UI patterns that can be
covered by automated tests (both unit and instrumented).

This context drives several architectural decisions: mandatory
`testTag` on interactive composables, strict state hoisting for
testable ViewModels, and comprehensive test coverage requirements.

**Package:** `com.atelbay.money_manager`

## Core Principles

### I. Clean Architecture Multi-Module

The project follows a layer-centric multi-module architecture
(~32 Gradle modules) with enforced dependency boundaries:

- Modules are organized as `domain/`, `data/`, `presentation/`,
  `core/`, and `app/`.
- `presentation/{feature}` depends on `domain/{feature}` and
  `core:model`. It MUST NEVER depend on `core:database`.
- `data/{feature}` depends on `domain/{feature}` and
  `core:database`.
- `domain/` and `data/` modules MUST NOT depend on
  `presentation/`.
- Every new feature MUST produce three modules:
  `domain:{feature}`, `data:{feature}`, `presentation:{feature}`.
- Package naming:
  `com.atelbay.money_manager.{layer}.{feature}.*`
- Shared infrastructure lives in `core/` modules:
  `core:model`, `core:database`, `core:datastore`, `core:ui`,
  `core:common`, `core:auth`, `core:ai`, `core:parser`,
  `core:remoteconfig`, `core:firestore`.
- Convention plugins in `build-logic/convention/` standardize
  build configuration across all modules.

### II. Kotlin-First & Jetpack Compose

All new code MUST be written in Kotlin. The UI layer MUST use
Jetpack Compose exclusively — no XML layouts, no View-based UI.

- Composable functions follow `PascalCase` naming.
- State hoisting is mandatory: Composables receive state and
  emit events, ViewModels hold state via `StateFlow`.
- Every interactive Composable MUST have a `testTag` modifier
  for UI test targeting.
- Previews (`@Preview`) MUST be provided for reusable
  components in `core:ui`.

### III. Material 3 Design System

The app MUST use Material 3 (Material You) components and
theming via the Compose Material 3 library.

- All colors, typography, and shapes MUST come from
  `MaterialTheme` — no hardcoded values.
- Dynamic color is supported but static theme MUST be the
  fallback.
- New components MUST prefer Material 3 equivalents
  (e.g., `TopAppBar`, `NavigationBar`, `Card`).

### IV. Animation & Motion

All animations MUST follow Material 3 motion guidelines.
Performance is a non-negotiable priority.

**Motion consistency:**
- Shared motion constants (durations, easings, specs) MUST be
  defined in `core:ui` and reused — no per-screen definitions.
- Navigation transitions MUST use consistent enter/exit
  patterns defined centrally in `MoneyManagerNavHost`.
- New animations MUST NOT invent custom easing curves unless
  Material 3 tokens are provably insufficient (document why).

**Performance rules:**
- `AnimationSpec`, `Easing`, and transition objects MUST be
  created with `remember` or declared as top-level constants —
  never allocated inside recomposing scopes.
- Animated values MUST use `Animatable`, `animateXAsState`,
  or `Transition` APIs — never drive animations through mutable
  state that triggers recomposition.
- Layout-phase animations (`Modifier.graphicsLayer`,
  `Modifier.offset`) are preferred over Composition-phase
  changes for transform animations (translation, scale, alpha).

### V. Hilt Dependency Injection

All dependency injection MUST use Hilt. No manual service
locators or custom DI containers.

- ViewModels MUST be annotated with `@HiltViewModel`.
- Repository bindings MUST be declared in `@Module`/`@Binds`
  inside the corresponding `data/{feature}` module.
- `@Singleton` scope is reserved for truly global instances
  (database, DataStore, API clients).
- Use cases MUST be constructor-injected, not field-injected.

### VI. Room Database

All structured local data MUST be persisted via Room.

- Entity classes live in `core:database`.
- DAOs MUST return `Flow<T>` for observable queries.
- Schema migrations MUST be explicit
  (no `fallbackToDestructiveMigration` in production builds).
- Mappers between Room entities and domain models MUST live
  in the corresponding `data/{feature}` module.
- `exportSchema = true` is mandatory for migration validation.

### VII. Testing Architecture

Testing is critical — this project is the foundation for a
master's thesis on UI test automation.

**Unit tests** (`src/test/`, JVM):
- Every ViewModel MUST have at least 3 tests: initial state,
  success path, error path.
- Every UseCase MUST have happy-path and error-case tests.
- Mappers MUST have forward and inverse transformation tests.
- Stack: JUnit 4, MockK, Turbine, kotlinx-coroutines-test.
- `MainDispatcherRule` is mandatory in all ViewModel tests.
- Use `runTest {}` (not `runBlocking`), Turbine for Flow
  assertions.

**UI tests** (`src/androidTest/`, instrumented):
- Compose UI tests use `ComposeTestRule` with `testTag`-driven
  node selection.
- Every screen MUST have at least basic render and interaction
  tests.
- UI tests live in `presentation/{feature}/src/androidTest/`.

**Anti-patterns:**
- No `Thread.sleep()` — use Turbine + TestDispatcher.
- No real Room databases in unit tests — use in-memory DB
  only in `androidTest`.

### VIII. Firebase Ecosystem

Firebase services are used for optional features. The app MUST
work fully without authentication or network.

**Firebase Auth + CredentialManager:**
- Optional Google Sign-In via CredentialManager API
  (not deprecated `GoogleSignInClient`).
- 4-module structure: `core:auth`, `domain:auth`, `data:auth`,
  `presentation:auth`.
- Web Client ID resolved at runtime via
  `getIdentifier("default_web_client_id", ...)` — never
  hardcoded.
- BOM 34+: no `ktx` suffixes — use `com.google.firebase.Firebase`
  and `com.google.firebase.auth.auth` directly.

**Firestore sync:**
- `data:sync` contains `SyncManager` for Room ↔ Firestore
  bidirectional sync.
- `core:firestore` wraps Firestore SDK.
- Sync is optional — offline-first with Room as source of truth.

**Firebase Remote Config:**
- `core:remoteconfig` provides `ParserConfigProvider` for
  bank parser regex patterns.
- Used for feature flags and parser configuration.

**Firebase AI (Gemini 2.5 Flash):**
- `core:ai` contains `GeminiService` for AI-assisted parsing.
- Used as fallback when RegEx parsing fails.

### IX. Type-Safe Navigation

Navigation MUST use Navigation Compose with type-safe routes.

- Routes are `@Serializable` data classes or objects.
- All destinations are registered in the centralized
  `MoneyManagerNavHost` in the `app` module.
- `SharedTransitionLayout` wraps all destinations; composition
  locals (`LocalSharedTransitionScope`,
  `LocalAnimatedVisibilityScope`) provide shared element support.
- Deep link support where applicable.

### X. Statement Import Pipeline

Bank statement import follows a two-level parsing strategy:

- **Level 1 (RegEx):** `PdfTextExtractor` (PdfBox-Android) →
  `BankDetector` (marker-based) → `RegexStatementParser`
  (config-driven). Free, fast, offline.
- **Level 2 (AI fallback):** When RegEx fails or bank is
  unknown, fall back to Gemini AI via `GeminiService`.
  Images go directly to AI.
- Parser configs are managed via Firebase Remote Config with
  local defaults in `ParserConfigProvider`.
- Supported banks: Kaspi, Freedom, Forte, Bereke, Eurasian.
- Adding a new bank: add markers to `BankDetector`, add regex
  to Remote Config defaults, add unit tests.
- Deduplication by `uniqueHash` — same transaction MUST NOT
  import twice.

### XI. Preferences DataStore

Small key-value preferences MUST use Preferences DataStore
(not Room).

- DataStore instance lives in `core:datastore` as a Hilt
  `@Singleton`.
- All preferences MUST be observable via `Flow`.
- Known keys: `onboarding_completed`, `selected_account_id`,
  `theme_mode`.
- Anti-pattern: MUST NOT use DataStore for complex nested or
  relational data — use Room instead.

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.3.0 |
| UI | Jetpack Compose + Material 3 | BOM 2026.01.01 |
| DI | Hilt | 2.58 |
| Database | Room | 2.8.4 |
| Navigation | Navigation Compose (type-safe) | 2.9.7 |
| Async | Coroutines + Flow | 1.10.2 |
| Build | Version Catalogs + Convention Plugins | AGP 8.13.2, KSP 2.3.1 |
| Charts | Vico | 2.4.3 |
| Images | Coil 3 (`io.coil-kt.coil3`) | 3.3.0 |
| PDF | PdfBox-Android | 2.0.27.0 |
| AI | Firebase AI (Gemini 2.5 Flash) | — |
| Auth | Firebase Auth + CredentialManager | credentials 1.3.0, googleid 1.1.1 |
| Remote Config | Firebase Remote Config | — |
| Firestore | Firebase Firestore | — |
| DataStore | Preferences DataStore | 1.1.7 |
| Testing | JUnit 4, MockK 1.14.9, Turbine 1.2.1 | — |
| Lint | Detekt + detekt-compose | 1.23.8 / 0.5.6 |
| CI/CD | GitHub Actions → Firebase App Distribution | — |

New dependencies MUST be added through Version Catalogs
(`libs.versions.toml`). Convention plugins in `build-logic/`
MUST be used for shared build configuration. KSP is used
instead of kapt.

## Development Workflow

- **Architecture validation**: Every PR MUST respect module
  dependency rules (Principle I).
- **UI review**: New screens MUST include `@Preview` composables
  and `testTag` assignments before merge.
- **Animation review**: Animation PRs MUST demonstrate no
  unnecessary recomposition (use Layout Inspector or
  recomposition counter in debug builds).
- **Commit style**: Conventional Commits format
  (`feat:`, `fix:`, `refactor:`, etc.).
- **CI pipeline**: `./gradlew assembleDebug test lint detekt`
  MUST pass before merge.

## Governance

This constitution is the authoritative source of architectural
and quality rules for the Money Manager project. All code
reviews and PRs MUST verify compliance with these principles.

- **Amendments**: Any principle change MUST be documented with
  rationale and include a migration plan for existing code if
  applicable.
- **Versioning**: The constitution follows SemVer —
  MAJOR for principle removals/redefinitions, MINOR for
  additions, PATCH for clarifications.
- **Compliance**: Use the plan template's "Constitution Check"
  gate to verify feature designs against these principles
  before implementation begins.
- **Runtime guidance**: See `CLAUDE.md` for project overview
  and `.claude/skills/` for detailed implementation patterns.

**Version**: 2.0.0 | **Ratified**: 2026-03-08 | **Last Amended**: 2026-03-08
