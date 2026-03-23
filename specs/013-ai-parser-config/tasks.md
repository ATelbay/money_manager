# Tasks: Improve AI Parser Config Generation

**Input**: Design documents from `/specs/013-ai-parser-config/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Phase 5 — unit tests for GeminiServiceImpl and ParseStatementUseCase (Constitution VII).

**Organization**: Tasks grouped by user story. US1 and US4 are merged because both require rewriting the same prompt function — writing it in Russian first then rewriting in English would be wasted effort.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add FailedAttempt data class, update interface signature, increase sample data

- [ ] T001 Add `FailedAttempt` data class and update `generateParserConfig()` signature in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiService.kt`. Add `data class FailedAttempt(val config: ParserConfig, val error: String)` as a top-level class. Update `generateParserConfig()` to accept two new parameters: `existingConfigs: List<ParserConfig> = emptyList()` and `previousAttempts: List<FailedAttempt> = emptyList()`. Use default values so existing callers don't break.
- [ ] T002 [P] Increase `SAMPLE_LINE_COUNT` from `10` to `60` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/StatementParser.kt`. Change only the constant value on line 24.

**Checkpoint**: Interface contract updated, sample data increased. Implementation can begin.

---

## Phase 2: User Story 1 + User Story 4 - Enhanced Prompt with Few-Shot Examples in English (Priority: P1 + P3)

**Goal**: Rewrite `buildParserConfigPrompt()` in English (US4) while adding few-shot examples and failed attempt sections (US1). These are merged because both modify the same function.

**Independent Test**: Call `generateParserConfig()` with existing configs and verify the prompt contains English instructions, JSON examples of working configs, and (if provided) previous failure context.

### Implementation

- [ ] T003 [US1] Update `generateParserConfig()` implementation signature in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`. Add `existingConfigs: List<ParserConfig>` and `previousAttempts: List<FailedAttempt>` parameters to match the interface. Pass them to `buildParserConfigPrompt()`.
- [ ] T004 [US1] Add `selectExamplesForPrompt(configs: List<ParserConfig>): List<ParserConfig>` private helper in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`. If ≤3 configs, return all. If >3, select up to 3 with diverse `amountFormat` values and boolean flag combinations (`useSignForType`, `negativeSignMeansExpense`, `useNamedGroups`). See research.md R3 for algorithm details.
- [ ] T005 [US1] Rewrite `buildParserConfigPrompt()` in English in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`. The new prompt must: (1) be entirely in English (satisfies US4), (2) preserve all structural rules from the Russian prompt (bank_id naming, named group syntax `(?<name>...)` not `(?P<name>...)`, amount_format options, injection guard with `<DATA>` blocks), (3) add a "## Working examples" section that renders selected configs as JSON blocks using `kotlinx.serialization` to serialize each `ParserConfig`, (4) add a "## Previous failed attempts" section that renders each `FailedAttempt` (config JSON + error string) — omit section if list is empty, (5) add instruction to prefer `use_sign_for_type=true` or `negative_sign_means_expense=true` over `operation_type_map` when the bank statement uses +/- signs for transaction direction, (6) update `buildParserConfigPrompt()` signature to accept `existingConfigs: List<ParserConfig>` and `previousAttempts: List<FailedAttempt>`, (7) instruct that `operation_type_map` is an array of `{"key": "...", "value": "income"|"expense"}` objects (matching the schema change in T006).

**Checkpoint**: Prompt is now in English with few-shot examples and failure context. US1 and US4 are complete at the GeminiServiceImpl level. Retry loop (US2) and schema fix (US3) still needed.

---

## Phase 3: User Story 3 - Fix operation_type_map Double-Encoding (Priority: P2)

**Goal**: Change the AI response schema so `operation_type_map` is a structured array instead of a string, eliminating double-encoding.

**Independent Test**: Verify that `parseParserConfigResponse()` correctly reads `operation_type_map` from a JSON array of `{"key":"...","value":"..."}` objects and produces the same `Map<String, String>` result.

### Implementation

- [ ] T006 [US3] Update `parserConfigSchema` in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`. Replace `"operation_type_map" to Schema.string()` with `"operation_type_map" to Schema.array(Schema.obj(properties = mapOf("key" to Schema.string(), "value" to Schema.enumeration(listOf("income", "expense")))))`. See research.md R1 for the exact schema definition.
- [ ] T007 [US3] Update `parseParserConfigResponse()` in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`. Replace the double-decoding logic (lines ~165-176) that reads `operation_type_map` as a `jsonPrimitive.content` string and re-parses it. Instead, read it as `jsonObj["operation_type_map"]?.jsonArray.orEmpty()` and convert with `.associate { entry -> val obj = entry.jsonObject; obj["key"]!!.jsonPrimitive.content to obj["value"]!!.jsonPrimitive.content }`. Keep the `try/catch` with `emptyMap()` fallback for robustness.

**Checkpoint**: operation_type_map is now parsed correctly from structured JSON. No more double-encoding issues.

---

## Phase 4: User Story 2 - Retry Loop with Failure Tracing (Priority: P1)

**Goal**: Wrap AI config generation + validation + parsing in a 3-attempt retry loop. Each failure creates a `FailedAttempt` with a specific error description, passed to the next attempt.

**Independent Test**: Trigger a scenario where the first AI config fails validation. Verify the system retries with failure context and that the retry prompt includes the failed config and error string.

### Implementation

- [ ] T008 [US2] Implement retry loop in `tryRegexThenGemini()` in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt`. Refactor steps 3-5 (AI generation, validation, parse) into a `repeat(MAX_AI_RETRIES)` loop where `MAX_AI_RETRIES = 3`. Before the loop, collect existing configs: `val allConfigs = parserConfigProvider.getConfigs() + loadCachedAiConfigs()`. Create `val failedAttempts = mutableListOf<FailedAttempt>()`. On each iteration: (1) call `geminiService.generateParserConfig(headerSnippet, extractedSampleRows, allConfigs, failedAttempts)` inside try/catch — on exception, add `FailedAttempt(config = ParserConfig(...empty...), error = "AI generation failed: ${e.message}")` and `continue`, (2) validate ReDoS → on fail add `FailedAttempt(config, "Regex failed ReDoS safety check")` and `continue`, (3) validate regex syntax → on fail add `FailedAttempt(config, "Regex syntax invalid: ${e.message}")` and `continue`, (4) validate dateFormat → on fail add `FailedAttempt(config, "DateFormat invalid: ${e.message}")` and `continue`, (5) parse with timeout → on `TimeoutCancellationException` add `FailedAttempt(config, "Regex timed out (possible catastrophic backtracking)")` and `continue`, (6) if parsed 0 transactions, extract sample lines and add `FailedAttempt(config, "Regex matched 0 transaction lines out of $totalLines total lines. Sample lines that should have matched:\n$sampleLines")` and `continue`, (7) on success: cache config and return. After loop exhaustion, fall through to full-AI parsing (existing behavior).
- [ ] T009 [US2] Add sample line extraction helper for 0-match diagnostics in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt`. When the generated regex matches 0 lines, extract 3-5 sample non-blank lines from the PDF text (after skipping header): `pdfText.lines().drop(HEADER_LINE_COUNT).filter { it.isNotBlank() }.take(5).joinToString("\n")`. Include these in the `FailedAttempt` error string. Note: `HEADER_LINE_COUNT` (10) is a constant in `StatementParser` — use the same value (10) as a private constant in `ParseStatementUseCase` or reference it. Count total non-blank lines for the "out of N" part.

**Checkpoint**: Retry loop is complete. All 4 user stories are implemented. System retries up to 3 times with specific error context before falling through to full-AI parsing.

---

## Phase 5: Unit Tests (Constitution VII)

**Purpose**: Unit tests for new/modified logic. Constitution VII mandates happy-path and error-case tests for every UseCase and testable service method.

- [ ] T010 [P] Write `core/ai/src/test/java/com/atelbay/money_manager/core/ai/GeminiServiceImplTest.kt`. Test cases: (1) `buildParserConfigPrompt()` with 0 existing configs → prompt has no "Working examples" section, (2) with 2 existing configs → prompt contains JSON examples, (3) with 5 configs → only 3 selected (diversity), (4) with 1 failed attempt → prompt contains "Previous failed attempts" section with error string, (5) `parseParserConfigResponse()` reads `operation_type_map` from JSON array of `{"key":"...","value":"..."}` objects → produces correct `Map<String,String>`, (6) `parseParserConfigResponse()` with empty/missing `operation_type_map` → returns `emptyMap()`. Use MockK to mock `GenerativeModel`. Stack: JUnit 4, MockK, kotlinx-coroutines-test.
- [ ] T011 [P] Write `domain/import/src/test/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCaseTest.kt` (or extend if exists). Test cases: (1) AI generates valid config on 1st attempt → no retry, config cached and used, (2) 1st attempt returns invalid regex, 2nd attempt succeeds → `FailedAttempt` with "Regex syntax invalid" passed to 2nd call, (3) 1st attempt returns 0-match config, 2nd attempt succeeds → error includes sample lines, (4) all 3 attempts fail → falls through to full-AI parsing, (5) network exception on 1st attempt → counts as failed attempt with "AI generation failed" error, loop continues. Mock `GeminiService`, `RegexStatementParser`, `ParserConfigProvider`. Stack: JUnit 4, MockK, Turbine, kotlinx-coroutines-test, `runTest {}`.

---

## Phase 6: Build Verification

**Purpose**: Build and regression check

- [ ] T012 Build the project with `./gradlew assembleDebug` to verify no compilation errors across all modified modules (core:ai, core:parser, domain:import)
- [ ] T013 Run all tests with `./gradlew :core:ai:test :core:parser:test :domain:import:test` to verify no regressions and new tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately. T001 and T002 can run in parallel.
- **Phase 2 (US1+US4)**: Depends on T001 (interface change). T003→T004→T005 are sequential (same file, dependent logic).
- **Phase 3 (US3)**: Depends on T001 (interface change). Can run in parallel with Phase 2 since T006/T007 modify different parts of GeminiServiceImpl than T003-T005.
- **Phase 4 (US2)**: Depends on T001 (FailedAttempt), T005 (prompt accepts previousAttempts), and ideally T006-T007 (schema fix). T008→T009 are sequential.
- **Phase 5 (Tests)**: Depends on all implementation phases (1-4). T010 and T011 can run in parallel (different modules).
- **Phase 6 (Build Verification)**: Depends on all previous phases.

### User Story Dependencies

- **US1 + US4 (Phase 2)**: Depends on Phase 1 only. No dependencies on other stories.
- **US3 (Phase 3)**: Depends on Phase 1 only. Can run in parallel with US1+US4.
- **US2 (Phase 4)**: Depends on Phases 1, 2, and 3 (needs the updated interface, prompt, and schema).

### Parallel Opportunities

- **T001 ∥ T002**: Interface change and sample count change are in different files.
- **Phase 2 ∥ Phase 3**: US1+US4 (prompt rewrite) and US3 (schema fix) modify different sections of GeminiServiceImpl and can be developed in parallel, then merged. **Merge note**: T005 instructs the AI that `operation_type_map` is an array of `{"key":"...","value":"..."}` objects — this MUST match T006's schema definition exactly. Both tasks share the same contract.

---

## Parallel Example: Phase 1

```bash
# Launch both setup tasks together (different files):
Task: "T001 — Add FailedAttempt + update interface in GeminiService.kt"
Task: "T002 — Increase SAMPLE_LINE_COUNT in StatementParser.kt"
```

## Parallel Example: Phase 2 ∥ Phase 3

```bash
# After Phase 1, launch US1+US4 and US3 in parallel:
Task: "T003-T005 — Prompt rewrite with few-shot in GeminiServiceImpl.kt"
Task: "T006-T007 — Schema + parser fix for operation_type_map in GeminiServiceImpl.kt"
```

---

## Implementation Strategy

### MVP First (US1 + US4 only)

1. Complete Phase 1: Setup (T001, T002)
2. Complete Phase 2: US1+US4 (T003-T005)
3. **STOP and VALIDATE**: Build succeeds, prompt is in English with few-shot examples
4. This alone provides significant improvement even without retries or schema fix

### Full Delivery

1. Phase 1: Setup → T001 ∥ T002
2. Phase 2 ∥ Phase 3: US1+US4 and US3 in parallel
3. Phase 4: US2 (retry loop) — depends on Phases 2+3
4. Phase 5: Build verification

---

## Notes

- All changes are in existing files — no new files or modules needed
- T005 (prompt rewrite) is the largest task — it involves rewriting ~40 lines of Russian prompt text to English while adding 2 new sections (examples + failures)
- T008 (retry loop) is the most complex task — refactoring a linear flow into a loop with error accumulation
- ParserConfig class is frozen — do NOT modify `core/remoteconfig/.../ParserConfig.kt`
- When generating FailedAttempt for AI generation exceptions, use a minimal empty ParserConfig since no config was produced
