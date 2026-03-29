---
description: "Importing bank statements into Money Manager: PDF parsing (PdfBox), RegEx parser, Gemini AI fallback, BankDetector, parsing strategy, supported banks"
---

# PDF & AI Statement Parsing

## Context

Feature for automatically importing transactions from a bank's PDF statement. Two-level strategy: RegEx parsing (free, fast) with fallback to Gemini AI.

**Key files:**
- `core/parser/src/.../PdfTextExtractor.kt` — extract text from PDF (PdfBox-Android 2.0.27.0)
- `core/parser/src/.../BankDetector.kt` — identify bank by markers in text
- `core/parser/src/.../RegexStatementParser.kt` — parse rows using regex pattern from config
- `core/parser/src/.../StatementParser.kt` — facade: extract → detect → regex parse
- `core/ai/src/.../GeminiService.kt` — Gemini AI interface
- `core/ai/src/.../GeminiServiceImpl.kt` — send blobs (PDF/image) to Gemini, responseMimeType=JSON
- `core/remoteconfig/src/.../ParserConfigProvider.kt` — regex pattern config (Firebase Remote Config + defaults)
- `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ParseStatementUseCase.kt` — orchestration: RegEx → fallback Gemini → deduplication
- `domain/import/src/main/java/com/atelbay/money_manager/domain/importstatement/usecase/ImportTransactionsUseCase.kt` — save to Room, fallback to "Other" category
- `core/model/src/.../TransactionOverride.kt` — user edits per-transaction

## Parsing Strategy

```
PDF bytes ──→ PdfTextExtractor ──→ raw text
                                      │
                                      ▼
                              BankDetector (markers: "Kaspi Gold", etc.)
                                      │
                              ┌───────┴────────┐
                              ▼                ▼
                      RegEx parser       Unknown bank
                              │                │
                         ┌────┴────┐           │
                         ▼         ▼           ▼
                      Success    Fail ──→ Gemini AI (fallback)
                         │
                         ▼
                  ParsedTransaction[]

Image bytes ──────────────────────→ Gemini AI (directly)
```

## User Flow

```
Home → Import icon → Import screen
  ├─ Select PDF (ActivityResultContracts.OpenDocument)
  └─ Take photo (TakePicturePreview)
      ↓
  PDF → RegEx parsing → if not recognized → Gemini AI
  Photo → JPEG bytes (image/jpeg) → Gemini AI
      ↓
  ImportResult (transactions + duplicates + errors)
      ↓
  Preview screen:
    ├─ Account selector (dropdown)
    ├─ Edit each transaction
    └─ "Import (N)" button
      ↓
  ImportTransactionsUseCase → Room DB + balance update
      ↓
  Success screen
```

## Models

- **ParsedTransaction** — recognized transaction (date, amount, type, details, categoryId, confidence, needsReview, uniqueHash)
- **ImportResult** — parsing result (total, newTransactions, duplicates, errors)
- **ImportState** — sealed interface (Idle, Parsing, Preview, Importing, Success, Error)
- **TransactionOverride** — user edits (amount?, type?, details?, date?, categoryId?)

## Supported Banks (RegEx)

| Bank | bank_id | Row format | Key flags |
|------|---------|------------|-----------|
| Kaspi Gold | `kaspi` | `DD.MM.YY [+-] amount ₸ Operation Details` | positional groups |
| Freedom Bank | `freedom` | `DD.MM.YYYY [+-] amount ₸ CURR Operation Details` | `use_sign_for_type`, `join_lines`, `comma_dot` |
| Forte Bank | `forte` | `DD.MM.YYYY [-]amount KZT Operation Details` | `negative_sign_means_expense`, `join_lines` |
| Bereke Bank | `bereke` | `DD.MM.YYYY TransactionType Description amount CURR [-]amount [**** XXXX]` | `use_named_groups`, `negative_sign_means_expense`, `comma_dot` |
| Eurasian Bank | `eurasian` | `DD.MM.YYYY [HH:mm:ss] OperationType Details txAmt CURR [+-]accAmt [Card/Account]` | `use_named_groups`, `negative_sign_means_expense`, `deduplicate_max_amount` |

### ParserConfig — new fields (Forte/Bereke/Eurasian)

| Field | Type | Purpose |
|-------|------|---------|
| `negative_sign_means_expense` | Boolean | sign="-" → EXPENSE, otherwise → INCOME |
| `use_named_groups` | Boolean | Regex uses named groups: `date`, `sign`, `amount`, `operation`, `details` |
| `deduplicate_max_amount` | Boolean | After parsing: for each group (date, details) keep only the row with max amount. Used for Eurasian where each transaction generates 2-3 rows |

### Eurasian — specifics
- Each foreign-currency transaction produces 3 rows (card debit, mirrored credit, actual KZT debit)
- `deduplicate_max_amount` keeps only the row with the largest amount (= actual KZT expense)
- Known limitation: 2 purchases at the same merchant on the same day → collapsed into one transaction

## Process

### Adding a new bank
1. Add bank markers to `BankDetector.kt`
2. Add regex pattern to Firebase Remote Config (or to defaults in `ParserConfigProvider`)
3. Add unit tests for the new format in `core/parser/src/test/`
4. Check edge cases: different date formats, amounts with spaces, negative amounts

### Modifying AI fallback
1. Gemini prompt is defined in `GeminiServiceImpl.kt`
2. Response format — JSON (responseMimeType="application/json")
3. Model: Gemini 2.5 Flash via Firebase AI SDK

## testTag naming

- `import:screen`, `import:selectPdf`, `import:takePhoto`
- `import:preview`, `import:accountSelector`, `import:importButton`
- `import:loading`, `import:successCount`, `import:errorMessage`

## Quality Bar

- Deduplication by `uniqueHash` (TransactionHashGenerator) — the same transaction is never imported twice
- Fallback to "Other" category if the AI-suggested category is not found in the DB
- `needsReview = true` for transactions with low confidence
- All parsing errors are logged but do not block importing the remaining transactions

## Anti-patterns

- Do NOT send PDF to Gemini if the RegEx parser succeeded — unnecessary cost
- Do NOT block import because of one invalid row — skip and continue
- Do NOT keep PDF/image bytes in memory longer than needed — release after parsing
