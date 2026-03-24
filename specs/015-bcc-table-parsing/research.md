# Research: Fix BCC Bank PDF Table Parsing

## R1: Multi-line Row Merging Strategy

**Decision**: Use a date-pattern heuristic on the first cell of each row to determine if it's a "start" row or a continuation row. Merge continuation rows into the previous start row by appending cell text with space separator.

**Rationale**: BCC and Halyk PDFs render transactions across 2+ visual lines. PdfTableExtractor groups by Y-coordinate, producing one row per visual line. The first cell of a transaction row always starts with a date (e.g., `2024-12-`, `01.03.2025`, `15.03.24`). Continuation rows have non-date content in cell[0] (e.g., `"31"`, `"KZT"`, `"ИИН 050740..."`). A regex like `\d{2,4}[-./]\d{2}` reliably distinguishes start rows.

**Alternatives considered**:
- Cell count heuristic (continuation rows have fewer non-empty cells): Rejected because BCC continuation rows like `["31", "KZT", "KZT", "KZT KZT"]` have 4 non-empty cells, same as data rows.
- Configurable flag per bank: Rejected because the date-pattern approach is generic and works for all banks — single-line banks always start with a date, so no merging triggers.

**Implementation location**: New private method `mergeMultiLineRows(rows: List<List<String>>): List<List<String>>` in `PdfTableExtractor`, called in `extractTable()` after collecting all page rows.

---

## R2: Sample Row Detection Fallback

**Decision**: After the existing modal-count split, validate that `sampleRows` actually contain date-like strings. If not, fall back to a date-scanning strategy: scan all rows for cells matching common date patterns, and use those rows as structural/transaction rows.

**Rationale**: For BCC, the modal-count heuristic fails because metadata rows (2 non-empty cells) outnumber transaction rows. After multi-line merging (R1), the row distribution should improve, but a fallback is still needed for edge cases where the modal count doesn't represent transaction rows.

**Alternatives considered**:
- Replace the modal-count heuristic entirely with date-scanning: Rejected because the modal-count approach works well for most banks (Kaspi, Forte, Bereke, Eurasian). Adding a fallback preserves the working path while fixing edge cases.
- Use column header detection: Rejected — too fragile, headers vary by language and bank.

---

## R3: `space_dot` Amount Format

**Decision**: Add `"space_dot"` as a fourth format in `AmountParser.parseAmount()`. Implementation: strip all characters except digits, `.`, and `-`, then parse as Double.

**Rationale**: BCC uses `100 000.00` format (space thousands separator, dot decimal). The existing `"dot"` format strips non-digit-dot-minus but doesn't strip spaces. The existing `"space_comma"` replaces comma with dot but BCC already uses dot as decimal.

**Alternatives considered**:
- Modify `"dot"` format to also strip spaces: Rejected because it would change behavior for existing banks using `"dot"` format, potentially accepting malformed strings that currently fail.
- Use `"space_comma"` and handle the result: Rejected because `space_comma` replaces `,` with `.` which could produce `100000..00` if there's already a dot.

**Files to update**:
1. `AmountParser.kt`: Add `"space_dot"` case
2. `GeminiServiceImpl.kt`: Add `"space_dot"` to both `tableParserConfigSchema` enum and `buildTableParserConfigPrompt` documentation. Also add to `parserConfigSchema` enum and `buildParserConfigPrompt` docs.

---

## R4: Double-Date Cell Extraction

**Decision**: In `TableStatementParser.parseRow()`, before parsing the date, extract the first date-like substring from the cell using a regex scan. If the cell contains `"2024-12-31 2024-12-31"`, extract `"2024-12-31"`.

**Rationale**: BCC's table has two date columns (operation date + processing date) that may merge into a single cell after column detection. After multi-line row merging, continuation text may also append to the date cell. Extracting the first date-like substring is more robust than assuming the cell contains only one clean date.

**Alternatives considered**:
- Split on whitespace and take the first element: Rejected because some date formats contain spaces (e.g., `"1 Jan 2025"`).
- Use the existing `DateTimeFormatter.parse()` with lenient mode: Rejected because lenient mode may parse garbage and Java's formatter doesn't do substring matching.

**Implementation**: Extract first match of a configurable date regex derived from the `dateFormat` string. For `yyyy-MM-dd` → scan for `\d{4}-\d{2}-\d{2}`. For `dd.MM.yyyy` → scan for `\d{2}\.\d{2}\.\d{4}`. A generic approach: convert the dateFormat to a regex by replacing letter groups with `\d+` and escaping punctuation.
