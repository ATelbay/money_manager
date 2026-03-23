# Research: Table-Based PDF Parsing

**Feature**: 014-table-pdf-parsing | **Date**: 2026-03-23

## R1: PdfBox-Android TextPosition API for Table Extraction

**Decision**: Use `PDFTextStripper.writeString(String, List<TextPosition>)` override to collect character coordinates, then cluster into rows/columns.

**Rationale**: PdfBox-Android's `TextPosition` provides `getXDirAdj()` (X coordinate) and `getYDirAdj()` (Y coordinate) for each character. By overriding `writeString` in a custom `PDFTextStripper` subclass, we intercept all text with position data before it's flattened to a string. This is the same API that tabula-java uses internally — we just implement the clustering ourselves.

**Implementation approach**:
- Subclass `PDFTextStripper`, override `writeString(text: String, textPositions: List<TextPosition>)`
- Collect all `TextPosition` objects across all pages into a flat list
- After processing all pages, cluster into rows by Y-coordinate (tolerance ~2pt accounts for baseline variations)
- Sort all X-start-positions across all rows, detect column boundaries by finding gaps > threshold (~10pt)
- Assign each character to the nearest column boundary, concatenate per cell
- Return `List<List<String>>` (rows × columns)

**Alternatives considered**:
- Tabula-java: Uses java.awt.geom internally — NOT available on Android. Would crash at runtime.
- PdfBox `PDFTextStripperByArea`: Requires pre-defining rectangular regions — we don't know column positions upfront. Chicken-and-egg problem.
- OCR (Tesseract): Overkill for text-based PDFs, massive dependency, slow.

**Key risks**:
- PDFs with merged cells or irregular layouts may produce noisy results → mitigated by the AI config step which can handle imperfect data
- Multi-page PDFs may have different Y-offsets per page → must normalize Y by page or process page-by-page and concatenate rows

## R2: Column Boundary Detection Algorithm

**Decision**: Sorted-gap algorithm instead of DBSCAN.

**Rationale**: Bank statement tables have 4-7 columns with clear visual gaps (typically 20-50pt between columns vs <2pt within a cell's characters). A simple algorithm suffices:

1. Collect the X-start of each "word" (first character after a gap > 3pt)
2. Build a histogram of X-start positions (bin width ~5pt)
3. Find histogram peaks — these are column start positions
4. Alternatively: sort unique X-starts, compute pairwise gaps, split at gaps > median_gap * 2

**Alternatives considered**:
- DBSCAN: Correct for arbitrary point clustering but overkill. Requires epsilon parameter tuning and has O(n log n) complexity.
- Fixed-width columns: Would fail for PDFs with different layouts.
- K-means: Requires knowing K (column count) upfront — unknown.

## R3: Serialization in core/model

**Decision**: Add `kotlinx-serialization` plugin and dependency to `core/model/build.gradle.kts`.

**Rationale**: `TableParserConfig` needs `@Serializable` for:
1. DataStore caching (serialize to JSON string)
2. AI response parsing (deserialize Gemini's JSON output)
3. Sharing between `core:parser` and `core:ai`

Currently `core:model` has only `kotlinx.datetime`. Adding serialization is minimal:
- Add `alias(libs.plugins.kotlin.serialization)` to plugins block
- Add `implementation(libs.kotlinx.serialization.json)` to dependencies

**Impact**: All modules depending on `core:model` will transitively get kotlinx-serialization-json on the classpath. This is acceptable — it's a small, common library already used in multiple modules.

**Alternative considered**: Put `TableParserConfig` in `core:remoteconfig` next to `ParserConfig`. Rejected because `core:parser` already depends on `core:remoteconfig` and `core:ai` depends on `core:remoteconfig` too — it would technically work but semantically `TableParserConfig` is a domain/model concept, not a remote config concept.

## R4: AI Prompt Design for Table Config Generation

**Decision**: Show sample table rows as JSON array of arrays; ask for column indices and format metadata.

**Rationale**: The current regex prompt is ~2000 tokens with complex rules about Java named groups, ReDoS safety, and regex syntax. The table config prompt needs only:
- Sample data: 5-10 rows as `[["2024-01-15", "Grocery Store", "-15,230.50", "KZT"], ...]`
- Questions: "date_column index?", "amount_column index?", "date_format?", "amount_format (dot|comma_dot|space_comma)?"
- No regex syntax, no ReDoS, no named groups — just integers and enum strings

Expected prompt size: ~500 tokens (4x smaller). Expected reliability: much higher — Gemini Flash handles classification/extraction tasks well.

**Retry strategy**: On failure, include:
- The previous config that failed
- Specific error message (e.g., "date parsing failed on row 3: '15.01.2024' doesn't match format 'yyyy-MM-dd'")
- The specific rows that failed to parse
- This gives the AI concrete evidence to self-correct (e.g., switch date format)

## R5: Pipeline Integration Order

**Decision**: Table path inserts between step 2 (cached AI regex configs) and step 3 (AI regex generation).

**Rationale**:
- Steps 1-2 (remote config regex + cached AI regex) are fast and free — always try first
- Table extraction + cached table configs (step 2.5a) is also fast — no AI call needed
- AI table config generation (step 2.5b) is cheaper/simpler than AI regex generation (step 3)
- AI regex generation (step 3) remains as legacy fallback
- Full AI parse (step 4) remains as ultimate fallback

Updated pipeline:
```
1. Remote Config regex configs → tryParsePdf()
2. Cached AI regex configs → tryParsePdf(additionalConfigs)
3. Extract table → try cached TableParserConfigs
4. AI generates TableParserConfig (3 retries)
5. AI regex generation (3 retries) — existing, kept as fallback
6. Full AI multimodal parse — existing ultimate fallback
```

## R6: Amount Format "dot" Handling

**Decision**: Add explicit `"dot"` format handling to the shared amount parser.

**Rationale**: Current `RegexStatementParser.parseAmount()` handles `"comma_dot"` and default (`"space_comma"`). The `ParserConfig` model has `amountFormat` defaulting to `"space_comma"`. The `TableParserConfig` defaults to `"dot"` (simplest format — just strip non-numeric except dot and minus). Need to add:
- `"dot"`: strip everything except digits, `.`, `-` → `toDouble()`
- `"comma_dot"`: strip `,` (thousand separator) → `toDouble()`
- `"space_comma"`: strip spaces, replace `,` with `.` → `toDouble()`
