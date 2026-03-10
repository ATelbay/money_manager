# Data Model: PDF Import Audit & Bereke Fix

**Feature**: 003-audit-pdf-import | **Date**: 2026-03-10

> This is an audit/fix feature. No new data models are introduced. This document captures the existing entities, their constraints, and the specific changes being made.

---

## Existing Entities

### ParserConfig
**Location**: `core/remoteconfig/src/main/java/com/atelbay/money_manager/core/remoteconfig/ParserConfig.kt`

| Field | Type | Description | Constraint |
|-------|------|-------------|------------|
| `bankId` | String | Unique bank identifier | Non-empty; used as map key |
| `bankMarkers` | List\<String\> | Strings that identify bank in PDF text | Non-empty; case-insensitive matched |
| `transactionPattern` | String | Regex pattern for one transaction line | Must be valid Kotlin Regex |
| `dateFormat` | String | Java `DateTimeFormatter` pattern | Must parse dates in the PDF |
| `operationTypeMap` | Map\<String, String\> | Operation name → "income"/"expense" | Values must be "income" or "expense" |
| `skipPatterns` | List\<String\> | Exact strings; lines containing them are removed | Literal match (Regex.escape applied) |
| `joinLines` | Boolean | Join continuation lines (multi-line descriptions) | Default: false |
| `amountFormat` | String | "comma_dot" or "space_comma" | Default: "space_comma" |
| `useSignForType` | Boolean | "+" = income, "-" = expense | Overrides operationTypeMap |
| `negativeSignMeansExpense` | Boolean | "-" = expense, no sign = income | Used when `useSignForType=false` |
| `useNamedGroups` | Boolean | Regex uses named capture groups | Required for Bereke, Eurasian |
| `deduplicateMaxAmount` | Boolean | Keep highest amount per (date, details) | Used for Eurasian triplet dedup |

**Changes in this feature**:
- `transactionPattern` for `bereke` bank will be updated (exact change TBD after P0 diagnosis)
- `skipPatterns` for `bereke` may be extended with additional real-PDF header/footer lines

---

### ParsedTransaction
**Location**: `core/model/src/main/java/com/atelbay/money_manager/core/model/ParsedTransaction.kt`

| Field | Type | Description |
|-------|------|-------------|
| `date` | LocalDate | Transaction date |
| `amount` | Double | Absolute amount (always positive) |
| `type` | TransactionType | INCOME or EXPENSE |
| `operationType` | String | Raw operation string from PDF |
| `details` | String | Merchant / description text |
| `categoryId` | Long? | Resolved DB category (null = unassigned) |
| `suggestedCategoryName` | String | Fallback category name suggestion |
| `confidence` | Float | 1.0 for regex, <1.0 for AI |
| `needsReview` | Boolean | true when confidence < 0.7 (AI only) |
| `uniqueHash` | String | Hash of (date, amount, type, details) |

**No changes** — entity is read-only from parser's perspective.

---

### Share Intent → Navigation

| Component | Role |
|-----------|------|
| `MainActivity.extractPdfUri()` | Extracts PDF URI from ACTION_SEND or ACTION_VIEW intent |
| `NavigationAction.OpenImport(pdfUri: String)` | Sealed interface, carries URI string |
| `PendingNavigationManager` | Singleton StateFlow queue; holds one pending action at a time |
| `MoneyManagerApp` LaunchedEffect | Consumes action when onboarding complete and NavHost settled |

**No changes** — verified correct for all 3 app states.

---

## Integration Test Fixture Structure

```
core/parser/src/test/resources/
├── bereke_statement.pdf    # Source: project root bereke_statement.pdf
├── gold_statement.pdf      # Source: project root gold_statement.pdf  (Kaspi)
├── freedom_statement.pdf   # Source: project root freedom_statement.pdf
├── forte_statement.pdf     # Source: project root forte_statement.pdf
└── eurasian_statement.pdf  # Source: project root eurasian_statement.pdf
```

**Constraint**: PDF files must be committed to version control alongside the test sources. CI must access them without network.

---

## Config Delivery State Transitions

```
App install / launch
    └─► Remote Config fetch (async, 1h interval)
            ├─ SUCCESS → ParserConfigProvider returns Remote Config values
            └─ FAILURE → ParserConfigProvider returns local fallback (default_parser_config.json)

Fix deployment:
    Step 1: Ship app update with fixed default_parser_config.json  (covers offline + new installs)
    Step 2: Update Firebase Remote Config parser_configs key       (covers existing installs immediately)
```
