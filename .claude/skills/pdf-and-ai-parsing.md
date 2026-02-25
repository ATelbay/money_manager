---
description: "Импорт банковских выписок в Money Manager: PDF парсинг (PdfBox), RegEx парсер, Gemini AI fallback, BankDetector, стратегия парсинга, поддерживаемые банки"
---

# PDF & AI Statement Parsing

## Context

Фича для автоматического импорта транзакций из PDF-выписки банка. Двухуровневая стратегия: RegEx-парсинг (бесплатно, быстро) с fallback на Gemini AI.

**Ключевые файлы:**
- `core/parser/src/.../PdfTextExtractor.kt` — извлечение текста из PDF (PdfBox-Android 2.0.27.0)
- `core/parser/src/.../BankDetector.kt` — определение банка по маркерам в тексте
- `core/parser/src/.../RegexStatementParser.kt` — парсинг строк по regex-паттерну из конфига
- `core/parser/src/.../StatementParser.kt` — фасад: extract → detect → regex parse
- `core/ai/src/.../GeminiService.kt` — интерфейс Gemini AI
- `core/ai/src/.../GeminiServiceImpl.kt` — отправка blobs (PDF/image) в Gemini, responseMimeType=JSON
- `core/remoteconfig/src/.../ParserConfigProvider.kt` — конфиг regex-паттернов (Firebase Remote Config + defaults)
- `feature/import/src/.../domain/usecase/ParseStatementUseCase.kt` — оркестрация: RegEx → fallback Gemini → дедупликация
- `feature/import/src/.../domain/usecase/ImportTransactionsUseCase.kt` — сохранение в Room, fallback на категорию "Другое"
- `core/model/src/.../TransactionOverride.kt` — пользовательские правки per-transaction

## Стратегия парсинга

```
PDF bytes ──→ PdfTextExtractor ──→ raw text
                                      │
                                      ▼
                              BankDetector (маркеры: "Kaspi Gold", etc.)
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
  ├─ Выбрать PDF (ActivityResultContracts.OpenDocument)
  └─ Сделать фото (TakePicturePreview)
      ↓
  PDF → RegEx парсинг → если не распознан → Gemini AI
  Фото → JPEG bytes (image/jpeg) → Gemini AI
      ↓
  ImportResult (transactions + duplicates + errors)
      ↓
  Preview screen:
    ├─ Выбор счёта (dropdown)
    ├─ Редактирование каждой транзакции
    └─ Кнопка "Импорт (N)"
      ↓
  ImportTransactionsUseCase → Room DB + balance update
      ↓
  Success screen
```

## Модели

- **ParsedTransaction** — распознанная транзакция (date, amount, type, details, categoryId, confidence, needsReview, uniqueHash)
- **ImportResult** — результат парсинга (total, newTransactions, duplicates, errors)
- **ImportState** — sealed interface (Idle, Parsing, Preview, Importing, Success, Error)
- **TransactionOverride** — пользовательские правки (amount?, type?, details?, date?, categoryId?)

## Поддерживаемые банки (RegEx)

| Банк | bank_id | Формат строки | Операции |
|------|---------|---------------|----------|
| Kaspi Gold | `kaspi` | `DD.MM.YY  [+-] сумма ₸  Операция  Детали` | Покупка, Перевод, Пополнение |

## Process

### Добавление нового банка
1. Добавить маркеры банка в `BankDetector.kt`
2. Добавить regex-паттерн в Firebase Remote Config (или в defaults в `ParserConfigProvider`)
3. Добавить unit-тесты для нового формата в `core/parser/src/test/`
4. Проверить edge cases: разные форматы дат, суммы с пробелами, отрицательные суммы

### Модификация AI fallback
1. Промпт для Gemini определён в `GeminiServiceImpl.kt`
2. Формат ответа — JSON (responseMimeType="application/json")
3. Модель: Gemini 2.5 Flash через Firebase AI SDK

## testTag naming

- `import:screen`, `import:selectPdf`, `import:takePhoto`
- `import:preview`, `import:accountSelector`, `import:importButton`
- `import:loading`, `import:successCount`, `import:errorMessage`

## Quality Bar

- Дедупликация по `uniqueHash` (TransactionHashGenerator) — одна и та же транзакция не импортируется дважды
- Fallback на категорию "Другое" если AI-предложенная категория не найдена в БД
- `needsReview = true` для транзакций с низким confidence
- Все ошибки парсинга логируются, но не блокируют импорт остальных транзакций

## Anti-patterns

- НЕ отправляй PDF в Gemini если RegEx парсер успешно отработал — это лишние затраты
- НЕ блокируй импорт из-за одной невалидной строки — пропусти и продолжай
- НЕ храни PDF/image bytes в памяти дольше чем нужно — освобождай после парсинга
