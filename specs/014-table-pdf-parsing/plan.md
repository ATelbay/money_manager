# Implementation Plan: Table-Based PDF Parsing with Simplified AI Config

**Branch**: `014-table-pdf-parsing` | **Date**: 2026-03-23 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-table-pdf-parsing/spec.md`

## Summary

Replace the failing AI regex generation step with a table-structure extraction pipeline using PdfBox-Android's TextPosition API. Instead of asking Gemini to write complex regex patterns, extract the table grid from PDFs and ask the AI simple questions: "which column is date? amount? operation?" The new table path slots between cached regex configs (step 2) and AI regex generation (step 4) in the existing pipeline, with full fallback to existing paths.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: PdfBox-Android 2.0.27.0 (TextPosition API), Firebase AI (Gemini 2.5 Flash), kotlinx-serialization-json, Hilt 2.58
**Storage**: Preferences DataStore (cached table configs), Room (unchanged)
**Testing**: JUnit 4, MockK, Turbine, kotlinx-coroutines-test; JVM PdfBox for test PDF generation
**Target Platform**: Android (minSdk per project)
**Project Type**: Mobile app (Android)
**Performance Goals**: Table extraction completes within 5s for typical bank statements (10-200 rows)
**Constraints**: No new dependencies; PdfBox-Android TextPosition API only; no Tabula (java.awt incompatible)
**Scale/Scope**: 5 supported banks, typical PDFs have 10-200 transaction rows, 4-7 columns

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | Changes span core/model, core/parser, core/ai, domain/import, presentation/import — all within existing module boundaries. No new modules needed (not a new "feature" requiring 3 modules — this extends the existing import feature). |
| II. Kotlin-First & Jetpack Compose | PASS | All new code in Kotlin. Debug UI changes in Compose. |
| III. Material 3 Design System | PASS | Debug sheet already uses Material 3 components. |
| IV. Animation & Motion | N/A | No animation changes. |
| V. Hilt DI | PASS | PdfTableExtractor: @Singleton @Inject. TableStatementParser: @Inject. All wired via Hilt constructor injection. |
| VI. Room Database | N/A | No schema changes. |
| VII. Testing Architecture | PASS | Unit tests for PdfTableExtractor, TableStatementParser, GeminiService table methods, ParseStatementUseCase table path. MockK for mocking. |
| VIII. Firebase Ecosystem | PASS | Extends existing GeminiService with new method. No new Firebase services. |
| IX. Type-Safe Navigation | N/A | No navigation changes. |
| X. Statement Import Pipeline | PASS | Extends pipeline with new step between steps 2 and 3. Preserves all existing steps. Adds TABLE_GENERATED to AiMethod. |
| XI. Preferences DataStore | PASS | New key KEY_AI_TABLE_PARSER_CONFIGS for cached table configs, following existing pattern. |

**Gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/014-table-pdf-parsing/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research findings
├── data-model.md        # Phase 1 data model
└── tasks.md             # Phase 2 task breakdown (created by /speckit.tasks)
```

### Source Code (repository root)

```text
core/model/src/main/java/com/atelbay/money_manager/core/model/
└── TableParserConfig.kt                    # NEW — column-index config model

core/parser/src/main/java/com/atelbay/money_manager/core/parser/
├── PdfTextExtractor.kt                     # MODIFY — extract ensureInitialized() as internal
├── PdfTableExtractor.kt                    # NEW — TextPosition-based table extraction
├── TableStatementParser.kt                 # NEW — table rows → ParsedTransaction
├── RegexStatementParser.kt                 # MODIFY — extract shared amount parsing utility
├── StatementParser.kt                      # MODIFY — add table extraction methods + new deps
└── AmountParser.kt                         # NEW — shared amount/date parsing utilities

core/ai/src/main/java/com/atelbay/money_manager/core/ai/
├── GeminiService.kt                        # MODIFY — add generateTableParserConfig interface method
└── GeminiServiceImpl.kt                    # MODIFY — implement table config prompt + schema

core/datastore/src/main/java/.../datastore/
└── UserPreferences.kt                      # MODIFY — add KEY_AI_TABLE_PARSER_CONFIGS

domain/import/src/main/java/.../usecase/
├── ParseStatementUseCase.kt                # MODIFY — insert table path in pipeline
└── ImportStepEvent.kt                      # MODIFY — add table-related events

presentation/import/src/main/java/.../debug/
└── DebugImportSheet.kt                     # MODIFY — display new table events

core/parser/src/test/java/.../parser/
├── PdfTableExtractorTest.kt                # NEW — table extraction tests
└── TableStatementParserTest.kt             # NEW — table parsing tests

core/ai/src/test/java/.../ai/
└── GeminiServiceImplTest.kt                # MODIFY — add table config tests

domain/import/src/test/java/.../usecase/
└── ParseStatementUseCaseTest.kt            # MODIFY — add table path tests
```

**Structure Decision**: No new modules. All changes fit within existing module boundaries. TableParserConfig goes in core/model (shared by core/parser and core/ai). This requires adding kotlinx-serialization plugin to core/model's build.gradle.kts.

## Key Design Decisions

### D1: TableParserConfig location → core/model

**Decision**: Place TableParserConfig in `core:model` with kotlinx-serialization.
**Rationale**: Both `core:parser` and `core:ai` need access. `core:model` is already the shared model module. Currently it has no serialization, but adding it is minimal (plugin + 1 dependency).
**Alternative rejected**: `core:remoteconfig` — would work (ParserConfig already there) but semantically wrong. TableParserConfig is a domain model, not a remote config artifact.

### D2: Shared amount parsing → extract utility

**Decision**: Extract `parseAmount()` and date parsing from `RegexStatementParser` into a shared `AmountParser` utility object in `core:parser`.
**Rationale**: `TableStatementParser` needs identical logic. Duplicating ~20 lines is error-prone. A simple object with static-like functions avoids any DI complexity.
**Alternative rejected**: Duplicating the code — maintainability risk with 3 amount formats.

### D3: Table extraction algorithm → simple clustering (not DBSCAN)

**Decision**: Use sorted-gap column detection: sort all X-positions, find gaps > threshold, split into columns. For rows: sort Y-positions, group by proximity.
**Rationale**: Full DBSCAN is overkill for bank statement tables (typically 4-7 columns, clear gaps). Sorted-gap detection is simpler, faster, and sufficient.
**Alternative rejected**: DBSCAN — adds unnecessary complexity and code for this use case.

### D4: PdfBox init sharing → internal visibility

**Decision**: Make `PdfTextExtractor.ensureInitialized()` `internal` so `PdfTableExtractor` can call it (both in `core:parser`).
**Rationale**: `PDFBoxResourceLoader.init()` is idempotent but must be called before any PdfBox usage. Sharing avoids redundant init logic.

### D5: Table config caching → separate DataStore key

**Decision**: New `KEY_AI_TABLE_PARSER_CONFIGS` key in DataStore, storing serialized list of TableParserConfig.
**Rationale**: Table configs and regex configs are fundamentally different shapes. Mixing them in one key would require a discriminated union. Separate keys are cleaner.

## Complexity Tracking

> No Constitution violations to justify. All gates pass.
