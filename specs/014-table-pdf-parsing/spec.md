# Feature Specification: Table-Based PDF Parsing with Simplified AI Config

**Feature Branch**: `014-table-pdf-parsing`
**Created**: 2026-03-23
**Status**: Draft
**Input**: Replace failing AI regex generation with table-structure extraction and simplified column-index configuration

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Import bank statement from table-structured PDF (Priority: P1)

A user selects a PDF bank statement that contains tabular transaction data (e.g., Forte, Bereke, Eurasian banks). The system extracts the table structure from the PDF, identifies columns (date, amount, operation, etc.), and imports transactions — without requiring a pre-built regex configuration for that specific bank.

**Why this priority**: This is the core value proposition. Currently, PDFs from unsupported banks fail at AI regex generation because Gemini Flash cannot reliably produce working regex patterns from flattened PDF text. Table extraction with column-index config is dramatically simpler for the AI to solve.

**Independent Test**: Can be tested by importing a PDF from any table-structured bank statement and verifying transactions are correctly extracted with date, amount, and description fields.

**Acceptance Scenarios**:

1. **Given** a user imports a PDF from a new (unconfigured) bank with table-structured data, **When** the system processes it, **Then** transactions are extracted with correct dates, amounts, and descriptions — and the user sees them in the import review screen.
2. **Given** a user imports a PDF from a bank with an existing regex config, **When** the system processes it, **Then** the existing regex path succeeds as before (no regression).
3. **Given** a user imports a PDF where table extraction produces data but the AI's first column-index guess is wrong, **When** the system retries, **Then** the retry includes specific failed rows and parse errors so the AI can self-correct (up to 3 attempts).

---

### User Story 2 - Cached table configs speed up repeat imports (Priority: P2)

A user imports a second PDF from the same bank. The system recognizes the bank via markers in the previously AI-generated table config and applies it immediately — no AI call needed.

**Why this priority**: Caching is essential for performance and cost. Without it, every import triggers an AI call even for previously seen banks.

**Independent Test**: Can be tested by importing two PDFs from the same bank sequentially; the second import should skip AI generation and parse instantly.

**Acceptance Scenarios**:

1. **Given** a table config was previously generated and cached for bank X, **When** the user imports another PDF from bank X, **Then** the cached config is used without an AI call and transactions are parsed successfully.
2. **Given** a cached table config exists but fails on a new PDF variant from the same bank, **When** parsing fails, **Then** the system falls through to AI generation for a fresh config.

---

### User Story 3 - Graceful fallback to existing paths (Priority: P2)

A user imports a PDF that is not table-structured (e.g., free-form text statement, scanned image). The system's table extraction returns no usable data, and the system falls back to the existing AI regex generation or full AI parse paths.

**Why this priority**: Backward compatibility is critical — the new table path must not break existing import flows for non-table PDFs.

**Independent Test**: Can be tested by importing a non-table PDF and verifying it falls through to regex AI or full AI parse without errors.

**Acceptance Scenarios**:

1. **Given** a user imports a non-table PDF, **When** table extraction finds fewer than 2 rows, **Then** the system skips the table path entirely and proceeds to AI regex generation.
2. **Given** a user imports a PDF where both table and regex AI paths fail, **When** full AI parse is enabled, **Then** the system falls through to multimodal AI parsing as the ultimate fallback.

---

### User Story 4 - Debug visibility for table parsing steps (Priority: P3)

A developer or power user views the debug import sheet and sees detailed progress events for the table extraction and config generation steps — including row/column counts, config attempts, and parse results.

**Why this priority**: Observability is important for diagnosing issues but not user-facing for most users.

**Independent Test**: Can be tested by enabling the debug sheet during import and verifying that table-related events appear in the event log.

**Acceptance Scenarios**:

1. **Given** a user imports a table-structured PDF with the debug sheet visible, **When** the system processes it, **Then** events show: table extracted (row/column count), config attempt (source + bank ID), and parse result (transaction count).
2. **Given** the AI generates a table config on retry, **When** viewing the debug sheet, **Then** each retry attempt is logged with its attempt number and outcome.

---

### Edge Cases

- What happens when a PDF has multiple tables (e.g., summary table + transaction table)? The system extracts all rows as a single table; the AI config's `skipHeaderRows` parameter allows skipping non-transaction rows at the top.
- What happens when table columns are misaligned across pages? Column boundary detection clusters X-positions across all pages, producing a consistent column grid.
- What happens when a cell spans multiple lines within a row? Characters with close Y-coordinates (within tolerance) are grouped into the same row.
- What happens when the AI returns column indices that are out of bounds for the extracted table? The parser skips rows where column access fails and logs a warning.
- What happens when amount values use different formats within the same PDF? The configured amount format applies uniformly; rows that fail parsing are skipped.
- What happens when the PDF has no text layer (scanned document)? Table extraction returns empty results, and the system falls through to existing paths.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST extract table structure from PDF files by analyzing character positions (X/Y coordinates), reconstructing rows and columns without requiring external table-extraction libraries.
- **FR-002**: System MUST support a column-index-based configuration model that specifies which column contains date, amount, operation, details, sign, and currency — using integer indices instead of regex patterns.
- **FR-003**: System MUST ask the AI to identify column indices and data formats from sample table rows, rather than asking it to generate regex patterns.
- **FR-004**: System MUST insert the table parsing path between existing cached regex configs and AI regex generation in the import pipeline, preserving all existing steps.
- **FR-005**: System MUST cache successfully generated table configs locally (separate from regex configs) and reuse them for subsequent imports from the same bank.
- **FR-006**: System MUST retry AI table config generation up to 3 times, providing the AI with specific failed rows and parse errors from previous attempts.
- **FR-007**: System MUST fall back to the existing AI regex generation path if table extraction produces fewer than 2 rows or all table config attempts fail.
- **FR-008**: System MUST support all existing amount formats (dot, comma_dot, space_comma), date formats, sign detection modes, and deduplication logic in the table parser — matching existing regex parser capabilities.
- **FR-009**: System MUST emit progress events for each table parsing step (extraction, config attempt, config result, AI request/response) for debug UI display.
- **FR-010**: System MUST preserve backward compatibility — all existing regex-based parsing continues to work unchanged.

### Key Entities

- **TableParserConfig**: Column-index-based configuration for parsing extracted table data. Contains bank identifier, bank markers for detection, column indices for each data field (date, amount, operation, details, sign, currency), date/amount format settings, and parsing behavior flags (negative sign interpretation, header row skipping, deduplication). Shared between the parser and AI modules.
- **Extracted Table**: A two-dimensional structure of cell strings (rows x columns) reconstructed from PDF character positions. Used as input to both the table parser and AI config generation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: PDFs from table-structured banks (at least 3 of the 5 supported banks) are successfully parsed via the table path, producing the same or more transactions than the current regex path.
- **SC-002**: AI table config generation succeeds within 3 retry attempts for at least 80% of table-structured PDFs from new/unsupported banks.
- **SC-003**: Repeat imports from the same bank using cached table configs complete without any AI calls.
- **SC-004**: Non-table PDFs fall through to existing paths with zero impact on current parsing success rates.
- **SC-005**: All existing bank statement imports (5 supported banks) continue to work with no regressions.

## Assumptions

- PdfBox-Android's TextPosition API provides sufficient coordinate precision (X/Y in points) to reconstruct table structure from typical bank statement PDFs.
- Bank statement PDFs use text-based content (not scanned images) — OCR is out of scope.
- A Y-coordinate tolerance of ~2pt and X-coordinate clustering epsilon of ~10pt are reasonable defaults for row/column detection, based on typical PDF rendering.
- The AI (Gemini 2.5 Flash) can reliably answer "which column is what?" given sample table rows — this is dramatically simpler than generating regex patterns.
- Table configs are cached locally using the same mechanism as existing AI regex config caching.

## Constraints

- No new dependencies — the solution uses only PdfBox-Android's TextPosition API which is already available.
- Tabula (tabula-java) is NOT Android-compatible (uses java.awt) and must not be used.
- The table parsing path must not slow down the happy path for banks with existing regex configs — regex configs are tried first.
- The table config model must be accessible from both the parser and AI modules, requiring placement in a shared location.
