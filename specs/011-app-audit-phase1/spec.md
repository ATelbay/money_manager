# Feature Specification: App Audit Phase 1 — Critical & High Priority Fixes

**Feature Branch**: `011-app-audit-phase1`
**Created**: 2026-03-20
**Status**: Draft
**Input**: Systematic audit and fix of 5 critical/high-priority issues found during full-app code review

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Financial Data Protected from Cloud Backup (Priority: P1)

A user has sensitive financial data (transactions, account balances) stored in the app. When their device performs automatic Google Drive backups, the financial database and preferences must NOT be included in the backup, preventing unencrypted financial data from being exposed to cloud storage.

**Why this priority**: This is a **security/privacy critical** issue. Financial data backed up unencrypted to Google Drive creates a data exposure risk for every user with auto-backup enabled. This affects all users silently and has the highest blast radius.

**Independent Test**: Can be verified by triggering a manual backup via ADB backup command and confirming the database file is excluded from the backup set.

**Acceptance Scenarios**:

1. **Given** the app is installed with financial data, **When** the device performs an automatic backup, **Then** the Room database containing financial transactions is excluded from the backup
2. **Given** the app is installed with user preferences, **When** the device performs an automatic backup, **Then** shared preferences are excluded from the backup
3. **Given** a user restores the app from backup on a new device, **Then** no stale financial data is restored from a previous backup

---

### User Story 2 - Save Button Recovers After Error (Priority: P1)

A user is editing an account or category and taps Save. If an unexpected error occurs during the save operation (e.g., database constraint violation, disk full), the save button must become enabled again so the user can retry or correct their input — rather than being permanently stuck in a disabled/"saving" state.

**Why this priority**: A stuck save button is a **data-loss scenario** — the user cannot save their work and must force-quit the app, potentially losing all edits. This directly impacts core CRUD functionality.

**Independent Test**: Can be tested by triggering a save error (e.g., duplicate name constraint) and verifying the save button re-enables with an error message displayed.

**Acceptance Scenarios**:

1. **Given** a user is on the Account Edit screen and taps Save, **When** the save operation fails with an error, **Then** the save button becomes enabled again AND an error message is displayed
2. **Given** a user is on the Category Edit screen and taps Save, **When** the save operation fails, **Then** the save button becomes enabled again AND an error message is displayed
3. **Given** a user is editing an existing account, **When** the save succeeds, **Then** only the user-modified fields (name, icon, currency) are updated — balance and creation date are preserved from the original record

---

### User Story 3 - App Displays Correct Language for All Text (Priority: P2)

A user who has selected English or Kazakh as their app language sees all interface text in their chosen language. Currently, ~15+ strings appear in Russian regardless of language setting — in error messages, import preview screens, and validation messages. All user-facing text must respect the selected language.

**Why this priority**: Broken localization degrades the experience for non-Russian-speaking users and makes the app appear unpolished. This affects every screen with hardcoded strings.

**Independent Test**: Can be tested by switching app language to English, navigating through all affected screens (account edit, category edit, transaction edit, import preview, settings), and verifying no Russian text appears.

**Acceptance Scenarios**:

1. **Given** the app language is set to English, **When** a user triggers a validation error on Account Edit, **Then** the error message appears in English
2. **Given** the app language is set to English, **When** a user views the Import Preview screen, **Then** all labels, headers, and messages appear in English
3. **Given** the app language is set to Kazakh, **When** a user triggers any error across all screens, **Then** error messages appear in Kazakh
4. **Given** the app language is set to English, **When** a user navigates to Settings and triggers an exchange rate update error, **Then** the error message appears in English

---

### User Story 4 - Fast Query Performance on Growing Data (Priority: P2)

As a user accumulates hundreds or thousands of transactions, the app remains responsive. Database queries for filtering transactions by category, viewing statistics by date range, and listing active accounts/categories execute efficiently without noticeable delays.

**Why this priority**: Without proper database indexes, query performance degrades as data grows. Users with 6+ months of data will experience progressively slower loading times on statistics and transaction list screens.

**Independent Test**: Can be verified by checking that database indexes exist after migration, and that query execution uses index scans rather than full table scans.

**Acceptance Scenarios**:

1. **Given** a user has 1000+ transactions, **When** they open the Statistics screen filtered by category and date range, **Then** data loads without perceptible delay
2. **Given** a user upgrades from the previous app version, **When** the app starts, **Then** the database migration runs successfully and indexes are created without data loss
3. **Given** a fresh install, **When** the database is first created, **Then** all indexes are present from the initial schema

---

### User Story 5 - Clean Architecture for Sync Feature (Priority: P3)

The app's sync functionality follows the same layered architecture pattern as all other features. The settings screen does not directly reference data-layer implementation details, making the codebase consistent and maintainable.

**Why this priority**: This is a **code quality / architecture** issue. While invisible to end users, the architecture violation makes the settings module harder to test and creates a precedent for bypassing layer boundaries. Lower priority because it has no user-facing impact.

**Independent Test**: Can be verified by checking that the settings presentation module has no dependency on any data module in its build configuration, and that the app compiles and sync functionality works as before.

**Acceptance Scenarios**:

1. **Given** the settings screen needs to display sync status, **When** it requests sync information, **Then** it communicates through a domain-layer interface (not data-layer classes)
2. **Given** a user triggers a sync retry from settings, **When** the retry is initiated, **Then** it flows through the domain interface to the data-layer implementation
3. **Given** the architecture is refactored, **When** the full app is built, **Then** all existing functionality works identically to before

---

### Edge Cases

- What happens if backup rules files contain malformed XML? The app still installs and functions — backup falls back to default behavior
- What happens if a save error occurs while the device is offline and the error is a network timeout? The save button must still recover
- What happens if a user has a mix of Russian and English text in custom category/account names? The localization system only affects system-provided strings, not user-entered data
- What happens if the database migration fails mid-way (e.g., device runs out of storage)? The migration transaction ensures atomicity — either all indexes are created or none
- What happens if a user downgrades the app after the database migration? The system will reject the version mismatch — this is standard behavior and acceptable

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST exclude the financial database file from cloud backup on both modern (12+) and legacy (11 and below) Android versions
- **FR-002**: System MUST exclude all shared preferences from cloud backup
- **FR-003**: System MUST re-enable the save button when a save operation fails in Account Edit
- **FR-004**: System MUST re-enable the save button when a save operation fails in Category Edit
- **FR-005**: System MUST display a user-facing error message when a save operation fails in Account Edit or Category Edit
- **FR-006**: System MUST preserve the original balance and creation date when updating an existing account (not overwrite with defaults)
- **FR-007**: All user-facing strings in validation errors, import preview, and settings MUST display in the user's selected language (Russian, English, or Kazakh)
- **FR-008**: System MUST have database indexes on frequently-queried columns: account soft-delete flag and remote identifier, category type and soft-delete flag, and a composite transaction index for statistics queries
- **FR-009**: System MUST include a backwards-compatible database migration that creates the new indexes without data loss for users upgrading from the current version
- **FR-010**: The settings/sync feature MUST communicate through a domain-layer interface, with no direct dependency from presentation to data modules

### Key Entities

- **Backup Rules**: Configuration that controls which app data is included in/excluded from the device's automatic cloud backup
- **AppStrings**: The app's localization system providing translated strings for Russian, English, and Kazakh languages
- **Database Index**: Optimization structure on database tables that speeds up queries on specific columns
- **SyncRepository**: Domain-layer interface abstracting the sync data operations from the presentation layer

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero financial data files are included in cloud backups (verified via backup inspection)
- **SC-002**: Save button recovery time after error is under 1 second — button re-enables immediately when an error occurs
- **SC-003**: 100% of user-facing strings display in the selected language across all affected screens (Account Edit, Category Edit, Transaction Edit, Import Preview, Settings, Sign-In, Category Transactions)
- **SC-004**: All existing automated tests continue to pass after changes (zero test regressions)
- **SC-005**: App builds successfully with no compilation errors after all changes
- **SC-006**: Database queries on indexed columns perform efficiently without full table scans
- **SC-007**: No presentation module has a build dependency on any data module after the architecture refactor

## Assumptions

- The app currently uses the platform's default backup behavior (full backup enabled), which is why the stock template rules result in financial data being backed up
- The localization system already supports Russian, English, and Kazakh — new keys just need to be added to the existing interfaces and implementations
- The current database version can be determined from the existing schema annotation and incremented by 1 for the migration
- The sync classes have a small enough API surface that extracting a domain interface is straightforward
- Users upgrading from the current version will have their database migrated automatically on first launch — no manual action required

## Scope Boundaries

**In scope**:
- The 5 specific issues described (S1, A2, L1+L2, D1, A1)
- All files and modules directly affected by these fixes

**Out of scope**:
- Other audit findings not in Phase 1
- Adding new features or functionality
- Refactoring code beyond what's needed for the 5 fixes
- Adding new automated tests (beyond ensuring existing tests pass)
- UI/UX redesign of any screens
