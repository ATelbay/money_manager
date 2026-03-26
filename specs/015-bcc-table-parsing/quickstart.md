# Quickstart: Fix BCC Bank PDF Table Parsing

## What This Feature Does

Fixes BCC Bank (Банк ЦентрКредит) PDF statement import which currently parses 0 transactions. Four independent bugs are addressed:

1. **Multi-line row merging** — BCC transactions span 2+ visual lines in the PDF; the table extractor now merges continuation rows into logical transaction rows
2. **Sample row detection** — The algorithm that selects sample rows for Gemini AI now falls back to date-pattern scanning when the modal-count heuristic fails
3. **`space_dot` amount format** — New format for amounts like `100 000.00` (space thousands, dot decimal)
4. **Double-date cell extraction** — Date cells containing two dates (operation + processing) now extract just the first date

## Files Changed

| File | Change |
|------|--------|
| `core/parser/.../PdfTableExtractor.kt` | Add `mergeMultiLineRows()` after `buildTable()` |
| `core/parser/.../StatementParser.kt` | Add date-pattern fallback in `extractSampleTableRowsWithContext()` |
| `core/parser/.../AmountParser.kt` | Add `"space_dot"` case in `parseAmount()` |
| `core/parser/.../TableStatementParser.kt` | Add `extractFirstDate()` in `parseRow()` |
| `core/ai/.../GeminiServiceImpl.kt` | Add `"space_dot"` to schema enums and prompt docs |

## How to Test

```bash
# Unit tests
./gradlew :core:parser:test

# Instrumented tests (requires emulator/device)
./gradlew :core:parser:connectedDebugAndroidTest

# Full build
./gradlew assembleDebug
```

Manual verification: import a BCC PDF statement on device → verify transactions appear in preview.

## Key Design Decisions

- Multi-line merging uses date-pattern detection (`\d{2,4}[-./]\d{2}`) in the first cell, not cell-count heuristics — this is safe for all banks since single-line banks always start with a date
- Merging happens inside `PdfTableExtractor.extractTable()` so all downstream consumers (sample extraction, parsing) benefit automatically
- The `"space_dot"` format is added alongside existing formats, not as a modification to `"dot"` — preserves existing behavior
