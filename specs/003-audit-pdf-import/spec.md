# Feature Specification: PDF Import Audit & Bereke Fix

**Feature Branch**: `003-audit-pdf-import`
**Created**: 2026-03-10
**Status**: Draft

## Overview

Users share PDF bank statements from other apps to Money Manager to import their transactions automatically. The app parses the PDF with pattern-matching rules specific to each bank. A confirmed bug exists: Bereke Bank statements fail pattern matching and fall back to an AI service, making import dependent on an internet connection and incurring usage costs. This audit fixes Bereke and verifies all 5 supported banks work correctly with real PDF files.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Bereke Bank Import Works Offline (Priority: P1)

A user with a Bereke Bank account opens the bank's mobile app, downloads their statement as a PDF, and shares it to Money Manager. The import should complete successfully without requiring an internet connection and without calling any AI service.

**Why this priority**: This is the confirmed bug. Users with Bereke Bank get an inferior experience — either import fails entirely without a connection or runs slowly/costs money via AI. Fixing this is the primary goal.

**Independent Test**: Share `bereke_statement.pdf` to Money Manager on a device with Wi-Fi disabled. Verify the import preview screen shows the correct transaction list with amounts, dates, and types.

**Acceptance Scenarios**:

1. **Given** a Bereke Bank PDF statement and no internet connection, **When** the user shares the PDF to Money Manager, **Then** the import preview screen appears with all transactions correctly extracted — no AI service is called.
2. **Given** a Bereke Bank PDF statement, **When** the user confirms the import, **Then** the transactions are saved with correct amounts, dates, income/expense types, and descriptions.
3. **Given** a Bereke Bank PDF, **When** the pattern matching is applied, **Then** the number of extracted transactions matches the transaction count visible in the PDF.

---

### User Story 2 - All 5 Banks Import Correctly (Priority: P2)

A user with accounts at Kaspi, Freedom, Forte, or Eurasian Bank shares a PDF statement to Money Manager and gets a correct transaction preview without needing an internet connection.

**Why this priority**: While the Bereke bug is confirmed, the audit must also verify no regressions exist for the other four banks. A single broken bank would block a segment of users.

**Independent Test**: Share each of the 5 bank PDF files to Money Manager one at a time with internet disabled. Each must produce a correct preview.

**Acceptance Scenarios**:

1. **Given** a Kaspi Bank PDF (gold_statement.pdf), **When** shared to Money Manager, **Then** transactions are imported with correct amounts in KZT (space-comma format) and correct income/expense classification.
2. **Given** a Freedom Bank PDF, **When** shared to Money Manager, **Then** multi-line transaction descriptions are joined correctly and amounts use comma-dot format.
3. **Given** a Forte Bank PDF, **When** shared to Money Manager, **Then** transactions with negative amounts are classified as expenses and bonus/fast-payment rows are parsed correctly.
4. **Given** a Eurasian Bank PDF, **When** shared to Money Manager, **Then** foreign-currency transaction triplets are deduplicated to one transaction (the KZT amount).
5. **Given** any of the 5 bank PDFs, **When** the user completes import, **Then** the account balance updates by the net sum of all imported transactions.

---

### User Story 3 - Share Intent Works in All App States (Priority: P3)

A user can share a PDF to Money Manager regardless of whether the app is not running (cold start), running in the background (warm), or showing the onboarding screen.

**Why this priority**: Share Intent is the primary entry point for this feature. If it fails in certain app states, users get a silent failure or are stuck on the wrong screen.

**Independent Test**: Share a PDF to Money Manager in each of the three states (cold start, warm/background, onboarding active). Verify the import screen opens each time.

**Acceptance Scenarios**:

1. **Given** the app is not running, **When** the user shares a PDF from another app, **Then** Money Manager launches and navigates directly to the import preview screen.
2. **Given** the app is running in the background, **When** the user shares a PDF from another app, **Then** the app comes to the foreground and navigates to the import preview screen.
3. **Given** the app is showing the onboarding screen, **When** the user shares a PDF, **Then** the pending import is queued and the import screen opens automatically once onboarding is completed.
4. **Given** an ACTION_VIEW intent with a PDF URI, **When** received by the app, **Then** the import screen opens with the correct file.
5. **Given** an ACTION_SEND intent with a PDF, **When** the PDF URI is a content:// URI, **Then** the app successfully reads the file (persistent URI permission granted).

---

### User Story 4 - Integration Tests Verify Parsing with Real PDFs (Priority: P4)

Developers running the test suite can confirm that each bank's parser produces the expected transactions from real PDF files without mocking the PDF extraction layer.

**Why this priority**: The existing unit tests use hardcoded text strings, which is why the Bereke bug was missed — the actual PdfBox output differs from the hardcoded strings. Integration tests using real PDF files close this gap.

**Independent Test**: Run the parser integration test suite and verify all tests pass. Tests must read real PDF files and assert specific transaction counts and values.

**Acceptance Scenarios**:

1. **Given** the `bereke_statement.pdf` file, **When** processed through the full extraction + parsing pipeline, **Then** the test asserts the correct number of transactions with correct amounts and types (no reliance on hardcoded text).
2. **Given** each of the 5 bank PDF files, **When** processed through the full pipeline, **Then** integration tests pass without any AI service call.
3. **Given** a PDF where PdfBox output uses different whitespace or line endings than hardcoded test strings, **Then** the integration test catches the mismatch that unit tests miss.

---

### Edge Cases

- What happens when a user shares a PDF from an unknown bank? The app should fall back to AI parsing (existing behavior — no change to this flow).
- What happens when the PDF is password-protected or corrupted? The import screen shows a clear error message.
- What happens when a Bereke Bank PDF has zero transactions? The import preview shows an empty state rather than triggering AI fallback.
- What happens when the Eurasian Bank PDF contains only KZT transactions (no foreign currency)? Single-row transactions must not be deduplicated away.
- What happens when the user shares a non-PDF file? The app shows an error instead of crashing.
- What happens if the content:// URI becomes invalid before the app reads it? The app shows an error message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST parse Bereke Bank PDF statements using pattern matching, without calling the AI service, when the PDF matches Bereke's detection markers. The corrected pattern MUST be applied to both the local fallback JSON and Firebase Remote Config.
- **FR-002**: The app MUST correctly extract transaction date, amount, income/expense type, and description for all 5 supported banks (Kaspi, Freedom, Forte, Bereke, Eurasian).
- **FR-003**: The app MUST NOT call the AI service for any of the 5 supported banks when the PDF is a valid statement from that bank.
- **FR-004**: The app MUST open the import screen when a PDF is shared via ACTION_SEND or ACTION_VIEW in any app state (cold start, warm, onboarding).
- **FR-005**: Integration tests MUST use real PDF files (not hardcoded text strings) as input to the extraction + parsing pipeline. Tests MUST run on the host machine (JVM) without a device or emulator, with PDF files placed in the test resources directory.
- **FR-006**: Integration tests MUST assert the transaction count and at least one transaction's amount, date, and type for each of the 5 banks.
- **FR-007**: The existing behavior for unsupported banks (AI fallback) MUST remain unchanged.
- **FR-008**: The Eurasian Bank parser MUST deduplicate foreign-currency transaction triplets to a single transaction with the KZT amount.
- **FR-009**: The Freedom Bank parser MUST correctly join multi-line transaction descriptions into a single transaction record.
- **FR-010**: All parser fixes MUST be covered by both the new integration tests and updated/added unit tests as needed.

### Key Entities

- **ParserConfig**: Bank-specific configuration defining detection markers, regex patterns, date/amount formats, and special flags (negativeSignMeansExpense, useNamedGroups, deduplicateMaxAmount, joinLines).
- **ParsedTransaction**: A single extracted transaction with date, amount, income/expense type, and description — the output of the parsing pipeline.
- **Share Intent**: The Android system mechanism (ACTION_SEND or ACTION_VIEW) that delivers a PDF file URI to the app from another application.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of Bereke Bank test statements parse correctly using pattern matching — zero AI service calls for Bereke PDFs.
- **SC-002**: All 5 supported banks pass integration tests that process real PDF files end-to-end (no mocked text extraction).
- **SC-003**: The import screen opens successfully in all 3 app states (cold start, warm, onboarding) as verified by manual or automated testing.
- **SC-004**: Integration test coverage reaches 5 banks × at least 3 assertions each (count, amount, type) — 15+ verifiable claims on real PDF output.
- **SC-005**: No regression: existing unit tests for Kaspi, Freedom, Forte, Eurasian continue to pass after any parser changes.

## Clarifications

### Session 2026-03-10

- Q: When a PDF is shared during onboarding, should import be queued or interrupt onboarding? → A: Queue the import — onboarding finishes normally, then the import screen opens automatically.
- Q: Should integration tests run as JVM tests or instrumented Android tests? → A: JVM tests — run on host machine, PDF files in test resources, no emulator needed.
- Q: Should the Bereke regex fix target only the local fallback JSON or also Firebase Remote Config? → A: Fix both — update local fallback JSON and push the corrected pattern to Firebase Remote Config.

## Assumptions

- The 5 PDF files already present in the project root (`gold_statement.pdf`, `freedom_statement.pdf`, `forte_statement.pdf`, `bereke_statement.pdf`, `eurasian_statement.pdf`) are valid, real-world bank statements and will be used as test fixtures.
- Parser configurations (ParserConfig) are fetched from Firebase Remote Config with a local fallback JSON; this audit fixes the Bereke pattern in both the local fallback JSON and Firebase Remote Config.
- The Bereke bug is caused by a mismatch between PdfBox's actual text output (whitespace, newlines, encoding) and the regex pattern or hardcoded test strings — the root cause will be confirmed during investigation.
- Share Intent handling logic in MainActivity is assumed correct for the primary case; testing in different app states is validation, not a suspected bug area.
- No new banks will be added as part of this audit.
- The AI fallback remains in place as a safety net for unsupported banks; this audit does not remove or redesign it.
