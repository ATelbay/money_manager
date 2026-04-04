# Money Manager — Personal Finance Tracker

## Project Overview

Money Manager is an Android app for personal finance tracking. Developed as part of a master's thesis focused on UI automation testing.

**Goal:** build an app with diverse UI patterns for subsequent coverage with automated tests.

**Package:** `com.atelbay.money_manager`

## Tech Stack

| Component | Technology | Version |
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

## Architecture (Layer-Centric Modules)

41 Gradle modules with enforced layer boundaries:

```
MoneyManager/
├── domain/                    # Repository interfaces + Use Cases
│   ├── transactions/          # TransactionRepository + CRUD use cases
│   ├── categories/            # CategoryRepository + CRUD use cases
│   ├── accounts/              # AccountRepository + CRUD use cases
│   ├── statistics/            # GetPeriodSummaryUseCase + models
│   ├── import/                # ParseStatement + ImportTransactions use cases
│   ├── auth/                  # AuthRepository + SignIn/SignOut use cases
│   ├── exchangerate/          # ExchangeRateRepository + use cases
│   ├── sync/                  # SyncUseCase
│   ├── recurring/             # RecurringTransactionRepository + use cases
│   └── budgets/               # BudgetRepository + use cases
├── data/                      # Repository implementations + Mappers + DI
│   ├── transactions/
│   ├── categories/
│   ├── accounts/
│   ├── auth/                  # FirebaseAuthRepositoryImpl
│   ├── exchangerate/          # Exchange rate API client
│   ├── sync/                  # SyncManager: Room ↔ Firestore
│   ├── recurring/
│   └── budgets/
├── presentation/              # Screens, ViewModels, States, Routes
│   ├── transactions/
│   ├── categories/
│   ├── accounts/
│   ├── statistics/
│   ├── import/
│   ├── settings/
│   ├── onboarding/
│   ├── auth/                  # SignInScreen, SignInViewModel
│   ├── recurring/
│   └── budgets/
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
│   ├── firestore/             # Firestore SDK wrapper
│   └── crypto/                # Encryption utilities (Tink)
├── build-logic/convention/
└── app/                       # Navigation, DI wiring
```

**Dependency Rules:**
- `presentation/{name}` → `domain/{name}` → `core:model`
- `data/{name}` → `domain/{name}` + `core:database`
- Presentation **NEVER** depends on `core:database`
- Domain/data modules do NOT depend on presentation

**Packages:** `com.atelbay.money_manager.{domain|data|presentation}.{feature}.*`

## Skills (technical guidelines)

All technical rules, patterns, and algorithms are split into modular Skills in `.claude/skills/`. Use Tool Search to find the relevant skill.

| Skill | Description |
|-------|----------|
| `architecture-and-di.md` | Layer-centric multi-module architecture, Convention Plugins, Hilt DI, Type-Safe Navigation |
| `clean-architecture-feature-scaffold.md` | New feature generator: 3 modules (domain/data/presentation) |
| `compose-ui-guidelines.md` | Compose patterns, naming, State Hoisting, testTag |
| `room-database.md` | Room entities, DAOs, migrations, DataStore, Firestore sync |
| `pdf-and-ai-parsing.md` | Bank statement import: RegEx → Gemini AI fallback |
| `generate-ui-test.md` | UI test generation with ComposeTestRule |
| `unit-testing.md` | Unit tests: ViewModel + UseCase with MockK and Turbine |
| `web-search-android-docs.md` | API verification via context7 MCP and web search |
| `gradle-troubleshooting.md` | Gradle build error diagnostics |
| `git-conventional-commits.md` | Change analysis and Conventional Commits format |
| `firebase-auth.md` | Firebase Auth + CredentialManager: Google Sign-In, Coil 3 |
| `mcp-tools.md` | MCP tools: context7 for docs, Firebase MCP |
| `pencil-design.md` | Pencil MCP: reading, creating, and referencing UI designs in .pen files |
| `stitch-design.md` | Stitch MCP: AI screen generation, design systems, variants |

## Useful Commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
./gradlew detekt
```

## Design-to-Code Workflow (Pencil + Speckit)

When a UI feature has a `.pen` design file, ALWAYS include a **Design Reference** section in `spec.md`:
- `.pen` file path (e.g. `money_manager_screens.pen`)
- Node IDs for light + dark variants (e.g. `rktgn`, `rGZ2b`)
- Instruction: use pencil MCP `get_screenshot(nodeId)` and `batch_get(nodeIds)` during implementation to verify visually

Propagate key node IDs into individual tasks in `tasks.md` so implementing agents can screenshot and compare the specific component they're building. This is critical because context is cleared between speckit steps.

Design file location: `~/Documents/pencil/money_manager_ds/money_manager_screens.pen`

## TODO / Not in MVP

- [ ] PIN / biometrics
- [ ] Multi-currency with conversion
- [ ] Cloud sync
- [ ] Widgets

