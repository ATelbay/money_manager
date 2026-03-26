# Implementation Plan: Fix BCC Bank PDF Table Parsing

**Branch**: `015-bcc-table-parsing` | **Date**: 2026-03-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/015-bcc-table-parsing/spec.md`

## Summary

BCC Bank PDF import fails with 0 transactions parsed. Four independent bugs must be fixed: (1) multi-line transaction rows are not merged in PdfTableExtractor, (2) sample row detection sends metadata instead of transaction rows to Gemini, (3) `space_dot` amount format is missing, (4) date cells with two concatenated dates fail to parse. All fixes are generic improvements to the parsing pipeline — no bank-specific hardcoding.

## Technical Context

**Language/Version**: Kotlin 2.3.0
**Primary Dependencies**: PdfBox-Android 2.0.27.0, Firebase AI (Gemini 2.5 Flash), kotlinx-serialization
**Storage**: N/A (no data layer changes)
**Testing**: JUnit 4 (unit), AndroidTest with ComposeTestRule (instrumented for PdfBox)
**Target Platform**: Android (minSdk 26)
**Project Type**: Mobile app — `core:parser` and `core:ai` modules
**Performance Goals**: PDF parsing completes in <5s for typical bank statements
**Constraints**: PdfBox-Android requires Android runtime (unit tests for PdfTableExtractor are @Ignored — use androidTest)
**Scale/Scope**: 4 files modified in core:parser, 1 in core:ai, tests added

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture Multi-Module | PASS | Changes stay within `core:parser` and `core:ai` — no layer boundary violations |
| II. Kotlin-First | PASS | All code is Kotlin |
| V. Hilt DI | PASS | No new injectable classes; PdfTableExtractor is already @Singleton @Inject |
| VII. Testing Architecture | PASS | Unit tests for AmountParser, TableStatementParser, StatementParser; androidTest for PdfTableExtractor |
| X. Statement Import Pipeline | PASS | Enhances table parsing path per the established Level 1 → Level 2 pipeline |

No violations. No new modules, no new dependencies.

**Post-Phase 1 re-check**: PASS — design adds methods to existing classes, no architectural changes.

## Project Structure

### Documentation (this feature)

```text
specs/015-bcc-table-parsing/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: research decisions
├── data-model.md        # Phase 1: data model (no changes)
├── quickstart.md        # Phase 1: quickstart guide
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (modified files)

```text
core/parser/src/main/java/com/atelbay/money_manager/core/parser/
├── PdfTableExtractor.kt        # Add mergeMultiLineRows() after buildTable()
├── StatementParser.kt           # Add date-pattern fallback in extractSampleTableRowsWithContext()
├── AmountParser.kt              # Add "space_dot" case in parseAmount()
└── TableStatementParser.kt      # Add extractFirstDate() in parseRow()

core/ai/src/main/java/com/atelbay/money_manager/core/ai/
└── GeminiServiceImpl.kt         # Add "space_dot" to schema enums + prompt text

core/parser/src/test/java/com/atelbay/money_manager/core/parser/
├── AmountParserTest.kt          # NEW: unit tests for space_dot format
├── TableStatementParserTest.kt  # Add: space_dot format test, double-date test
└── StatementParserTest.kt       # Add: date-fallback sample row detection test

core/parser/src/androidTest/java/com/atelbay/money_manager/core/parser/
└── PdfTableExtractorTest.kt     # Add: multi-line merging tests
```

**Structure Decision**: No new modules. All changes fit within existing `core:parser` and `core:ai` modules.

## Implementation Details

### Fix 1: Multi-line Row Merging (PdfTableExtractor.kt)

**Method**: `mergeMultiLineRows(rows: List<List<String>>): List<List<String>>`

**Algorithm**:
```
DATE_PATTERN = Regex("\\d{2,4}[-./]\\d{2}[-./]?")

result = mutableListOf<MutableList<String>>()
for each row in rows:
    firstCell = row[0].trim()
    if firstCell matches DATE_PATTERN at start → new row (add to result)
    else if result is empty → new row (can't merge into nothing)
    else → merge: for each cell index i, append " " + row[i] to result.last()[i]
return result (trimmed)
```

**Call site**: In `extractTable()`, after `allRows.addAll(stripper.buildTable())` loop completes, call `mergeMultiLineRows(allRows)` and return the result.

**Safety for single-line banks**: Kaspi, Forte, Bereke all have date patterns in cell[0] of every data row. The merge condition never triggers — zero behavioral change for existing banks.

### Fix 2: Sample Row Detection Fallback (StatementParser.kt)

**Method**: Enhance `extractSampleTableRowsWithContext()`

After the existing modal-count split produces `structuralRows` and `metadataRows`, add a validation step:

```kotlin
val datePattern = Regex("\\d{2,4}[-./]\\d{2}[-./]?\\d{0,4}")
val sampleRows = structuralRows.drop(1).take(10)

// Check if any sample row contains a date in any cell
val hasDate = sampleRows.any { row -> row.any { cell -> datePattern.containsMatchIn(cell) } }

if (!hasDate && table.size > 2) {
    // Fallback: find rows with dates
    val dateRows = table.filter { row -> row.any { cell -> datePattern.containsMatchIn(cell) } }
    val nonDateRows = table.filter { row -> row.none { cell -> datePattern.containsMatchIn(cell) } }
    return TableExtractionResult(
        sampleRows = dateRows.take(10),
        metadataRows = nonDateRows,
        columnHeaderRow = null, // Can't reliably detect header in fallback
    )
}
```

### Fix 3: `space_dot` Amount Format (AmountParser.kt)

Add one case to the `when` block:

```kotlin
"space_dot" -> amountStr.replace(Regex("[^\\d.\\-]"), "").toDouble()
```

Update `GeminiServiceImpl.kt`:
- Add `"space_dot"` to `tableParserConfigSchema` enum list (line 95)
- Add `"space_dot"` to `parserConfigSchema` enum list (line 47)
- Add `"space_dot": "100 000.50"` to prompt text in both `buildTableParserConfigPrompt` and `buildParserConfigPrompt`

### Fix 4: First-Date Extraction (TableStatementParser.kt)

In `parseRow()`, before calling `AmountParser.parseDateString()`:

```kotlin
val cleanDateStr = extractFirstDate(dateStr, config.dateFormat)

private fun extractFirstDate(cell: String, dateFormat: String): String {
    // Convert dateFormat to a regex: replace letter sequences with \d+, escape punctuation
    val pattern = dateFormat
        .replace(Regex("[yMd]+")) { "\\d+" }
        .replace(".", "\\.")
        .replace("-", "\\-")
    val match = Regex(pattern).find(cell)
    return match?.value ?: cell.trim()
}
```

This extracts the first date matching the expected format from cells that may contain extra text.
