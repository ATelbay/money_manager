# Data Model: Fix BCC Bank PDF Table Parsing

No new entities or schema changes are introduced. This feature modifies behavior within existing parsing pipeline classes.

## Modified Entities

### TableParserConfig (existing)

**File**: `core/model/src/main/java/.../TableParserConfig.kt`

- `amountFormat: String` — gains new valid value `"space_dot"` alongside existing `"dot"`, `"comma_dot"`, `"space_comma"`. The field is a free-form String (not an enum), so no model code change is needed. The new value is handled by `AmountParser.parseAmount()`.

### AmountParser (existing)

**File**: `core/parser/src/main/java/.../AmountParser.kt`

New `when` branch:
- `"space_dot"` → strips all characters except `\d`, `.`, `-` → `toDouble()`

### PdfTableExtractor — Row Model (implicit)

The output type `List<List<String>>` remains unchanged. After merging, each `List<String>` represents a logical transaction row (possibly assembled from 2+ visual rows). No structural change to the data model.

## State Transitions

No state machines or lifecycle changes. The parsing pipeline remains stateless and functional:

```
PDF bytes → PdfTableExtractor.extractTable() → [mergeMultiLineRows] → List<List<String>>
         → StatementParser.extractSampleTableRowsWithContext() → [date-fallback] → TableExtractionResult
         → GeminiService.generateTableParserConfig() → TableParserConfig
         → TableStatementParser.parse() → [extractFirstDate] → List<ParsedTransaction>
```
