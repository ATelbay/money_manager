# Tasks: Table-Based PDF Parsing with Simplified AI Config

**Input**: Design documents from `/specs/014-table-pdf-parsing/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md

**Tests**: Included — constitution mandates testing (Principle VII), and feature description explicitly specifies test requirements.

**Organization**: Tasks grouped by user story. US1 is the MVP.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Build Configuration)

**Purpose**: Build changes required before any implementation

- [ ] T001 [P] Add kotlinx-serialization plugin and dependency to `core/model/build.gradle.kts` — add `alias(libs.plugins.kotlin.serialization)` to plugins, `implementation(libs.kotlinx.serialization.json)` to dependencies
- [ ] T002 [P] Add `implementation(projects.core.model)` dependency to `core/ai/build.gradle.kts`

**Checkpoint**: `./gradlew assembleDebug` passes with new build dependencies

---

## Phase 2: Foundational (Shared Models & Utilities)

**Purpose**: Core infrastructure that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 [P] Create `TableParserConfig` and `TableParserConfigList` data classes in `core/model/src/main/java/com/atelbay/money_manager/core/model/TableParserConfig.kt` — `@Serializable` with `@SerialName` annotations for all 13 fields per data-model.md. Include `TableParserConfigList(val configs: List<TableParserConfig>)` wrapper
- [ ] T004 [P] Extract shared `AmountParser` object in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/AmountParser.kt` — move `parseAmount(amountStr: String, format: String): Double` from `RegexStatementParser`, add explicit `"dot"` format handling (strip non-numeric except `.` and `-`), add `parseDateString(dateStr: String, dateFormat: String): LocalDate` using `DateTimeFormatter.ofPattern(dateFormat)`, update `RegexStatementParser` to delegate to `AmountParser.parseAmount()`
- [ ] T005 [P] Make `PdfTextExtractor.ensureInitialized()` `internal` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/PdfTextExtractor.kt` — change from `private` to `internal` so `PdfTableExtractor` can reuse PDFBox initialization

**Checkpoint**: Foundation ready — all models and utilities available for user story implementation

---

## Phase 3: User Story 1 - Import bank statement from table-structured PDF (Priority: P1) MVP

**Goal**: Extract table structure from PDFs, generate column-index config via AI, parse transactions

**Independent Test**: Import a table-structured bank statement PDF → verify transactions extracted with correct dates, amounts, descriptions

### Implementation for User Story 1

- [ ] T006 [P] [US1] Create `PdfTableExtractor` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/PdfTableExtractor.kt` — `@Singleton` class with `@Inject constructor(@ApplicationContext context: Context)`. Inner class extends `PDFTextStripper`, overrides `writeString(String, List<TextPosition>)` to collect X/Y coordinates. Algorithm: group by Y (tolerance ~2pt) for rows, sorted-gap detection on X positions (threshold ~10pt) for columns, concatenate characters per cell. Public API: `fun extractTable(bytes: ByteArray): List<List<String>>` (empty on failure), `fun extractTableOrNull(bytes: ByteArray): List<List<String>>?` (null if <2 rows). Call `PdfTextExtractor.ensureInitialized()` before processing. Process page-by-page to handle Y-offset differences across pages
- [ ] T007 [P] [US1] Create `TableStatementParser` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/TableStatementParser.kt` — `@Inject constructor()`, no deps. `fun parse(table: List<List<String>>, config: TableParserConfig): List<ParsedTransaction>`. Skip first `config.skipHeaderRows` rows. For each row: extract date/amount/sign/operation/details by column index using `AmountParser.parseAmount()`. Use `AmountParser` for date parsing too (extract `parseDateString(dateStr, dateFormat): LocalDate`). Generate `uniqueHash` via `generateTransactionHash()` from core:common. Determine transaction type: `negativeSignMeansExpense` → check sign/amount sign, else check `signColumn`. Apply `deduplicateByMaxAmount` if configured (reuse logic pattern from `RegexStatementParser`). Handle out-of-bounds column indices gracefully (skip row, log via Timber)
- [ ] T008 [P] [US1] Add `TableFailedAttempt` data class and `generateTableParserConfig` method to `GeminiService` interface in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiService.kt` — `data class TableFailedAttempt(val config: TableParserConfig, val error: String, val failedRows: List<String> = emptyList())`. New method: `suspend fun generateTableParserConfig(headerSnippet: String, sampleTableRows: List<List<String>>, previousAttempts: List<TableFailedAttempt> = emptyList()): TableParserConfig`
- [ ] T009 [US1] Implement `generateTableParserConfig` in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt` — add `tableParserConfigSchema` (Schema.obj with integer fields for column indices, string enums for formats), `buildTableParserConfigPrompt()` (show rows as JSON array of arrays, ask for column indices + date_format + amount_format + negative_sign_means_expense; if previousAttempts non-empty show failed config + error + failed rows), `parseTableParserConfigResponse()` (deserialize JSON to TableParserConfig), new `tableConfigModel()` with responseSchema. Prompt should be dramatically simpler than regex prompt — no regex rules, no ReDoS, no named-group syntax
- [ ] T010 [US1] Add `TableParseResult` data class and table methods to `StatementParser` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/StatementParser.kt` — add `PdfTableExtractor` and `TableStatementParser` as constructor deps. New `data class TableParseResult(val transactions: List<ParsedTransaction>, val bankId: String?, val extractedTable: List<List<String>> = emptyList())`. New methods: `fun tryParseTable(bytes: ByteArray, tableConfigs: List<TableParserConfig>): TableParseResult?` (extract table, detect bank via markers in extracted text, try matching configs), `fun tryParseWithTableConfig(bytes: ByteArray, config: TableParserConfig): TableParseResult` (extract + parse with specific config), `fun extractSampleTableRows(bytes: ByteArray): List<List<String>>` (extract table, return first 10 data rows for AI prompt)
- [ ] T011 [US1] Add `TABLE_GENERATED` to `AiMethod` enum and update `ParseResult` in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — add `TABLE_GENERATED` to AiMethod enum. Add `val aiGeneratedTableConfig: TableParserConfig? = null` and `val sampleTableRows: List<List<String>>? = null` to ParseResult data class
- [ ] T012 [US1] Implement table AI generation path in `ParseStatementUseCase` in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — in `tryRegexThenGemini()`, after step 2 (cached AI regex configs) and before step 3 (AI regex generation), insert: (a) extract table via `statementParser.extractSampleTableRows(pdfBytes)`, (b) if table has >=2 rows, loop `MAX_AI_RETRIES = 3`: call `geminiService.generateTableParserConfig()`, validate dateFormat, try `statementParser.tryParseWithTableConfig()` with timeout, on success return `AiMethod.TABLE_GENERATED`. On failure: build `TableFailedAttempt` with error + failed rows for retry. If all retries fail: fall through to existing AI regex generation (step 3)

### Tests for User Story 1

- [ ] T013 [P] [US1] Create `PdfTableExtractorTest` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/PdfTableExtractorTest.kt` — use JVM PdfBox (`libs.pdfbox`, already in test deps) to create test PDFs programmatically. Tests: simple 4-column table, multi-page table, PDF with no table structure → empty result, empty PDF → empty result, table with varying column widths
- [ ] T014 [P] [US1] Create `TableStatementParserTest` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/TableStatementParserTest.kt` — handcrafted `List<List<String>>` inputs. Tests: 4-column table (Forte-like), 7-column table (Bereke-like), all 3 amount formats (dot/comma_dot/space_comma), sign column detection, header row skipping (skipHeaderRows=0, 1, 2), deduplicateMaxAmount=true, out-of-bounds column index → row skipped, empty table → empty result
- [ ] T015 [P] [US1] Add table config tests to `GeminiServiceImplTest` in `core/ai/src/test/java/com/atelbay/money_manager/core/ai/GeminiServiceImplTest.kt` — test prompt construction with sample table data (verify JSON array format, column index questions), test `parseTableParserConfigResponse()` with valid JSON → correct TableParserConfig, test retry prompt includes failed config + error + failed rows
- [ ] T016 [US1] Add table path tests to `ParseStatementUseCaseTest` in `domain/import/src/test/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCaseTest.kt` — mock `statementParser.extractSampleTableRows()` to return table data, mock `geminiService.generateTableParserConfig()`, test: table AI generation happy path → TABLE_GENERATED, first attempt fails + retry succeeds, all table attempts fail → falls through to AI regex path

**Checkpoint**: US1 complete — table extraction + AI config generation + pipeline integration working. Can import table-structured PDFs from new banks.

---

## Phase 4: User Story 2 - Cached table configs speed up repeat imports (Priority: P2)

**Goal**: Cache AI-generated table configs in DataStore, reuse on subsequent imports from same bank

**Independent Test**: Import same bank PDF twice → second import uses cached config without AI call

### Implementation for User Story 2

- [ ] T017 [P] [US2] Add `KEY_AI_TABLE_PARSER_CONFIGS` to `UserPreferences` in `core/datastore/src/main/java/com/atelbay/money_manager/core/datastore/UserPreferences.kt` — new `stringPreferencesKey("ai_table_parser_configs")`, add `val cachedAiTableParserConfigs: Flow<String?>` and `suspend fun setCachedAiTableParserConfigs(json: String)` and `suspend fun clearCachedAiTableParserConfigs()`, following exact pattern of existing `KEY_AI_PARSER_CONFIGS`
- [ ] T018 [US2] Implement table config caching in `ParseStatementUseCase` in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — add `loadCachedTableConfigs(): List<TableParserConfig>` (read from DataStore, deserialize `TableParserConfigList`), add `cacheTableConfig(config: TableParserConfig)` (replace by bankId, append new, serialize back). In the table path: before AI generation (step 2.5a), try `statementParser.tryParseTable(pdfBytes, cachedTableConfigs)` — if success, return immediately with `AiMethod.TABLE_GENERATED`. After AI generation succeeds: call `cacheTableConfig(generatedConfig)`

### Tests for User Story 2

- [ ] T019 [US2] Add caching tests to `ParseStatementUseCaseTest` in `domain/import/src/test/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCaseTest.kt` — test: cached table config hit → no AI call, cached table config miss → falls through to AI generation, successful AI generation → config cached, cached config with same bankId → replaced (not duplicated)

**Checkpoint**: US2 complete — repeat imports skip AI calls, table configs persist across sessions.

---

## Phase 5: User Story 3 - Graceful fallback to existing paths (Priority: P2)

**Goal**: Ensure non-table PDFs and failed table paths fall through to existing regex AI and full AI parse

**Independent Test**: Import a non-table PDF → verify it falls through to regex AI or full AI parse without errors

### Implementation for User Story 3

- [ ] T020 [US3] Ensure fallback logic in `ParseStatementUseCase` in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — verify: if `extractSampleTableRows()` returns <2 rows → skip entire table path (steps 2.5a + 2.5b), proceed directly to AI regex generation (step 3). If all 3 table config retries fail → proceed to AI regex generation. If AI regex also fails → proceed to full AI parse (existing behavior). Add Timber logging at each fallback transition. Ensure no exceptions from table path propagate and break the pipeline (wrap in try-catch)

### Tests for User Story 3

- [ ] T021 [US3] Add fallback tests to `ParseStatementUseCaseTest` in `domain/import/src/test/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCaseTest.kt` — test: table extraction returns empty → AI regex path runs, table extraction returns 1 row → AI regex path runs, all table config retries fail → AI regex path runs, table path throws exception → caught and falls through to AI regex, existing regex configs still tried first (step 1-2 before table path)

**Checkpoint**: US3 complete — backward compatibility verified, all existing import flows unaffected.

---

## Phase 6: User Story 4 - Debug visibility for table parsing steps (Priority: P3)

**Goal**: Add progress events for table extraction and config generation, display in debug import sheet

**Independent Test**: Enable debug sheet during import → verify table-related events appear in event log

### Implementation for User Story 4

- [ ] T022 [P] [US4] Add table-related events to `ImportStepEvent` in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ImportStepEvent.kt` — add: `data class TableExtracted(val rowCount: Int, val columnCount: Int)`, `data class TableConfigAttempt(val source: String, val bankId: String? = null)`, `data class TableConfigResult(val source: String, val txCount: Int)`, `data class AiTableConfigRequest(val attempt: Int)`, `data class AiTableConfigResponse(val attempt: Int, val bankId: String)`, `data class AiTableConfigParseResult(val attempt: Int, val txCount: Int)` — all extending `ImportStepEvent()`
- [ ] T023 [US4] Emit table events in `ParseStatementUseCase` pipeline in `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — emit `TableExtracted` after table extraction, `TableConfigAttempt(source="cached_table")` before cached config attempt, `TableConfigResult` after cached config parse, `AiTableConfigRequest(attempt=N)` before each AI call, `AiTableConfigResponse` after AI returns, `AiTableConfigParseResult` after parsing AI-generated config. Use existing `collector.emit()` pattern
- [ ] T024 [US4] Add table event display in `DebugImportSheet` in `presentation/import/src/main/java/com/atelbay/money_manager/presentation/importstatement/ui/debug/DebugImportSheet.kt` — extend `toDisplayInfo()` to handle all 6 new `ImportStepEvent` variants. Use existing icon/color scheme: `InfoGray` for extraction/attempts, `SuccessGreen` for successful results, `WarningAmber` for retries. Show row/column counts, bank IDs, attempt numbers, transaction counts in detail text

**Checkpoint**: US4 complete — all table parsing steps visible in debug sheet.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Build verification and cleanup

- [ ] T025 Run `./gradlew assembleDebug` and fix any compilation errors across all modified modules
- [ ] T026 Run `./gradlew :core:parser:test :core:ai:test :domain:import:test` and fix any test failures
- [ ] T027 Run `./gradlew lint detekt` and fix any lint/detekt warnings in new/modified files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001, T002) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (T003, T004, T005) — core MVP
- **US2 (Phase 4)**: Depends on US1 (needs table path in pipeline to add caching around)
- **US3 (Phase 5)**: Depends on US1 (needs table path to verify fallback behavior)
- **US4 (Phase 6)**: Depends on US1 (needs table path to emit events from)
- **Polish (Phase 7)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — no dependencies on other stories
- **US2 (P2)**: Depends on US1 — adds caching around the table path built in US1
- **US3 (P2)**: Depends on US1 — validates fallback behavior of the pipeline built in US1
- **US4 (P3)**: Depends on US1 — adds observability events to the pipeline built in US1
- **US2, US3, US4**: Can run in parallel AFTER US1 completes (they modify different aspects of ParseStatementUseCase but different sections)

### Within Each User Story

- Models/utilities before services
- Services before pipeline integration
- Pipeline integration before tests (tests verify the integrated behavior)
- Core implementation before UI (debug sheet)

### Parallel Opportunities

**Phase 1**: T001, T002 — parallel (different build files)
**Phase 2**: T003, T004, T005 — parallel (different files in different modules)
**Phase 3 (US1)**: T006, T007, T008 — parallel (PdfTableExtractor, TableStatementParser, GeminiService interface — all different files). T013, T014, T015 — parallel (test files for different classes)
**After US1**: US2, US3, US4 can proceed in parallel

---

## Parallel Example: User Story 1

```bash
# Launch foundational tasks together (Phase 2):
Task: "Create TableParserConfig in core/model/.../TableParserConfig.kt"
Task: "Extract AmountParser in core/parser/.../AmountParser.kt"
Task: "Make ensureInitialized() internal in PdfTextExtractor"

# Launch core implementations together (after foundational):
Task: "Create PdfTableExtractor in core/parser/.../PdfTableExtractor.kt"
Task: "Create TableStatementParser in core/parser/.../TableStatementParser.kt"
Task: "Add generateTableParserConfig to GeminiService interface"

# Launch tests together (after implementation):
Task: "Create PdfTableExtractorTest"
Task: "Create TableStatementParserTest"
Task: "Add table config tests to GeminiServiceImplTest"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (build changes)
2. Complete Phase 2: Foundational (model + utilities)
3. Complete Phase 3: User Story 1 (table extraction + AI config + pipeline)
4. **STOP and VALIDATE**: Import a table-structured PDF from a new bank — verify transactions extracted
5. Run `./gradlew assembleDebug test` — all green

### Incremental Delivery

1. Setup + Foundational → Build compiles
2. US1 → Table extraction + AI config generation working (MVP!)
3. US2 → Cached configs eliminate repeat AI calls
4. US3 → Fallback behavior verified for non-table PDFs
5. US4 → Debug visibility for all table steps
6. Polish → Lint clean, all tests passing

### Note on US2/US3/US4 Parallelization

US2, US3, and US4 all modify `ParseStatementUseCase.kt`. While they touch different sections of the file:
- US2: adds `loadCachedTableConfigs()` + `cacheTableConfig()` + cached config attempt step
- US3: adds try-catch wrapping + fallback logging
- US4: adds `collector.emit()` calls throughout the table path

For serial execution (single developer), implement in order: US2 → US3 → US4.
For parallel execution (agent teams), each agent works in a worktree and changes are merged sequentially.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- All tests use JUnit 4 + MockK (per constitution Principle VII)
- PdfTableExtractor tests use JVM PdfBox (`libs.pdfbox`) to create test PDFs programmatically
- TableStatementParser tests use handcrafted `List<List<String>>` — no PDF needed
- Commit after each phase checkpoint
- `./gradlew assembleDebug` should pass after each phase
