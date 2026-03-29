# Money Manager

A personal finance tracker for Android. Built as part of a master's thesis on UI test automation, it covers a wide range of real-world UI patterns designed to be exercised by automated test suites.

---

## Screenshots

<!-- TODO: add demo video/GIF of overall app flow -->

| Transactions | Statistics | Import |
|---|---|---|
| <!-- TODO: add screenshot of Transactions (Home) screen --> | <!-- TODO: add screenshot of Statistics screen --> | <!-- TODO: add screenshot of Import screen --> |

| Budgets | Recurring | Accounts |
|---|---|---|
| <!-- TODO: add screenshot of Budgets screen --> | <!-- TODO: add screenshot of Recurring Transactions screen --> | <!-- TODO: add screenshot of Accounts screen --> |

| Onboarding | Settings | Sign-In |
|---|---|---|
| <!-- TODO: add screenshot of Onboarding screen --> | <!-- TODO: add screenshot of Settings screen --> | <!-- TODO: add screenshot of Sign-In screen --> |

---

## Features

### Core

- **Transactions** — create, edit, delete, search, and filter income and expense transactions
- **Accounts** — manage multiple accounts with per-account balance tracking and aggregate totals
- **Categories** — separate category lists for income and expenses, each with a custom icon and color
- **Statistics** — bar and pie charts (Vico) with period selection (week, month, quarter, year, custom); drill-down into per-category transaction lists

### Import

- **PDF bank statement import** — open a PDF directly from the Files app or share it to Money Manager
- **Supported banks** — Kaspi Bank, Freedom Bank, Forte Bank, Bereke Bank, Eurasian Bank
- **AI fallback** — when a regex pattern does not match, Gemini 2.5 Flash is invoked to extract transactions; the resulting config is submitted as a candidate for future Remote Config promotion
- **Auto-update** — parser configs are served via Firebase Remote Config and cached locally in DataStore; a Firebase Cloud Function evaluates new candidates automatically

### Budgets and Recurring

- **Budgets** — set monthly spending limits per category and track progress
- **Recurring transactions** — define repeating income or expense entries that generate transactions automatically

### Multi-Currency

- **Exchange rates** — exchange rates from the National Bank of Kazakhstan (NBK) are fetched and cached
- **Currency picker** — select a base and quote currency with live conversion preview

### Cloud and Auth

- **Google Sign-In** — one-tap sign-in via Firebase Auth + CredentialManager (no deprecated `GoogleSignInClient`)
- **Encrypted Firestore sync** — local Room data is mirrored to Firestore; all field values are encrypted at rest using Google Tink (AES-GCM + HKDF)
- **Profile** — signed-in users see their Google account photo (loaded via Coil 3) in Settings

### Settings and Theming

- **Theme** — System / Light / Dark, with Material You dynamic color on Android 12+
- **Language** — Russian, English, Kazakh (runtime locale switch without restart)
- **Currency** — configurable default display currency
- **Data** — sign-out, account deletion

---

## Tech Stack

### Application

| Library | Version |
|---|---|
| Kotlin | 2.3.0 |
| Android Gradle Plugin (AGP) | 8.13.2 |
| KSP | 2.3.1 |
| Jetpack Compose BOM | 2026.01.01 |
| Navigation Compose (type-safe) | 2.9.7 |
| Hilt | 2.58 |
| Hilt Navigation Compose | 1.3.0 |
| Room | 2.8.4 |
| Kotlinx Coroutines | 1.10.2 |
| Preferences DataStore | 1.1.7 |
| Kotlinx Serialization JSON | 1.10.0 |
| Kotlinx DateTime | 0.7.1 |
| Kotlinx Collections Immutable | 0.4.0 |
| Vico (charts, compose-m3) | 2.4.3 |
| Firebase BOM | 34.8.0 |
| Firebase Auth | (BOM-managed) |
| Firebase Firestore | (BOM-managed) |
| Firebase Remote Config | (BOM-managed) |
| Firebase AI (Gemini 2.5 Flash) | (BOM-managed) |
| Google Tink Android | 1.8.0 |
| AndroidX Credentials | 1.3.0 |
| Google Identity (googleid) | 1.1.1 |
| Coil 3 | 3.3.0 |
| PdfBox-Android | 2.0.27.0 |
| Timber | 5.0.1 |
| LeakCanary (debug) | 2.14 |
| Splash Screen | 1.0.1 |

### Testing

| Library | Version |
|---|---|
| JUnit 4 | 4.13.2 |
| AndroidX Test Ext JUnit | 1.3.0 |
| MockK | 1.14.9 |
| Turbine | 1.2.1 |
| Roborazzi | 1.41.0 |
| Robolectric | 4.14.1 |
| Espresso Core | 3.7.0 |
| Detekt | 1.23.8 |
| Detekt Compose Rules | 0.5.6 |

---

## Architecture

### Overview

The project follows **MVVM + Clean Architecture** with strict layer boundaries enforced at the Gradle module level. There are approximately 40 Gradle modules organized into four layers.

```
Presentation  -->  Domain  -->  core:model
     |                              ^
     v                              |
   (no)          Data  ------------>+
                  |
             core:database
```

- `presentation/{feature}` depends on `domain/{feature}` and `core:model`
- `data/{feature}` depends on `domain/{feature}` and `core:database`
- Presentation modules **never** depend on `core:database`
- Domain and data modules **never** depend on presentation

### Module Tree

```
MoneyManager/
├── app/                              # NavHost, Hilt component wiring, MainActivity
│
├── domain/
│   ├── transactions/                 # TransactionRepository interface + CRUD use cases
│   ├── categories/                   # CategoryRepository interface + CRUD use cases
│   ├── accounts/                     # AccountRepository interface + CRUD use cases
│   ├── statistics/                   # GetPeriodSummaryUseCase + period models
│   ├── import/                       # ParseStatementUseCase + ImportTransactionsUseCase
│   ├── auth/                         # AuthRepository interface + SignIn/SignOut use cases
│   ├── exchangerate/                 # ExchangeRateRepository + use cases
│   ├── recurring/                    # RecurringRepository + use cases
│   ├── budgets/                      # BudgetRepository + use cases
│   └── sync/                         # SyncRepository interface
│
├── data/
│   ├── transactions/                 # TransactionRepositoryImpl + mapper + DI module
│   ├── categories/                   # CategoryRepositoryImpl + mapper + DI module
│   ├── accounts/                     # AccountRepositoryImpl + mapper + DI module
│   ├── auth/                         # FirebaseAuthRepositoryImpl + DI module
│   ├── exchangerate/                 # Exchange rate API client + DI module
│   ├── sync/                         # SyncManager: Room <-> Firestore bidirectional sync
│   ├── recurring/                    # RecurringRepositoryImpl + DI module
│   └── budgets/                      # BudgetRepositoryImpl + DI module
│
├── presentation/
│   ├── transactions/                 # TransactionListScreen, TransactionEditScreen, ViewModel
│   ├── categories/                   # CategoryListScreen, CategoryEditScreen, ViewModel
│   ├── accounts/                     # AccountListScreen, AccountEditScreen, ViewModel
│   ├── statistics/                   # StatisticsScreen, StatisticsCategoryScreen, ViewModel
│   ├── import/                       # ImportScreen, ViewModel
│   ├── settings/                     # SettingsScreen, CurrencyPickerScreen, ViewModel
│   ├── onboarding/                   # OnboardingScreen, OnboardingSetupScreen, ViewModel
│   ├── auth/                         # SignInScreen, SignInViewModel
│   ├── recurring/                    # RecurringListScreen, RecurringEditScreen, ViewModel
│   └── budgets/                      # BudgetListScreen, BudgetEditScreen, ViewModel
│
└── core/
    ├── model/                        # Domain models: Account, Transaction, Category, Budget…
    ├── database/                     # Room database, entities, DAOs, migrations
    ├── datastore/                    # Preferences DataStore: theme, language, cached configs
    ├── ui/                           # Material 3 theme, shared Compose components
    ├── common/                       # Extensions, utility functions
    ├── ai/                           # GeminiService wrapper (Firebase AI SDK)
    ├── parser/                       # PDF text extraction, BankDetector, table parser
    ├── remoteconfig/                 # Firebase Remote Config wrapper + ParserConfig model
    ├── auth/                         # CredentialManager wrapper
    ├── firestore/                    # Firestore SDK wrapper, encrypted DTO types
    └── crypto/                       # Google Tink AES-GCM encryption helpers
```

### UI State Pattern

Each screen follows unidirectional data flow:

```
ViewModel
  StateFlow<UiState>  -->  Screen (@Composable)
  event functions     <--  user interactions
```

`UiState` is a sealed class or data class held in a `StateFlow`. Screens collect state with `collectAsStateWithLifecycle()` and send user events back as function calls on the ViewModel. Immutable collections (`kotlinx-collections-immutable`) are used in state types to ensure structural equality checks work correctly with Compose.

### Package Naming

```
com.atelbay.money_manager.{domain|data|presentation|core}.{feature}.*
```

---

## Getting Started

### Prerequisites

| Tool | Minimum version |
|---|---|
| Android Studio | Meerkat (2024.3.1) or newer |
| JDK | 17 (Temurin recommended) |
| Android SDK | API 26 (minSdk), API 35 (compileSdk) |
| Gradle | Wrapper included (no separate install needed) |

### Firebase Setup

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name `com.atelbay.money_manager`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Enable **Authentication** (Google provider).
5. Enable **Firestore** (start in test or production mode as appropriate).
6. Enable **Remote Config** and **AI (Vertex AI for Firebase / Gemini API)**.

### Clone and Build

```bash
git clone https://github.com/<your-org>/MoneyManager.git
cd MoneyManager

# Place google-services.json in app/
cp /path/to/google-services.json app/google-services.json

# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Static analysis
./gradlew lint
./gradlew detekt
```

---

## Release Build

Signing is resolved from two sources in priority order:

1. **`keystore.properties`** file in the project root (local development)
2. **Environment variables** (CI/CD)

### keystore.properties (local)

Create `keystore.properties` in the project root (this file is git-ignored):

```properties
storeFile=app/keystore.jks
storePassword=<store-password>
keyAlias=<key-alias>
keyPassword=<key-password>
```

### Environment variables (CI/CD)

| Variable | Description |
|---|---|
| `SIGNING_STORE_FILE` | Path to the keystore file |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

The release workflow also expects a base64-encoded keystore as `KEYSTORE_BASE64` secret (decoded to `app/keystore.jks` at build time).

Build the signed release APK or AAB:

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

---

## CI/CD

All pipelines run on `ubuntu-latest` with JDK 17 (Temurin). The `google-services.json` file is injected at runtime from the `GOOGLE_SERVICES_JSON` GitHub secret (base64-encoded).

| Workflow | File | Trigger | Description |
|---|---|---|---|
| CI | `ci.yml` | Pull request to `main` | Compile gate (Kotlin), Android Lint, unit tests. Cancels in-progress runs on new pushes. |
| Build Debug APK | `build.yml` | Manual (`workflow_dispatch`) | Assembles a debug APK and uploads it as a GitHub Actions artifact (14-day retention). |
| QA Build | `qa.yml` | Manual (`workflow_dispatch`) | Assembles a debug APK for QA distribution and uploads it as an artifact (7-day retention). |
| Release | `release.yml` | Push of `v*` tag | Runs tests, signs and builds release APK + AAB, then conditionally distributes via Firebase App Distribution, uploads to Play Store internal track, and creates a GitHub Release. Each distribution step is gated by a repository variable (`ENABLE_FIREBASE`, `ENABLE_PLAY_STORE`, `ENABLE_GITHUB_RELEASE`). |

### Required GitHub Secrets

| Secret | Used by |
|---|---|
| `GOOGLE_SERVICES_JSON` | All workflows |
| `KEYSTORE_BASE64` | `release.yml` |
| `SIGNING_KEY_ALIAS` | `release.yml` |
| `SIGNING_KEY_PASSWORD` | `release.yml` |
| `SIGNING_STORE_PASSWORD` | `release.yml` |
| `FIREBASE_APP_ID` | `release.yml` (Firebase distribution) |
| `FIREBASE_SERVICE_CREDENTIALS` | `release.yml` (Firebase distribution) |
| `PLAY_SERVICE_ACCOUNT_JSON` | `release.yml` (Play Store) |

---

## Navigation

The app uses type-safe Navigation Compose (serializable destinations). The navigation graph is as follows:

```
[Splash / SplashScreen]
        |
        +--> [Onboarding]
        |         |
        |         +--> [OnboardingSetup]
        |                    |
        |                    +--> [CreateAccount]
        |                               |
        v                               v
                         [Home / TransactionList]
                         |        |        |
              [Statistics]  [AccountList]  [Settings]
                   |              |              |
    [StatisticsCategoryTx]  [AccountEdit]  [CurrencyPicker]
                                           |
                                        [SignIn]

  From any bottom-nav tab:
  [Home] --> [TransactionEdit]
  [Home] --> [CategoryList] --> [CategoryEdit]
  [Home] --> [RecurringList] --> [RecurringEdit]
  [Home] --> [BudgetList] --> [BudgetEdit]
  [Home] --> [Import]  (also reachable via PDF share intent)
```

Navigation destinations are defined as `@Serializable` data objects and data classes in `app/src/main/java/com/atelbay/money_manager/navigation/Destinations.kt`.

---

## Permissions

The app declares a single manifest permission:

| Permission | Purpose |
|---|---|
| `android.permission.INTERNET` | Exchange rate API, Firebase services, Gemini AI |

PDF files are received through `ACTION_SEND` / `ACTION_VIEW` intent filters (no storage permission required on Android 10+).

---

## Project Context

Money Manager is developed as part of a master's thesis on **UI test automation for Android**. The primary goal is to build an application containing a representative set of real-world UI patterns — navigation graphs, multi-step forms, lazy lists, charts, modals, and theming — so that the test automation methodology can be validated against non-trivial, production-realistic code.

The test pyramid includes:
- **Unit tests** — ViewModels and use cases tested with MockK and Turbine
- **Screenshot tests** — Compose screens verified with Roborazzi + Robolectric (no emulator required)
- **Instrumented tests** — full UI flows with `ComposeTestRule` and Espresso

---

## License

<!-- TODO: Add license -->
