# Feature Specification: Fix BCC Bank PDF Table Parsing

**Feature Branch**: `015-bcc-table-parsing`
**Created**: 2026-03-25
**Status**: Draft
**Input**: Fix BCC Bank (Банк ЦентрКредит) PDF import — 0 transactions parsed via table path due to multi-line rows, incorrect sample row detection, missing amount format, and double-date cells.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - BCC Bank Statement Import (Priority: P1)

A user selects a BCC Bank PDF statement for import. The system extracts transaction rows from the PDF table, sends a representative sample to Gemini AI for column-index configuration, and parses all transactions correctly — including amounts, dates, descriptions, and transaction types (income vs expense).

**Why this priority**: This is the core user-facing bug. BCC users currently get 0 transactions on import, making the feature completely non-functional for this bank.

**Independent Test**: Import a BCC PDF statement and verify that all transactions appear in the import preview with correct dates, amounts, descriptions, and income/expense classification.

**Acceptance Scenarios**:

1. **Given** a BCC PDF statement with 4+ transactions across multiple pages, **When** the user initiates import, **Then** all transactions are parsed and displayed in the import preview with correct dates, amounts, and descriptions.
2. **Given** a BCC transaction with a long description spanning 3-6 visual lines in the PDF, **When** the PDF is parsed, **Then** the description is merged into a single transaction row and the full text is available.
3. **Given** a BCC transaction amount formatted as `100 000.00` (space thousands separator, dot decimal), **When** the amount is parsed, **Then** it is correctly interpreted as `100000.00`.
4. **Given** a BCC date cell containing two dates like `2024-12-31 2024-12-31`, **When** the date is parsed, **Then** the first date `2024-12-31` is extracted and used.
5. **Given** a BCC transaction with negative amount `-10 000.00`, **When** parsed, **Then** it is classified as an expense. A positive amount like `100 000.00` is classified as income.

---

### User Story 2 - No Regression for Existing Banks (Priority: P1)

Users importing statements from Kaspi, Forte, Bereke, Eurasian, and Freedom banks continue to get correct parsing results after the BCC fixes are applied.

**Why this priority**: Equal to P1 because breaking existing working banks is unacceptable. The multi-line merging and sample row detection changes must be backward-compatible.

**Independent Test**: Import a Kaspi and Forte PDF statement (single-line row banks) and verify the same number of transactions are parsed as before the fix.

**Acceptance Scenarios**:

1. **Given** a Kaspi PDF statement (single-line rows), **When** imported after the fix, **Then** all transactions parse identically to before — no rows incorrectly merged, no transactions lost.
2. **Given** a Forte PDF statement, **When** imported, **Then** parsing results are identical to before.
3. **Given** a Bereke PDF statement (multi-column, named groups), **When** imported, **Then** parsing results are identical to before.

---

### User Story 3 - Halyk Bank Multi-line Row Fix (Priority: P2)

Halyk Bank also has multi-line transaction rows. The same multi-line merging fix that enables BCC should also improve Halyk parsing.

**Why this priority**: Secondary benefit — Halyk multi-line issue is a known problem but not the primary driver of this fix.

**Independent Test**: Import a Halyk PDF statement and verify that multi-line transactions are now merged correctly and more transactions are parsed than before.

**Acceptance Scenarios**:

1. **Given** a Halyk PDF with multi-line transaction rows, **When** imported after the fix, **Then** continuation rows are merged into their parent transaction rows.
2. **Given** a Halyk transaction where the description spans 2+ lines, **When** parsed, **Then** the full description text is captured in a single transaction.

---

### Edge Cases

- What happens when a BCC PDF has only 1-2 transactions? The sample row detection should still find them and send them to the AI for configuration.
- What happens when the date cell contains extra text beyond two dates (e.g. currency suffix like "KZT" merged in)? The date extractor should still find and extract the first valid date.
- What happens when a continuation row has more non-empty cells than the parent row? Merging should still work based on the absence of a date pattern in the first cell.
- What happens when a bank's first cell legitimately contains a short non-date string (not a continuation)? The merging heuristic must rely on date-pattern detection, not just cell length.
- What happens when all rows have the same number of non-empty cells? The sample detection fallback (date-pattern scan) should correctly identify transaction rows.
- What happens when a PDF has no recognizable date patterns in any row? The system falls back gracefully (no crash) and returns 0 transactions as before.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The PDF table extractor MUST merge continuation rows into their parent transaction rows. A continuation row is identified by the absence of a date-like pattern (e.g., `\d{2,4}[-./]\d{2}[-./]`) in its first cell.
- **FR-002**: The merging MUST concatenate each cell of the continuation row to the corresponding cell of the parent row, separated by a space.
- **FR-003**: The merging MUST NOT alter rows for banks with single-line transactions (e.g., Kaspi, Forte). If every row starts with a date pattern, no merging occurs.
- **FR-004**: The sample row detection MUST correctly identify transaction rows even when metadata rows outnumber transaction rows. If the modal-count heuristic yields rows without any date-like strings, a fallback strategy MUST scan for rows containing date patterns in any cell.
- **FR-005**: The system MUST support a `space_dot` amount format that interprets space as a thousands separator and dot as a decimal separator (e.g., `100 000.00` -> `100000.00`).
- **FR-006**: The AI prompt for generating table parser configurations MUST document `space_dot` as a valid `amount_format` option.
- **FR-007**: The date parser MUST handle cells containing multiple dates by extracting the first valid date-like substring.
- **FR-008**: The multi-line row merging MUST be applied before sample row extraction, so Gemini receives properly merged transaction rows.

### Key Entities

- **TableParserConfig**: Configuration for parsing a specific bank's table format. The `amountFormat` field (String) gains a new valid value: `"space_dot"`.
- **Table Row**: A list of cell strings extracted from one visual line of the PDF. After merging, represents one logical transaction.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: BCC Bank PDF statements with 4+ transactions are parsed with 100% of transactions extracted (up from current 0%).
- **SC-002**: Existing bank imports (Kaspi, Forte, Bereke, Eurasian, Freedom) produce identical parsing results before and after the fix — zero regressions.
- **SC-003**: Amounts in `space_dot` format (e.g., `100 000.00`, `-10 000.00`) are parsed correctly to their numeric values.
- **SC-004**: Multi-line transactions with descriptions spanning up to 6 visual lines are merged into a single transaction row.
- **SC-005**: All existing parser unit and instrumented tests continue to pass.

## Assumptions

- The column boundary detection algorithm (fixed in commit 55ecebd) produces correct column positions for the BCC PDF transaction table area. If column boundaries are still incorrect for BCC, the multi-line merging will work on incorrectly sliced cells — but this is out of scope per the constraint "don't modify column boundary detection."
- BCC uses ISO date format `yyyy-MM-dd`. Gemini AI will correctly identify this from the sample rows.
- The `negativeSignMeansExpense` flag (already supported) is sufficient for BCC's sign convention (negative amount = expense).
- Halyk's multi-line row pattern is similar enough to BCC's that the same date-pattern-based merging heuristic works for both banks.

## Scope Boundaries

**In scope**:
- Multi-line row merging in PdfTableExtractor
- Sample row detection improvement in StatementParser
- `space_dot` amount format in AmountParser
- First-date extraction in TableStatementParser
- Gemini prompt update for `space_dot`
- Unit tests for new logic

**Out of scope**:
- Column boundary detection changes (already fixed)
- Regex parsing path changes
- Hardcoded BCC parser config (AI generates it)
- 3-attempt retry loop changes in ParseStatementUseCase
- Other bank-specific fixes beyond what the generic improvements enable
