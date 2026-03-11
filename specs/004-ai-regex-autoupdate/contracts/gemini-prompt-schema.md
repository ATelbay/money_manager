# Gemini Prompt & Response Schema: ParserConfig Generation

## Response Schema (Firebase AI SDK)

```kotlin
val parserConfigSchema = Schema.obj(
    properties = mapOf(
        "bank_id" to Schema.string(),
        "bank_markers" to Schema.array(Schema.string()),
        "transaction_pattern" to Schema.string(),
        "date_format" to Schema.string(),
        "operation_type_map" to Schema.obj(mapOf()),  // dynamic keys
        "skip_patterns" to Schema.array(Schema.string()),
        "join_lines" to Schema.boolean(),
        "amount_format" to Schema.enumeration(listOf("space_comma", "comma_dot", "dot")),
        "use_sign_for_type" to Schema.boolean(),
        "negative_sign_means_expense" to Schema.boolean(),
        "use_named_groups" to Schema.boolean(),
        "deduplicate_max_amount" to Schema.boolean(),
    ),
    optionalProperties = listOf(
        "skip_patterns", "join_lines", "amount_format",
        "use_sign_for_type", "negative_sign_means_expense",
        "use_named_groups", "deduplicate_max_amount",
    ),
)
```

**Note**: `operation_type_map` has dynamic keys (operation names from the bank). The Schema API doesn't support `additionalProperties`, so this field should use `Schema.string()` to accept the serialized JSON map, or be described in the prompt and parsed post-response.

## Prompt Template

```
Ты — эксперт по парсингу банковских выписок. Проанализируй образец строк из PDF-выписки и сгенерируй конфигурацию парсера.

## Правила для bank_id
Определи банк из заголовка/текста. Используй lowercase latin slug:
- Известные банки: kaspi, freedom, forte, bereke, eurasian
- Для неизвестных: транслитерация + lowercase + подчёркивания (например: "Народный банк" → "narodniy_bank", "Jusan Bank" → "jusan_bank")

## Правила для bank_markers
Укажи 2-3 уникальные строки-маркера, которые однозначно идентифицируют этот банк в тексте PDF.

## Правила для transaction_pattern
Создай regex-паттерн, который извлекает из каждой строки транзакции:
- Дату (группа "date" или позиция 1)
- Знак +/- если есть (группа "sign" или позиция 2)
- Сумму (группа "amount" или позиция 3)
- Тип операции (группа "operation" или позиция 4)
- Детали/описание (группа "details" или позиция 5)

Если используешь именованные группы — установи use_named_groups=true.

## Правила для amount_format
- "space_comma": "10 000,50" (пробел-разделитель тысяч, запятая-дробная)
- "comma_dot": "10,000.50" (запятая-разделитель тысяч, точка-дробная)
- "dot": "10000.50" (без разделителя тысяч, точка-дробная)

## Правила для operation_type_map
Маппинг названий операций на тип: "income" или "expense".
Например: {"Покупка": "expense", "Пополнение": "income"}

## Образец строк из выписки:
{sampleRows}
```

## Sample Extraction Strategy

From `PdfTextExtractor.extract(bytes)` output:
1. Split text into lines
2. Skip first 10 lines (header area)
3. Take next 10 non-empty lines (transaction area sample)
4. If fewer than 5 lines found, use all available lines after header

This sample is sent as text in the prompt (not as blob), since the text is already extracted.
