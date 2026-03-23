# Quickstart: Table-Based PDF Parsing

**Feature**: 014-table-pdf-parsing | **Date**: 2026-03-23

## What This Feature Does

Adds a table-extraction step to the bank statement import pipeline. Instead of relying solely on regex patterns (which Gemini Flash fails to generate reliably), the system now:

1. Extracts table structure from PDFs using character coordinates
2. Asks the AI "which column is what?" instead of "write a regex"
3. Parses transactions using simple column indices

## Build & Test

```bash
# Build
./gradlew assembleDebug

# Run affected unit tests
./gradlew :core:parser:test :core:ai:test :domain:import:test

# Run all tests
./gradlew test

# Lint
./gradlew lint detekt
```

## Key Files

| File | Purpose |
|------|---------|
| `core/model/.../TableParserConfig.kt` | Column-index config model |
| `core/parser/.../PdfTableExtractor.kt` | TextPosition-based table extraction |
| `core/parser/.../TableStatementParser.kt` | Table rows → ParsedTransaction |
| `core/parser/.../AmountParser.kt` | Shared amount/date parsing |
| `core/ai/.../GeminiServiceImpl.kt` | AI table config generation |
| `domain/import/.../ParseStatementUseCase.kt` | Pipeline integration |

## Pipeline Order

```
1. Remote Config regex        ← existing, unchanged
2. Cached AI regex configs    ← existing, unchanged
3. Cached table configs       ← NEW (fast, no AI call)
4. AI table config generation ← NEW (simple prompt, 3 retries)
5. AI regex generation        ← existing fallback
6. Full AI multimodal parse   ← existing ultimate fallback
```

## Manual Testing

1. **Existing banks**: Import PDFs from Kaspi, Freedom, Forte, Bereke, Eurasian — should work via step 1 (regex) as before
2. **New bank (table)**: Import a PDF from an unconfigured bank with tabular data — should succeed via step 3 or 4
3. **Non-table PDF**: Import a non-tabular PDF — should skip table steps and fall through to step 5/6
4. **Debug sheet**: Enable debug import sheet — verify table extraction events appear
