# Implementation Plan: Improve AI Parser Config Generation

**Branch**: `013-ai-parser-config` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-ai-parser-config/spec.md`

## Summary

Improve the Gemini-based `generateParserConfig` flow to produce working regex configs by: (1) sending 60 lines of sample data instead of 10, (2) including 2-3 existing working configs as few-shot examples, (3) adding a 3-attempt retry loop with structured failure tracing, (4) fixing `operation_type_map` double-encoding by using `Schema.array()` of key-value pairs, and (5) rewriting the prompt in English.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: Firebase AI SDK (Gemini 2.5 Flash), kotlinx-serialization-json, Hilt 2.58, PdfBox-Android 2.0.27.0
**Storage**: Preferences DataStore (cached AI configs), Firebase Remote Config (parser configs)
**Testing**: JUnit 4, MockK, Turbine, kotlinx-coroutines-test
**Target Platform**: Android (minSdk 26)
**Project Type**: Mobile app (Android)
**Performance Goals**: AI config generation + up to 3 retries should complete within reasonable time (~30s total max); each regex parse attempt has 5s timeout
**Constraints**: ParserConfig data class is frozen; RegexStatementParser unchanged; full-parse path unchanged
**Scale/Scope**: 4 files modified across 3 existing modules (core:ai, core:parser, domain:import)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | No new modules. Changes in core:ai, core:parser, domain:import — all existing modules with correct dependency directions. |
| II. Kotlin-First & Jetpack Compose | PASS | All code is Kotlin. No UI changes. |
| V. Hilt DI | PASS | GeminiServiceImpl is already `@Singleton @Inject`. FailedAttempt is a plain data class, no DI needed. |
| VII. Testing Architecture | PASS | Unit tests required for: retry loop logic, FailedAttempt construction, prompt building with few-shot examples, operation_type_map parsing. |
| VIII. Firebase Ecosystem | PASS | Uses Firebase AI (Gemini) and Remote Config — existing integrations, no new Firebase services. |
| X. Statement Import Pipeline | PASS | Enhances Level 1→Level 2 transition. Does not change RegexStatementParser or full-AI fallback path. |
| XI. Preferences DataStore | PASS | Cached AI configs in DataStore unchanged. |

**Gate result**: ALL PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/013-ai-parser-config/
├── plan.md              # This file
├── research.md          # Phase 0: technical decisions
├── data-model.md        # Phase 1: FailedAttempt entity
├── quickstart.md        # Phase 1: implementation guide
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
core/ai/src/main/java/com/atelbay/money_manager/core/ai/
├── GeminiService.kt              # Interface: add FailedAttempt, update signature
└── GeminiServiceImpl.kt          # Impl: prompt, schema, response parser

core/parser/src/main/java/com/atelbay/money_manager/core/parser/
└── StatementParser.kt            # SAMPLE_LINE_COUNT 10→60

domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/
└── ParseStatementUseCase.kt      # Retry loop in tryRegexThenGemini()

core/ai/src/test/java/com/atelbay/money_manager/core/ai/
└── GeminiServiceImplTest.kt      # Unit tests for prompt/schema/parsing changes

domain/import/src/test/java/com/atelbay/money_manager/domain/importstatement/usecase/
└── ParseStatementUseCaseTest.kt  # Unit tests for retry loop
```

**Structure Decision**: No new modules or directories. All changes are in existing files within core:ai, core:parser, and domain:import modules. Test files may be new if they don't exist yet.
