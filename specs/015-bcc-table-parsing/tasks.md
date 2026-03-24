# Tasks: Fix BCC Bank PDF Table Parsing

**Input**: Design documents from `/specs/015-bcc-table-parsing/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: Included — spec explicitly requires unit tests for new logic.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Foundational (Independent Parsing Improvements)

**Purpose**: Independent fixes that serve as building blocks for the BCC import fix. These modify separate files with no cross-dependencies.

- [ ] T001 [P] Add `space_dot` amount format to `AmountParser.parseAmount()` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/AmountParser.kt`. Add a new `when` branch: `"space_dot" -> amountStr.replace(Regex("[^\\d.\\-]"), "").toDouble()`. This strips spaces and all non-numeric characters except `.` and `-`, then parses as Double. Handles BCC amounts like `100 000.00` and `-10 000.00`.

- [ ] T002 [P] Add `extractFirstDate()` method to `TableStatementParser` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/TableStatementParser.kt`. In `parseRow()`, before calling `AmountParser.parseDateString(dateStr, config.dateFormat)`, extract the first date-like substring from the cell. Implement a private `extractFirstDate(cell: String, dateFormat: String): String` that converts the dateFormat pattern to a regex (replace `[yMd]+` sequences with `\\d+`, escape `.` and `-`) and returns the first match, or `cell.trim()` if no match found. This handles cells like `"2024-12-31 2024-12-31"` by extracting just `"2024-12-31"`.

- [ ] T003 [P] Add `space_dot` to Gemini schema enums and prompt text in `core/ai/src/main/java/com/atelbay/money_manager/core/ai/GeminiServiceImpl.kt`. Update 4 locations: (1) `tableParserConfigSchema` — add `"space_dot"` to the `amount_format` enum list at line ~95, (2) `parserConfigSchema` — add `"space_dot"` to the `amount_format` enum list at line ~47, (3) `buildTableParserConfigPrompt` — add `"space_dot" (100 000.50)` to the amount_format documentation, (4) `buildParserConfigPrompt` — add `"space_dot": "100 000.50"` to the amount_format documentation.

**Checkpoint**: Three independent fixes complete — `space_dot` parsing works, date extraction handles multi-date cells, Gemini can recommend `space_dot` format.

---

## Phase 2: User Story 1 — BCC Bank Statement Import (Priority: P1) 🎯 MVP

**Goal**: Fix BCC PDF import so all transactions are parsed correctly — multi-line rows merged, sample rows sent to Gemini properly, amounts and dates parsed.

**Independent Test**: Import a BCC PDF statement → verify all transactions appear in preview with correct dates, amounts, descriptions, and income/expense types.

### Implementation for User Story 1

- [ ] T004 [US1] Add `mergeMultiLineRows()` method to `PdfTableExtractor` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/PdfTableExtractor.kt`. Add a private method `mergeMultiLineRows(rows: List<List<String>>): List<List<String>>` that iterates rows and checks if each row's first cell (trimmed) starts with a date pattern `Regex("^\\d{2,4}[-./]\\d{2}[-./]?")` (anchored with `^` because we check the first cell only; optional trailing separator to match both `yyyy-MM-dd` and `dd.MM.yyyy` formats). If it matches → start a new logical row. If it doesn't match and there's a previous row → merge by appending each cell's text (space-separated) to the corresponding cell of the previous row. Handle edge cases: empty rows, rows with different cell counts (pad with empty strings), first row being a non-date row (treat as standalone). Call this method in `extractTable()` on `allRows` before returning: replace `allRows` with `mergeMultiLineRows(allRows)` and return the merged result. Ensure padding works when continuation row has more cells than parent (pad parent with empty strings) or fewer cells (pad continuation). Add a comment documenting this edge case for reviewers.

- [ ] T005 [US1] Add date-pattern fallback to `extractSampleTableRowsWithContext()` in `core/parser/src/main/java/com/atelbay/money_manager/core/parser/StatementParser.kt`. After the existing modal-count split produces `structuralRows` and `sampleRows`, add validation: define `val datePattern = Regex("\\d{2,4}[-./]\\d{2}[-./]?\\d{0,4}")`, check `val hasDate = sampleRows.any { row -> row.any { cell -> datePattern.containsMatchIn(cell) } }`. If `!hasDate && table.size > 2`, fall back: filter `table` into `dateRows` (rows where any cell matches `datePattern`) and `nonDateRows` (rest), return `TableExtractionResult(sampleRows = dateRows.take(10), metadataRows = nonDateRows, columnHeaderRow = null)`. This ensures Gemini always receives actual transaction rows, not metadata.

### Tests for User Story 1

- [ ] T006 [P] [US1] Add unit tests for `space_dot` amount format in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/TableStatementParserTest.kt`. Add test cases: (1) `space_dot format parses 100 000.00 correctly` → table with `"100 000.00"` in amount column, config with `amountFormat = "space_dot"` → assert amount == 100000.0, (2) `space_dot format handles negative amount -10 000.00` → assert amount == 10000.0 and type == EXPENSE when `negativeSignMeansExpense = true`, (3) `space_dot format parses amount with currency suffix stripped` → `"100 000.00 KZT"` → assert amount == 100000.0. Use existing `buildConfig()` helper.

- [ ] T007 [P] [US1] Add unit tests for `extractFirstDate()` in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/TableStatementParserTest.kt`. Add test cases: (1) `double date cell extracts first date with yyyy-MM-dd format` → table with date cell `"2024-12-31 2024-12-31"`, config with `dateFormat = "yyyy-MM-dd"` → assert parsed date == 2024-12-31, (2) `date cell with extra text extracts date` → date cell `"2024-12-31 KZT"` → assert parsed date == 2024-12-31, (3) `single clean date parses normally` → date cell `"31.03.2024"` with `dateFormat = "dd.MM.yyyy"` → assert date == 2024-03-31 (no regression), (4) `date cell with no matching date falls back to trim` → date cell `"NoDate"` → assert row returns null (date parse fails gracefully).

- [ ] T008 [P] [US1] Add unit tests for date-pattern fallback in sample row detection in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/StatementParserTest.kt`. Add test: `extractSampleTableRowsWithContext falls back to date scanning when modal count is wrong` — mock `pdfTableExtractor.extractTable()` to return a BCC-like table: 4 metadata rows with 2 non-empty cells each (e.g., `["Валюта", "KZT", "", ""]`), 2 transaction rows with 4 non-empty cells (e.g., `["2024-12-31", "Аударым", "10 000.00", "-10 000.00"]`). Assert that `extractSampleTableRowsWithContext()` returns the 2 transaction rows as `sampleRows` (via fallback), not the metadata rows. Add a second test: `extractSampleTableRowsWithContext handles minimal BCC table with 1-2 transactions` — mock a table with 4 metadata rows and only 1 transaction row. Assert the fallback still finds and returns the single transaction row as `sampleRows` (edge case from spec).

**Checkpoint**: BCC Bank PDF import should now parse all transactions correctly. Multi-line rows are merged, Gemini receives actual transaction data, amounts in `space_dot` format parse, and double-date cells extract the first date.

---

## Phase 3: User Story 2 — No Regression for Existing Banks (Priority: P1)

**Goal**: Verify that multi-line merging and sample row detection changes don't break Kaspi, Forte, Bereke, Eurasian, or Freedom bank parsing.

**Independent Test**: Run all existing parser unit tests → all pass. Import Kaspi/Forte PDFs → same results as before.

### Verification for User Story 2

- [ ] T009 [US2] Run all existing parser unit tests and verify zero failures: `./gradlew :core:parser:test`. This validates that `AmountParser` changes (new `space_dot` format doesn't affect existing formats), `TableStatementParser` changes (`extractFirstDate` falls through to original behavior for single-date cells), and `StatementParser` changes (fallback only triggers when modal-count split has no dates — existing banks always have dates in structural rows). Fix any failures found.

- [ ] T010 [US2] Add a unit test verifying multi-line merging is a no-op for single-line bank tables in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/StatementParserTest.kt`. Mock `pdfTableExtractor.extractTable()` to return a Kaspi-like clean 4-column table where every row starts with a date (e.g., `["15.03.2024", "-5000.00", "Покупка", "Магазин"]`). Call `extractSampleTableRowsWithContext()` and assert the result is identical to before — no rows merged, correct count of sample rows, header row properly detected. Add a second test: `mergeMultiLineRows handles continuation row with mismatched cell count` — mock a table where row 1 has 4 cells and continuation row 2 has 5 cells. Assert that after merging, the result row has 5 cells (padded parent) with correct concatenation, and no IndexOutOfBoundsException.

**Checkpoint**: All existing bank parsers continue to work correctly. Zero regressions.

---

## Phase 4: User Story 3 — Halyk Bank Multi-line Row Fix (Priority: P2)

**Goal**: Verify that the generic multi-line merging fix also improves Halyk bank parsing where transactions span multiple visual lines.

**Independent Test**: Import a Halyk PDF → verify multi-line transactions are merged and parsed correctly.

### Verification for User Story 3

- [ ] T011 [US3] Add a unit test for Halyk-like multi-line merging in `core/parser/src/test/java/com/atelbay/money_manager/core/parser/StatementParserTest.kt`. Mock `pdfTableExtractor.extractTable()` to return a Halyk-like table with multi-line rows: e.g., row 1: `["01.01.2026", "TX001", "-", "5000.00", "KZT", "Purchase", "Shop"]`, row 2 (continuation): `["", "", "", "", "", "", "Branch: Almaty Center"]`. Call `extractSampleTableRowsWithContext()` and assert the continuation row is merged into row 1 (details cell should contain `"Shop Branch: Almaty Center"`), and the result contains the correct number of merged transaction rows.

**Checkpoint**: Halyk multi-line transactions benefit from the same fix. All three user stories verified.

---

## Phase 5: Polish & Build Verification

**Purpose**: Final validation across all changes

- [ ] T012 Run full project build and lint: `./gradlew assembleDebug test lint`. Verify zero build errors, zero test failures, zero new lint warnings in modified files. Fix any issues found.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — T001, T002, T003 can all start immediately and run in parallel
- **US1 (Phase 2)**: T004 and T005 can start after Phase 1. T006, T007, T008 can run in parallel with T004/T005 (different files) but should be validated after implementation
- **US2 (Phase 3)**: Depends on Phase 2 completion (needs all fixes in place to test regression)
- **US3 (Phase 4)**: Depends on T004 (multi-line merging). Can run in parallel with US2
- **Polish (Phase 5)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Depends on foundational Phase 1 tasks. This is the primary deliverable.
- **US2 (P1)**: Depends on US1 completion — tests verify no regression from US1 changes.
- **US3 (P2)**: Depends on T004 (multi-line merging from US1). Can run in parallel with US2.

### Parallel Opportunities

**Phase 1** (all three tasks are independent — different files):
```
T001 (AmountParser.kt) || T002 (TableStatementParser.kt) || T003 (GeminiServiceImpl.kt)
```

**Phase 2** (tests can run in parallel, implementation is sequential):
```
T004 (PdfTableExtractor.kt) → T005 (StatementParser.kt)
T006 || T007 || T008  (all test files, can run in parallel with each other)
```

**Phase 3-4** (can run in parallel with each other):
```
T009 → T010  (US2 verification)
T011          (US3 verification, can run parallel with US2)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational fixes (T001-T003) — **parallel, ~15 min**
2. Complete Phase 2: US1 implementation (T004-T005) + tests (T006-T008) — **~30 min**
3. **STOP and VALIDATE**: Run `./gradlew :core:parser:test`, deploy to device, import BCC PDF
4. If BCC works → proceed to US2/US3 verification

### Incremental Delivery

1. Phase 1 (T001-T003) → Independent parsing improvements ready
2. Phase 2 (T004-T008) → BCC import works → **MVP complete**
3. Phase 3 (T009-T010) → Regression verified → **Production ready**
4. Phase 4 (T011) → Halyk bonus confirmed
5. Phase 5 (T012) → Full build clean

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- PdfTableExtractor unit tests are @Ignored (need Android runtime) — merging logic is tested indirectly via StatementParser mock tests and on-device androidTest
- No new modules, no new dependencies — all changes within existing `core:parser` and `core:ai`
- Commit after each phase for clean git history
