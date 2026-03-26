# Fix BCC Bank (Банк ЦентрКредит) PDF Table Parsing

## Problem

BCC Bank PDF import fails — 0 transactions parsed via both table and regex paths. This is after the PdfTableExtractor column detection fix (commit `55ecebd` on branch `fix/pdf-table-extractor-column-detection`).

## Root Cause Analysis

There are **two independent bugs** causing the BCC failure. Both must be fixed.

### Bug 1: Multi-line transaction rows not merged

BCC PDF renders each transaction across 2+ visual lines. PdfTableExtractor groups by Y-coordinate, so each visual line becomes a separate row. Example from actual device logs:

```
Row N:   ["2024-12- 2024-12-31", "Аударым", "10 000.00", "-10 000.00 0.00 0.00"]
Row N+1: ["31", "KZT", "KZT", "KZT KZT"]
```

The date `2024-12-31` is split as `"2024-12-"` (row N) and `"31"` (row N+1). The amount `10 000.00` is on row N, but `KZT` is on row N+1. These should be merged into one logical transaction row.

**Other banks affected:** Halyk also has this issue (noted in `specs/014-table-pdf-parsing/NOTES.md` line 39).

### Bug 2: `extractSampleTableRowsWithContext` sends only metadata to Gemini, no transaction rows

In `StatementParser.extractSampleTableRowsWithContext()` (line 120), the algorithm splits rows into "metadata" vs "structural" based on `modalCount` — the most frequent non-empty-cell count per row.

For BCC's 4-column extraction:
- Header/metadata rows like `["Валюта", "KZT", "", ""]` have 2 non-empty cells
- Transaction rows like `["2024-12-31", "Аударым", "10 000.00", "-10 000.00 0.00 0.00"]` have 4 non-empty cells
- BUT continuation rows like `["31", "KZT", "KZT", "KZT KZT"]` also have 4

If there are more header-type rows (2 non-empty) than transaction rows, `modalCount = 2`, `threshold = max(2/2, 2) = 2`. Then all rows with ≥2 non-empty cells are "structural", and the first structural row becomes `columnHeaderRow` — which is actually a header/address line, not the table column headers. The sample rows sent to Gemini are all metadata, not transactions.

## What the BCC PDF Actually Contains

### Text extracted by PdfTextExtractor (regex path, properly spaced):
```
Операция Шотта Операция Шоттағы Комиссия, Кешбэк Банктік Айырбас
Операция
күні көрсетілген валютасын сома LZT KZT айырбастау бағамы
сипаттамасы
күні дағы сома мөлшерлеме

2024-12- 2024-12-31 Аударым 10 000.00 -10 000.00 0.00 0.00
31 KZT KZT KZT KZT
2024-12- 2024-12-28 Аударым 100 000.0 -100 000.00 0.00 0.00
28 0 KZT KZT KZT KZT
2024-12- 2024-12-28 Толыќтыру 100 000.0 100 000.00 0.00 0.00
28 0 KZT KZT KZT KZT
2024-12- 2024-12-25 Пополнение от АО 107 061.0 107 061.00 0.00 0.00
25 Финансовый центр, 0 KZT KZT KZT KZT
ИИН
050740000618, счет
KZ95601011100004
4506 Плательщик:
АО Финансовый
центр
```

### Key observations about the BCC transaction format:
- **Date format**: `yyyy-MM-dd` (ISO style, NOT `dd.MM.yyyy`)
- **Amount format**: space-separated with dot decimal: `100 000.00` — this is `"space_dot"` but our parser only supports `"dot"`, `"comma_dot"`, `"space_comma"`. Need to add `"space_dot"` support OR use `"dot"` format after stripping spaces.
- **Column structure**: ~8 logical columns: Op date | Shown date | Description | Op currency amount | Account KZT amount | Commission KZT | Cashback KZT | Exchange rate
- **Sign is in the amount**: negative amounts = expense (`-10 000.00`), positive = income (`100 000.00`)
- **Multi-line rows**: date wraps (`"2024-12-"` + `"31"`), long descriptions wrap over 3-6 lines
- **Currency suffix**: `KZT` appears on continuation lines, not on the amount line itself

### Table extracted by PdfTableExtractor (4 columns detected):
```json
["Телефон: +7 727 244 77 77 (ш", "етелден), 505 (Қазақстан ішінде)", "", ""]
["Шот бойынша үзінді", "KZ348562204141013952", "", ""]
["Валюта", "KZT", "", ""]
["Кезеңі", "01.08.2024 - 23.01.2025", "", ""]
["01.08.2024 қалдык", "Кезеңге кірістер", "Кезеңг", "е шығыстар  23.01.2025 қалдык"]
["0.00 KZT", "618 244.00", "615 0", "00.00 KZT 3 244.00 KZT"]
```

The column boundaries are cutting through words (e.g. `"Кезеңг"` | `"е шығыстар"`). This means the column detection found boundaries at positions that don't correspond to actual table columns in the BCC PDF. The BCC PDF layout may use different spacing in the header vs the transaction table.

## Required Fixes

### Fix 1: Multi-line row merging in PdfTableExtractor

**File:** `core/parser/src/main/java/.../PdfTableExtractor.kt`

After building the raw table rows, add a post-processing step to merge continuation rows. A continuation row is one where:
- The first cell (date column typically) does NOT start with a date-like pattern
- OR the first cell is very short (e.g. just `"31"` or `"28"`)
- OR the row has significantly fewer non-empty cells than the row above it

**Algorithm:**
1. After `buildTable()` produces `List<List<String>>`, iterate through rows
2. For each row, check if it looks like a "start of new transaction" — first cell matches a date pattern like `\d{2,4}[-./]\d{2}[-./]` or similar
3. If it does NOT look like a new transaction, merge it with the previous row by appending each cell's text (with a space separator) to the corresponding cell of the previous row
4. Return the merged rows

**Important:** This merging should happen INSIDE `PdfTableExtractor.extractTable()` after `buildTable()`, so all downstream consumers benefit. The merge logic should be configurable or smart enough to not break banks that don't have multi-line rows (Kaspi, Forte etc. — their rows are single-line so merging won't trigger).

### Fix 2: Improve `extractSampleTableRowsWithContext` to find actual transaction rows

**File:** `core/parser/src/main/java/.../StatementParser.kt` (method at line 120)

The current logic uses `modalCount` of non-empty cells, which is fragile. Improve it:

1. After splitting by modal count, check if the `sampleRows` (structural rows after dropping header) actually contain any date-like strings in their first column
2. If not, try a different splitting strategy: look for rows where ANY cell matches a date pattern (`\d{4}-\d{2}-\d{2}` or `\d{2}\.\d{2}\.\d{4}`)
3. Use those date-containing rows as the basis for structural rows
4. The metadata rows are everything before the first date-containing row

### Fix 3: Add `space_dot` amount format support

**File:** `core/parser/src/main/java/.../AmountParser.kt`

BCC uses amounts like `100 000.00` — space as thousands separator, dot as decimal. Currently `parseAmount()` supports:
- `"dot"`: `10000.50` — strips non-digit except `.` and `-`
- `"comma_dot"`: `10,000.50` — strips commas
- `"space_comma"`: `10 000,50` — strips spaces, replaces `,` with `.`

Add:
- `"space_dot"`: `100 000.00` — strip spaces (and any non-digit except `.` and `-`), then parse as double

This is straightforward: `amountStr.replace(Regex("[^\\d.\\-]"), "").toDouble()`

**Also update** the Gemini prompt in `GeminiServiceImpl` (the `buildTableParserConfigPrompt` method) to document the new `"space_dot"` format option.

**Also update** `TableParserConfig` model if needed — check if `amountFormat` is an enum or free-form string.

### Fix 4: Handle date column containing two dates

BCC's date column contains `"2024-12-31 2024-12-31"` (operation date + processing date concatenated with space). After multi-line merging, the cell might be `"2024-12- 2024-12-31 31 KZT"`.

Actually, with proper multi-line merging, the date cell should become something like `"2024-12-31"` (first part) if the continuation `"31"` gets merged into the right cell. But if the merged result is `"2024-12-31 2024-12-31"`, the date parser needs to handle extracting just the first date.

**In `TableStatementParser.parseRow()`**, before parsing the date, try to extract just the first date-like substring from the cell. For example, if the cell contains `"2024-12-31 2024-12-31"`, extract `"2024-12-31"`.

## Critical Files

| File | What to change |
|------|---------------|
| `core/parser/src/main/java/.../PdfTableExtractor.kt` | Add multi-line row merging after buildTable() |
| `core/parser/src/main/java/.../StatementParser.kt` | Fix extractSampleTableRowsWithContext to find transaction rows |
| `core/parser/src/main/java/.../AmountParser.kt` | Add "space_dot" format |
| `core/parser/src/main/java/.../TableStatementParser.kt` | Handle double-date cells, extract first date |
| `core/ai/src/main/java/.../GeminiServiceImpl.kt` | Add "space_dot" to prompt documentation |
| `core/model/src/main/java/.../TableParserConfig.kt` | Check if amountFormat needs updating |
| `core/parser/src/androidTest/.../PdfTableExtractorTest.kt` | Add tests for multi-line merging |

## Verification

1. `./gradlew :core:parser:connectedDebugAndroidTest` — all tests pass
2. `./gradlew :core:parser:test` — unit tests pass
3. Deploy to device, import BCC PDF → verify transactions parse correctly
4. Import Halyk PDF → verify multi-line merging helps there too
5. Import Kaspi/Forte PDF → verify no regressions (single-line rows should be unaffected)

## What NOT to change

- Don't modify the column boundary detection algorithm (just fixed in the previous commit)
- Don't change the 3-attempt retry loop structure in ParseStatementUseCase
- Don't modify the regex path — focus on making the table path work
- Don't add hardcoded BCC config — the AI should be able to generate it once the table extraction is correct
