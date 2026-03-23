# Data Model: Table-Based PDF Parsing

**Feature**: 014-table-pdf-parsing | **Date**: 2026-03-23

## New Entities

### TableParserConfig

Column-index-based configuration for parsing table-extracted PDF data. Shared between core:parser (parsing) and core:ai (generation).

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| bankId | String | Yes | — | Unique identifier for the bank (e.g., "forte", "bereke_ai") |
| bankMarkers | List\<String\> | Yes | — | Text markers used to detect this bank in PDF content |
| dateColumn | Int | Yes | — | 0-based column index containing transaction date |
| amountColumn | Int | Yes | — | 0-based column index containing transaction amount |
| operationColumn | Int? | No | null | 0-based column index for operation/description text |
| detailsColumn | Int? | No | null | 0-based column index for additional details |
| signColumn | Int? | No | null | 0-based column index for +/- sign indicator |
| currencyColumn | Int? | No | null | 0-based column index for currency code |
| dateFormat | String | Yes | — | Java DateTimeFormatter pattern (e.g., "dd.MM.yyyy") |
| amountFormat | String | No | "dot" | Amount format: "dot", "comma_dot", or "space_comma" |
| negativeSignMeansExpense | Boolean | No | true | If true, negative amounts are expenses |
| skipHeaderRows | Int | No | 1 | Number of header rows to skip before data rows |
| deduplicateMaxAmount | Boolean | No | false | If true, keep only max-amount row per (date, details) group |

**Identity**: Unique by `bankId`. When caching, a new config with the same `bankId` replaces the old one.

**Serialization**: `@Serializable` with `@SerialName` annotations for snake_case JSON keys. Stored in DataStore as JSON array string.

**Relationship to ParserConfig**: Parallel model — not a subtype. ParserConfig uses regex patterns; TableParserConfig uses column indices. Both produce `List<ParsedTransaction>` output.

### TableParserConfigList

Container for serializing/deserializing a list of TableParserConfig objects.

| Field | Type | Description |
|-------|------|-------------|
| configs | List\<TableParserConfig\> | All cached table parser configs |

### TableFailedAttempt

Records a failed AI table config generation attempt for retry feedback.

| Field | Type | Description |
|-------|------|-------------|
| config | TableParserConfig | The config that was tried |
| error | String | Error message describing what failed |
| failedRows | List\<String\> | String representations of rows that failed to parse |

### TableParseResult

Result of table-based parsing, returned by StatementParser.

| Field | Type | Description |
|-------|------|-------------|
| transactions | List\<ParsedTransaction\> | Successfully parsed transactions |
| bankId | String? | Detected or configured bank identifier |
| extractedTable | List\<List\<String\>\> | Raw extracted table data (for debug/retry) |

## Modified Entities

### ParseResult (in ParseStatementUseCase)

| Field | Change | Description |
|-------|--------|-------------|
| aiGeneratedTableConfig | ADD (TableParserConfig?) | AI-generated table config to submit upstream |
| sampleTableRows | ADD (List\<List\<String\>>?) | Sample table rows for upstream submission |

### AiMethod (enum in ParseStatementUseCase)

| Value | Change | Description |
|-------|--------|-------------|
| TABLE_GENERATED | ADD | Transactions parsed via AI-generated table config |

### ImportStepEvent (sealed class)

| Event | Change | Fields |
|-------|--------|--------|
| TableExtracted | ADD | rowCount: Int, columnCount: Int |
| TableConfigAttempt | ADD | source: String, bankId: String? |
| TableConfigResult | ADD | source: String, txCount: Int |
| AiTableConfigRequest | ADD | attempt: Int |
| AiTableConfigResponse | ADD | attempt: Int, bankId: String |
| AiTableConfigParseResult | ADD | attempt: Int, txCount: Int |

### UserPreferences (DataStore keys)

| Key | Change | Type | Description |
|-----|--------|------|-------------|
| KEY_AI_TABLE_PARSER_CONFIGS | ADD | stringPreferencesKey | Serialized TableParserConfigList JSON |

## Data Flow

```
PDF bytes
  │
  ├─ PdfTableExtractor.extractTable(bytes)
  │   └─ Returns: List<List<String>> (rows × columns)
  │
  ├─ StatementParser.tryParseTable(): match tableConfigs by bankMarkers
  │   └─ For each cached config, check if any bankMarker appears in extracted cell text
  │   └─ Returns: first matching TableParserConfig's parse result, or null
  │
  ├─ TableStatementParser.parse(table, config)
  │   └─ Returns: List<ParsedTransaction>
  │
  └─ GeminiServiceImpl.generateTableParserConfig(header, sampleRows, attempts)
      └─ Returns: TableParserConfig
```

## Validation Rules

- `dateColumn` and `amountColumn` must be non-negative integers
- `dateFormat` must be a valid `DateTimeFormatter` pattern
- `amountFormat` must be one of: "dot", "comma_dot", "space_comma"
- `skipHeaderRows` must be >= 0
- Column indices must not exceed the actual column count of extracted table rows (enforced at parse time, not config time)
- `bankMarkers` must be non-empty list
