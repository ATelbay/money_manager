# Tasks: AI-Driven Parser Auto-Update

**Input**: Design documents from `/specs/004-ai-regex-autoupdate/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)

## Path Conventions

Android modules use package `com.atelbay.money_manager`:
- `core/ai/src/main/java/.../core/ai/`
- `core/parser/src/main/java/.../core/parser/`
- `core/firestore/src/main/java/.../core/firestore/`
- `core/datastore/src/main/java/.../core/datastore/`
- `core/remoteconfig/src/main/java/.../core/remoteconfig/`
- `domain/importstatement/src/main/java/.../domain/importstatement/`
- `presentation/importstatement/src/main/java/.../presentation/importstatement/`
- `functions/src/` (TypeScript, Cloud Function)

---

## Phase 1: Setup

**Purpose**: Verify build, ensure existing tests pass

- [ ] T001 Verify clean build with `./gradlew assembleDebug` and `./gradlew test` pass on branch `004-ai-regex-autoupdate`

---

## Phase 2: Foundational — ReDoS Validator (US4)

**Purpose**: ReDoS validation is a prerequisite for US1 (AI-generated regex must be validated before use) and US2 (candidates must pass validation before storage). Build it first as a standalone utility.

**Goal**: Detect regex patterns vulnerable to catastrophic backtracking before they are used or stored.

**Independent Test**: Feed known-vulnerable patterns (`(a+)+`, `(a|a)*`, `\d+\d+`) and verify rejection; feed existing 5 bank patterns and verify acceptance.

- [ ] T002 [P] [US4] Create `RegexValidator` class with `@Inject constructor()` and `fun isReDoSSafe(pattern: String): Boolean` heuristic checks (nested quantifiers, overlapping alternations, adjacent overlapping groups) in `core/parser/src/main/java/.../core/parser/RegexValidator.kt`
- [ ] T003 [P] [US4] Create `RegexValidatorTest` with test cases: nested quantifiers `(a+)+` → unsafe, overlapping alternations `(a|a)*` → unsafe, adjacent groups `\d+\d+` → unsafe, all 5 existing bank patterns (kaspi, freedom, forte, bereke, eurasian) → safe, simple valid pattern → safe in `core/parser/src/test/java/.../core/parser/RegexValidatorTest.kt`

**Checkpoint**: RegexValidator is independently testable with `./gradlew :core:parser:test`

---

## Phase 3: User Story 1 — Import Statement from Unsupported Bank (Priority: P1) 🎯 MVP

**Goal**: When a PDF from an unsupported bank is imported, extract sample rows, send to Gemini to generate a ParserConfig, validate it (ReDoS + syntax), parse the full PDF with the generated config, and show preview. Falls back to full-AI if generation fails.

**Independent Test**: Import a PDF from a bank not in the 5 supported banks → verify transactions are parsed and previewed using the AI-generated regex config.

### Core Infrastructure

- [ ] T004 [P] [US1] Add `extractSampleRows(text: String): String` method to `StatementParser` — skips first 10 lines (header), takes next 10 non-empty lines as sample in `core/parser/src/main/java/.../core/parser/StatementParser.kt`
- [ ] T005 [P] [US1] Add `generateParserConfig(sampleRows: String): ParserConfig` method to `GeminiService` interface in `core/ai/src/main/java/.../core/ai/GeminiService.kt`
- [ ] T006 [US1] Implement `generateParserConfig()` in `GeminiServiceImpl` — create a second `GenerativeModel` with `responseSchema` (using `Schema.obj()` from `com.google.firebase.ai.type.Schema`) matching ParserConfig fields, build prompt with bankId normalization rules (known banks mapping + slug generation rule) and sample rows per `contracts/gemini-prompt-schema.md`, deserialize response via kotlinx.serialization in `core/ai/src/main/java/.../core/ai/GeminiServiceImpl.kt`

### Local Cache

- [ ] T007 [P] [US1] Add `cachedAiParserConfigs: Flow<String?>` getter and `setCachedAiParserConfigs(json: String)` / `clearCachedAiParserConfigs()` methods with `stringPreferencesKey("ai_parser_configs")` to `UserPreferences` in `core/datastore/src/main/java/.../core/datastore/UserPreferences.kt`

### ParseStatementUseCase — AI Config Generation Step

- [ ] T008 [US1] Extend `ParseStatementUseCase.tryRegexThenGemini()` to insert AI config generation between regex failure and full-AI fallback. New flow: (1) try Remote Config regex → (2) try cached AI configs from DataStore → (3) extract sample rows + call `geminiService.generateParserConfig()` → (4) validate with `RegexValidator.isReDoSSafe()` + compile regex for syntax check (`Regex(pattern)` in try/catch) + validate dateFormat (`DateTimeFormatter.ofPattern()` in try/catch) → (5) parse full PDF with generated config via `RegexStatementParser` → (6) if transactions > 0: cache config in DataStore, return result → (7) if 0 transactions or any step fails: fall back to existing `parseWithGemini()`. Add `UserPreferences` and `RegexValidator` as constructor dependencies in `domain/importstatement/src/main/java/.../domain/importstatement/usecase/ParseStatementUseCase.kt`
- [ ] T008a [US1] Modify `StatementParser.tryParsePdf()` to support multiple configs per bank — change `BankDetector.detect()` call to return `List<ParserConfig>` (all configs matching bankMarkers), iterate each config with `RegexStatementParser.parse()` and return first result with transactions > 0. Update `BankDetector.detect()` to `fun detectAll(text: String, configs: List<ParserConfig>): List<ParserConfig>` (keep old `detect()` as convenience calling `detectAll().firstOrNull()`) in `core/parser/src/main/java/.../core/parser/BankDetector.kt` and `core/parser/src/main/java/.../core/parser/StatementParser.kt`

### Unit Tests

- [ ] T009 [P] [US1] Add unit tests for `extractSampleRows()` — test with PDF text having header+data, test with less than 5 data lines, test with empty text returns empty (triggers skip of AI config generation), test with header-only PDF returns empty in `core/parser/src/test/java/.../core/parser/StatementParserTest.kt`
- [ ] T010 [P] [US1] Add unit tests for `ParseStatementUseCase` — test AI config generation success path (mock Gemini returns valid config, regex parses transactions), test fallback when Gemini fails (network error), test fallback when generated regex is ReDoS-vulnerable, test fallback when generated config parses 0 transactions, test cached config is used on second call in `domain/importstatement/src/test/java/.../domain/importstatement/usecase/ParseStatementUseCaseTest.kt`

**Checkpoint**: US1 is fully functional — import a PDF from unsupported bank → AI generates config → regex parses → preview shown. Testable with `./gradlew :domain:importstatement:test :core:parser:test`

---

## Phase 4: User Story 4 — ReDoS Safety (Priority: P2) — Integration

**Goal**: Ensure ReDoS validation is wired into the full pipeline (already built in Phase 2 and integrated in T008). This phase adds the `withTimeout` runtime guard for defense-in-depth.

**Independent Test**: Import a PDF where Gemini hypothetically returns a vulnerable regex → verify app does not freeze and falls back to full-AI.

- [ ] T011 [US4] Add `withTimeout(5_000)` coroutine guard around `RegexStatementParser.parse()` call in `ParseStatementUseCase.tryRegexThenGemini()` — catches `TimeoutCancellationException` and falls back to full-AI parsing in `domain/importstatement/src/main/java/.../domain/importstatement/usecase/ParseStatementUseCase.kt`
- [ ] T012 [US4] Add unit test for timeout guard — mock `RegexStatementParser.parse()` to delay indefinitely → verify `ParseStatementUseCase` falls back to Gemini without hanging in `domain/importstatement/src/test/java/.../domain/importstatement/usecase/ParseStatementUseCaseTest.kt`

**Checkpoint**: ReDoS safety is complete — both static heuristic and runtime timeout guard active.

---

## Phase 5: User Story 2a — Sample Anonymization (Priority: P2)

**Goal**: Anonymize statement samples before submission to the candidate pool. Standalone utility with comprehensive unit tests.

**Independent Test**: Feed raw statement rows with merchant names, account numbers, amounts, dates → verify merchants replaced with MERCHANT_N, accounts removed, amounts/dates preserved.

- [ ] T013 [P] [US2] Create `SampleAnonymizer` class with `@Inject constructor()` and `fun anonymize(sampleRows: String): String` — (1) detect and replace merchant/recipient name tokens with sequential "MERCHANT_1", "MERCHANT_2" placeholders, (2) remove account/card number patterns (sequences of 4+ digits, `****NNNN` masks), (3) preserve amounts (digit sequences with separators adjacent to currency codes), (4) preserve dates (patterns matching dd.MM.yyyy etc.), (5) preserve currency codes and column whitespace in `core/parser/src/main/java/.../core/parser/SampleAnonymizer.kt`
- [ ] T014 [P] [US2] Create `SampleAnonymizerTest` with test cases: merchant names replaced sequentially, same merchant gets same placeholder, account numbers removed (`****1234`, `KZ123456789`), amounts preserved (`10 000,50`), dates preserved (`01.01.2026`), currency codes preserved (`KZT`, `USD`), empty input returns empty, multi-line input with mixed content in `core/parser/src/test/java/.../core/parser/SampleAnonymizerTest.kt`

**Checkpoint**: Anonymizer is independently testable with `./gradlew :core:parser:test`

---

## Phase 6: User Story 2b — Firestore Candidate Pool (Priority: P2)

**Goal**: Submit validated AI-generated configs to a shared Firestore candidate pool with deduplication. Increment successCount for matching candidates.

**Independent Test**: After successful import with AI config, verify a candidate document appears in `parser_candidates` collection with correct fields. Second import from same bank increments successCount.

### Firestore Layer

- [ ] T015 [P] [US2] Create `ParserCandidateDto` data class with fields: bankId, transactionPattern, parserConfigJson, anonymizedSample, userIdHash, successCount, status, createdAt, updatedAt (all matching Firestore schema from `contracts/firestore-schema.md`) in `core/firestore/src/main/java/.../core/firestore/dto/ParserCandidateDto.kt`
- [ ] T016 [US2] Add methods to `FirestoreDataSource` interface: `suspend fun findParserCandidate(bankId: String, transactionPattern: String): ParserCandidateDto?`, `suspend fun pushParserCandidate(dto: ParserCandidateDto)`, `suspend fun incrementCandidateSuccessCount(candidateId: String)` in `core/firestore/src/main/java/.../core/firestore/datasource/FirestoreDataSource.kt`
- [ ] T017 [US2] Implement the 3 new methods in `FirestoreDataSourceImpl` — `findParserCandidate()` queries `parser_candidates` collection with `whereEqualTo("bankId", bankId).whereEqualTo("transactionPattern", pattern).whereEqualTo("status", "candidate").limit(1)`, `pushParserCandidate()` adds to `parser_candidates` collection, `incrementCandidateSuccessCount()` uses `FieldValue.increment(1)` on successCount and updates updatedAt in `core/firestore/src/main/java/.../core/firestore/datasource/FirestoreDataSourceImpl.kt`

### Domain Layer

- [ ] T018 [US2] Create `SubmitParserCandidateUseCase` — constructor injects `FirestoreDataSource`, `SampleAnonymizer`, `RegexValidator`. Method `suspend operator fun invoke(config: ParserConfig, sampleRows: String, userId: String?)`: (0) if userId is null (unauthenticated user) — return early, skip submission silently, (1) validate regex with `RegexValidator.isReDoSSafe()` — skip submission if unsafe, (2) anonymize sample with `SampleAnonymizer.anonymize()`, (3) hash userId with SHA-256, (4) serialize config to JSON, (5) query Firestore for existing candidate with same bankId + transactionPattern, (6) if found: increment successCount, (7) if not found: create new ParserCandidateDto with successCount=1, status="candidate" in `domain/importstatement/src/main/java/.../domain/importstatement/usecase/SubmitParserCandidateUseCase.kt`

### Presentation Layer — Wire Submission

- [ ] T019 [US2] Add `SubmitParserCandidateUseCase` as dependency to `ImportViewModel`. After `importTransactionsUseCase()` succeeds in `importTransactions()`, if the current import used an AI-generated config: launch coroutine to call `submitParserCandidateUseCase(config, sampleRows, userId)` — fire-and-forget with try/catch (silently skip on failure, log with Timber). Track whether current import used AI-generated config via a flag in `ImportViewModel` in `presentation/importstatement/src/main/java/.../presentation/importstatement/ui/ImportViewModel.kt`

### Unit Tests

- [ ] T020 [P] [US2] Create `SubmitParserCandidateUseCaseTest` — test new candidate creation (no existing match), test successCount increment (existing match found), test skipped when regex is ReDoS-unsafe, test userId is hashed (not raw), test sample is anonymized before submission, test skipped when userId is null (unauthenticated user) in `domain/importstatement/src/test/java/.../domain/importstatement/usecase/SubmitParserCandidateUseCaseTest.kt`

### ImportViewModel Tests (Constitution VII)

- [ ] T020a [US2] Add unit tests for `ImportViewModel` candidate submission flow — test: (1) after successful import with AI-generated config, `submitParserCandidateUseCase` is called, (2) after successful import with regex config (non-AI), submission is NOT called, (3) submission failure does not affect import success state, (4) initial state is Idle. Use `MainDispatcherRule`, `runTest {}`, MockK for dependencies in `presentation/importstatement/src/test/java/.../presentation/importstatement/ui/ImportViewModelTest.kt`

### Firestore Security Rules

- [ ] T021 [US2] Update Firestore security rules to add `parser_candidates` collection rules per `contracts/firestore-schema.md` — authenticated create (successCount==1, status=="candidate"), authenticated update (only successCount+updatedAt, increment by 1), authenticated read in `firestore.rules`

**Checkpoint**: US2 is complete — after import with AI config, anonymized candidate is submitted to Firestore. Dedup works. Testable with `./gradlew :domain:importstatement:test` and Firestore emulator.

---

## Phase 7: User Story 3 — Automatic Config Promotion (Priority: P3)

**Goal**: Cloud Function auto-promotes candidates to Remote Config when successCount reaches threshold. All users receive new bank support without app update.

**Independent Test**: Manually set a candidate's successCount to 3 in Firestore emulator → verify Cloud Function adds the config to Remote Config `parser_configs` key.

### Cloud Function (TypeScript)

- [ ] T022 [P] [US3] Initialize Cloud Function project — create `functions/` directory with `package.json` (firebase-admin, firebase-functions v2), `tsconfig.json`, `.eslintrc.js` per `contracts/cloud-function-api.md` in `functions/`
- [ ] T023 [US3] Implement `onParserCandidateUpdated` Cloud Function in `functions/src/promote-candidate.ts` — Firestore trigger on `parser_candidates/{candidateId}` document update: (1) read successCount and status from updated doc, (2) exit if status != "candidate", (3) read promotion_threshold from Remote Config (default 3), (4) exit if successCount < threshold, (5) read current `parser_configs` from Remote Config Admin API, (6) deserialize to ParserConfigList, (7) check bankId+transactionPattern not already in list → if exists: set status="rejected", exit, (8) deserialize parserConfigJson from candidate, (9) append to banks array, (10) publish updated JSON to Remote Config, (11) set status="promoted" on candidate doc, (12) log promotion
- [ ] T024 [US3] Create `functions/src/index.ts` — export `onParserCandidateUpdated` and optionally `onParserCandidateCreated` (same logic for threshold==1 edge case) from `functions/src/index.ts`

**Checkpoint**: US3 is complete — Cloud Function promotes candidates at threshold, all users receive new bank config via existing Remote Config `getConfigs()` flow (no Android changes needed — promoted configs are appended to the same `parser_configs` key). Testable with Firebase emulator suite.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, edge cases, and cleanup

- [ ] T026 [P] Verify end-to-end flow: import PDF from unsupported bank → AI generates config → regex parses → preview → import → candidate submitted to Firestore → successCount incremented on repeat → Cloud Function promotes at threshold
- [ ] T027 [P] Add Timber logging at key decision points: AI config generation attempt, ReDoS validation result, cache hit/miss, candidate submission success/failure, fallback triggers in all modified files
- [ ] T028 Run `./gradlew assembleDebug test lint detekt` and fix any issues

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (ReDoS Validator)**: Depends on Phase 1 — standalone utility, no other dependencies
- **Phase 3 (US1 - MVP)**: Depends on Phase 2 (uses RegexValidator)
- **Phase 4 (US4 - Integration)**: Depends on Phase 3 (adds timeout to existing flow)
- **Phase 5 (US2a - Anonymization)**: Depends on Phase 1 only — can run in parallel with Phase 3
- **Phase 6 (US2b - Firestore)**: Depends on Phase 3 (needs AI config flow) + Phase 5 (needs anonymizer)
- **Phase 7 (US3 - Cloud Function)**: Depends on Phase 6 (needs candidates in Firestore) — TypeScript, independent of Android build
- **Phase 8 (Polish)**: Depends on all previous phases

### Parallel Opportunities

```
Phase 1 (Setup)
    ↓
Phase 2 (ReDoS) ←──────── Phase 5 (Anonymizer) [PARALLEL]
    ↓                         ↓
Phase 3 (US1 - MVP)          │
    ↓                         │
Phase 4 (US4 - Timeout)      │
    ↓                         ↓
Phase 6 (US2 - Firestore) ←──┘
    ↓
Phase 7 (US3 - Cloud Function)
    ↓
Phase 8 (Polish)
```

### Within Each Phase

- Tasks marked [P] can run in parallel
- T002 and T003 can run in parallel (different files)
- T004, T005, T007 can run in parallel (different modules)
- T009, T010 can run in parallel (different test files)
- T013, T014 can run in parallel (source + test)
- T015, T020 can run in parallel (DTO + test)

---

## Implementation Strategy

### MVP First (Phase 1–3 = US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: ReDoS Validator (T002–T003)
3. Complete Phase 3: US1 — AI Config Generation (T004–T010)
4. **STOP and VALIDATE**: Import a PDF from unsupported bank → verify parsed preview
5. This is a fully functional MVP — user can import from any bank

### Incremental Delivery

1. Phase 1–3 → MVP: import from any bank via AI-generated config
2. Phase 4 → Security hardening: runtime timeout guard
3. Phase 5 → Anonymization utility (independently testable)
4. Phase 6 → Crowdsourcing: candidates submitted to Firestore
5. Phase 7 → Automation: Cloud Function promotes popular configs
6. Phase 8 → Polish: logging, lint, end-to-end verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Cloud Function (Phase 7) is TypeScript — completely independent of Android build
- No new Gradle modules — all Android tasks extend existing modules
- ParserConfig model is unchanged — AI generates the same schema
- Commit after each phase checkpoint
