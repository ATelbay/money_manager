# Tasks: PDF Import Audit & Bereke Fix

**Input**: Design documents from `/specs/003-audit-pdf-import/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅

**Tests**: Integration tests are part of this feature's core deliverable (FR-005, FR-006). They are REQUIRED, not optional.

**Organization**: Phases map to user stories from spec.md (P1–P4) after a Setup + Foundational diagnosis phase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story ([US1]–[US4]) this task belongs to
- Paths use the layer-centric module structure from plan.md

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Copy PDF test fixtures and create the shared test utilities that all integration tests depend on.

- [x] T001 Copy 5 PDF files from project root into `core/parser/src/test/resources/`: `bereke_statement.pdf`, `gold_statement.pdf`, `freedom_statement.pdf`, `forte_statement.pdf`, `eurasian_statement.pdf`
- [x] T002 [P] Create `PdfTestHelper.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/PdfTestHelper.kt` — singleton object with `extractText(resourceName: String): String` using `PDDocument.load` + `PDFTextStripper(sortByPosition=true)`, no Android context
- [x] T003 [P] Copy `core/remoteconfig/src/main/res/raw/default_parser_config.json` to `core/parser/src/test/resources/default_parser_config.json` and create `ParserConfigTestFactory.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/ParserConfigTestFactory.kt` — deserializes the JSON and exposes `getConfig(bankId: String): ParserConfig`

---

## Phase 2: Foundational (Diagnosis — Blocks US1 Fix)

**Purpose**: Determine the exact PdfBox text output for Bereke before writing any fix. This phase MUST complete before Phase 3.

**⚠️ CRITICAL**: The bug root cause is unknown until T005 runs. Do not attempt T006 (the fix) before completing this phase.

- [x] T004 Create `BerekePdfDiagnosticTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/BerekePdfDiagnosticTest.kt` — single test that loads `bereke_statement.pdf` via `PdfTestHelper`, prints the full raw text to stdout, then asserts `text.isNotBlank()`
- [x] T005 Run `./gradlew :core:parser:test --tests "*.BerekePdfDiagnosticTest" --info` and capture the `=== RAW PDFBOX OUTPUT ===` block; compare line-by-line against `BerekeBankParserTest` hardcoded strings to identify: (a) which specific lines fail regex matching, (b) whether the cause is missing `skipPatterns`, wrong join heuristic, or pattern mismatch

**Checkpoint**: Root cause confirmed. Fix strategy selected (see research.md hypotheses H1–H4).

---

## Phase 3: User Story 1 — Bereke Bank Import Works Offline (Priority: P1) 🎯 MVP

**Goal**: Bereke Bank PDFs parse via regex with zero AI fallback calls — works fully offline.

**Independent Test**: Run `./gradlew :core:parser:test --tests "*Bereke*"` — all tests pass. Then share `bereke_statement.pdf` to app with Wi-Fi disabled; import preview shows correct transactions.

- [x] T006 [US1] Based on T005 findings, fix Bereke bank entry in `core/remoteconfig/src/main/res/raw/default_parser_config.json` — update `transactionPattern` and/or `skipPatterns` to match real PdfBox output (the specific change depends on T005 output)
- [x] T007 [US1] Review `BerekeBankParserTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/BerekeBankParserTest.kt` — if any hardcoded strings don't match real PdfBox format discovered in T005, update them to reflect actual joined-line format; add new test cases for any edge cases revealed
- [x] T008 [US1] Add diagnostic `Timber.d` logging to `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — log before AI fallback with 3 distinct messages: "bank not detected", "bank X detected but 0 transactions parsed", "bank X parsed N transactions via regex"
- [x] T009 [US1] Write `BerekeBankIntegrationTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/BerekeBankIntegrationTest.kt` — load `bereke_statement.pdf` via `PdfTestHelper`, detect bank via `BankDetector` + `ParserConfigTestFactory`, parse via `RegexStatementParser`, assert: (a) bank detected as "bereke", (b) transaction count matches known value from T005, (c) at least one specific transaction's date / amount / type matches the real PDF
- [x] T010 [US1] Run `./gradlew :core:parser:test --tests "*Bereke*"` — confirm `BerekeBankParserTest` (11 unit tests) + `BerekeBankIntegrationTest` (≥3 tests) all pass; fix any regressions before proceeding

**Checkpoint**: Bereke Bank fix complete. US1 fully functional and testable independently.

---

## Phase 4: User Story 2 — All 5 Banks Import Correctly (Priority: P2)

**Goal**: Kaspi, Freedom, Forte, and Eurasian banks all parse correctly via regex with real PDFs — no regressions.

**Independent Test**: Run `./gradlew :core:parser:test` — all 5 bank integration tests pass.

- [x] T011 [P] [US2] Write `KaspiBankIntegrationTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/KaspiBankIntegrationTest.kt` — load `gold_statement.pdf`, detect bank as "kaspi", assert transaction count > 0, assert at least one transaction: date format `dd.MM.yy` parsed correctly, amount in KZT (space-comma), correct EXPENSE/INCOME type
- [x] T012 [P] [US2] Write `FreedomBankIntegrationTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/FreedomBankIntegrationTest.kt` — load `freedom_statement.pdf`, detect bank as "freedom", assert transaction count > 0, assert multi-line descriptions joined correctly (details field not truncated mid-word), assert comma-dot amount parsing correct
- [x] T013 [P] [US2] Write `ForteBankIntegrationTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/ForteBankIntegrationTest.kt` — load `forte_statement.pdf`, detect bank as "forte", assert transaction count > 0, assert at least one EXPENSE with `negativeSignMeansExpense=true` logic, assert at least one "Пополнение счета" → INCOME
- [x] T014 [P] [US2] Write `EurasianBankIntegrationTest.kt` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/EurasianBankIntegrationTest.kt` — load `eurasian_statement.pdf`, detect bank as "eurasian", assert `deduplicateMaxAmount` reduces triplets (pre-dedup count > post-dedup count for foreign-currency statements), assert final transaction count > 0 with correct amounts
- [x] T015 [US2] Run `./gradlew :core:parser:test` — all existing unit tests (BankDetectorTest, RegexStatementParserTest, FreedomBankParserTest, EurasianBankParserTest, ForteBankParserTest, BerekeBankParserTest) + all 5 integration tests must pass; investigate and fix any failures

**Checkpoint**: All 5 banks pass integration tests on real PDFs. US2 fully functional.

---

## Phase 5: User Story 3 — Share Intent Works in All App States (Priority: P3)

**Goal**: PDF shared to Money Manager opens the import screen in all 3 app states — cold start, warm, onboarding.

**Independent Test**: Manual device verification with 3 scenarios.

**Note**: No code changes needed (confirmed in research.md). This phase is manual verification only.

- [ ] T016 [US3] Manual verification — **Cold start**: force-stop Money Manager → share `bereke_statement.pdf` from Files app → verify Money Manager opens and import screen appears with transaction list
- [ ] T017 [US3] Manual verification — **Warm start**: navigate Money Manager to Home screen → press Home button → share `bereke_statement.pdf` → verify app foregrounds and import screen appears
- [ ] T018 [US3] Manual verification — **Onboarding queue**: clear app data (fresh install state) → share `bereke_statement.pdf` → complete onboarding flow → verify import screen opens automatically with the PDF after onboarding finishes

**Checkpoint**: Share Intent works in all 3 app states. US3 verified.

---

## Phase 6: User Story 4 — Integration Tests Verify Real PdfBox Output (Priority: P4)

**Goal**: Integration test suite provides 15+ verifiable claims on real PDF output (FR-006, SC-004).

**Independent Test**: Run `./gradlew :core:parser:test` and count assertions: each bank must have ≥3 explicit value assertions.

- [x] T019 [US4] Audit all 5 integration tests (T009, T011–T014) — each test MUST assert all three: (a) exact transaction count (not just `> 0`), (b) specific transaction amount (`assertEquals(expectedAmount, tx.amount, 0.01)`), (c) specific transaction type — add or update assertions in integration test files as needed
- [x] T020 [US4] Add "no AI fallback" assertion to each integration test: confirm `BankDetector.detect(text, configs) != null` with the correct `bankId` — this directly verifies FR-003 ("app MUST NOT call AI service for supported banks when regex succeeds")

**Checkpoint**: 15+ verifiable assertions across 5 banks. US4 complete. Ready for polish phase.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Deploy the fix to production config, final CI validation, offline verification.

- [ ] T021 [P] Update `parser_configs` key in Firebase Remote Config (Firebase console or CLI) with the fixed Bereke `transactionPattern` and any updated `skipPatterns` from T006 — ensures existing app installs get the fix without an app update
- [ ] T022 [P] Run full CI gate: `./gradlew :core:parser:test assembleDebug lint detekt` — all checks must pass; no new lint/detekt violations introduced
- [ ] T023 Offline integration verification — disable device Wi-Fi and mobile data → share each of the 5 bank PDFs to Money Manager one by one → confirm import preview appears for each with correct transactions → confirm Logcat shows "parsed N transactions via regex" (not "AI fallback") for all 5

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately; T002 and T003 run in parallel
- **Foundational (Phase 2)**: Depends on T001 (PDFs in resources) and T002 (PdfTestHelper exists) — BLOCKS Phase 3
- **US1 (Phase 3)**: Depends on Foundational completion (T005 output determines T006 fix)
- **US2 (Phase 4)**: Depends on Phase 1 Setup complete; T011–T014 are independent of US1 fix and each other (different files)
- **US3 (Phase 5)**: Depends on US1 fix deployed (need Bereke to work for meaningful test)
- **US4 (Phase 6)**: Depends on T009, T011–T014 existing
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Depends on Foundational diagnosis (T005) — primary fix, MVP deliverable
- **US2 (P2)**: Depends only on Setup (Phase 1 complete) — T011–T014 can run in parallel with US1 implementation
- **US3 (P3)**: Depends on US1 fix (need working Bereke import for meaningful verification)
- **US4 (P4)**: Depends on T009, T011–T014 (integration tests must exist before auditing their assertions)

### Critical Path

```
T001 → T004 → T005 → T006 → T009 → T010
  ↓
T002 → (T011 || T012 || T013 || T014) → T015
T003 ↗
```

---

## Parallel Opportunities

### Phase 1 (can run together)
```
T002: Create PdfTestHelper.kt
T003: Create ParserConfigTestFactory.kt + copy JSON
```
*(T001 must complete first — PDFs needed for diagnostic)*

### Phase 4 US2 Integration Tests (all in parallel)
```
T011: KaspiBankIntegrationTest.kt
T012: FreedomBankIntegrationTest.kt
T013: ForteBankIntegrationTest.kt
T014: EurasianBankIntegrationTest.kt
```
*(Each targets a different file; no shared state)*

### Phase 7 Polish (can run together)
```
T021: Firebase Remote Config update
T022: CI gate run
```

---

## Implementation Strategy

### MVP (User Story 1 Only)
1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Diagnosis (T004–T005) — mandatory before fix
3. Complete Phase 3: Bereke Fix (T006–T010)
4. **VALIDATE**: Run `./gradlew :core:parser:test --tests "*Bereke*"` → all pass
5. Share `bereke_statement.pdf` with Wi-Fi off → import works → MVP confirmed

### Full Delivery
1. Setup + Diagnosis → Bereke Fix (US1)
2. Add US2 integration tests for 4 remaining banks
3. Manual Share Intent verification (US3)
4. Integration test assertion audit (US4)
5. Firebase Remote Config deployment + full CI gate

---

## Notes

- T005 (diagnostic run) is the single most important step — it determines the actual fix needed
- T011–T014 can start in parallel with T006–T010 since they target different files and independent bank configs
- Integration tests at T009, T011–T014 must assert **specific values** from the real PDFs, not just `isNotEmpty()` — exact values are discovered during T005 and T011–T014 implementation
- Commit order: T006 fix → T007 unit test update → T008 logging → T009 integration test (one logical commit per US1)
- Do NOT delete `BerekePdfDiagnosticTest.kt` — convert it to a permanent diagnostic utility test that can be re-run after future PDF format changes
