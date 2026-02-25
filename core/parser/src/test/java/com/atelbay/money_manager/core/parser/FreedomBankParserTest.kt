package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FreedomBankParserTest {

    private lateinit var parser: RegexStatementParser
    private lateinit var freedomConfig: ParserConfig

    @Before
    fun setUp() {
        parser = RegexStatementParser()
        freedomConfig = ParserConfig(
            bankId = "freedom",
            bankMarkers = listOf("Фридом Банк", "Freedom Bank", "bankffin.kz", "KSNVKZKA", "Super Card"),
            transactionPattern = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s+([+-])\\s?([\\d,]+\\.\\d{2})\\s*₸\\s+[A-Z]{3}\\s+(Покупка|Перевод|Пополнение|Сумма в обработке|Другое|Платеж|Снятие|Платеж по кредиту|Погашение|Овердрафт)\\s+(.+?)\\s*$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = mapOf(
                "Покупка" to "expense",
                "Перевод" to "expense",
                "Пополнение" to "income",
                "Сумма в обработке" to "expense",
                "Другое" to "income",
                "Платеж" to "expense",
                "Снятие" to "expense",
                "Платеж по кредиту" to "expense",
                "Погашение" to "expense",
                "Овердрафт" to "expense",
            ),
            skipPatterns = listOf(
                "Краткое содержание",
                "По курсу, установленному",
                "Сумма в обработке. Банк ожидает",
                "Подлинность справки",
                "Дата Сумма Валюта",
            ),
            joinLines = true,
            amountFormat = "comma_dot",
            useSignForType = true,
        )
    }

    @Test
    fun `parse expense purchase`() {
        val text = "23.02.2026 -5,749.80 ₸ KZT Покупка WOLT.COM ALMATY KZ"

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2026, 2, 23), tx.date)
        assertEquals(5749.80, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Покупка", tx.operationType)
        assertEquals("WOLT.COM ALMATY KZ", tx.details)
    }

    @Test
    fun `parse income deposit`() {
        val text = "21.02.2026 +50,000.00 ₸ KZT Пополнение Пополнение. ФИО: ТЕЛЬБАЙ АРЫСТАН"

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(50000.0, tx.amount, 0.01)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals("Пополнение", tx.operationType)
    }

    @Test
    fun `parse income transfer uses sign for type`() {
        val text = "24.02.2026 +28,774.53 ₸ KZT Перевод Перевод валюты Freedom на счет KZ12551B529955307KZT"

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(28774.53, tx.amount, 0.01)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals("Перевод", tx.operationType)
    }

    @Test
    fun `parse expense transfer uses sign for type`() {
        val text = "02.02.2026 -22,496.00 ₸ KZT Перевод Пополнение предоплаты по договору"

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(22496.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
    }

    @Test
    fun `parse income drugoe uses sign for type`() {
        val text = "06.02.2026 +50,000.00 ₸ KZT Другое Плательщик:Тельбай Арыстан"

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals("Другое", result[0].operationType)
    }

    @Test
    fun `parse amount without thousands separator`() {
        val text = "02.02.2026 +273.41 ₸ KZT Пополнение Выплата процентов по вкладу"

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        assertEquals(273.41, result[0].amount, 0.01)
    }

    @Test
    fun `join multiline summa v obrabotke`() {
        val text = """
25.02.2026 -9,201.44 ₸ KZT Сумма в
обработке WOLT.COM ALMATY KZ
        """.trimIndent()

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(9201.44, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Сумма в обработке", tx.operationType)
        assertEquals("WOLT.COM ALMATY KZ", tx.details)
    }

    @Test
    fun `join multiline poplnenie details`() {
        val text = """
21.02.2026 +50,000.00 ₸ KZT Пополнение
Пополнение. ФИО: ТЕЛЬБАЙ
АРЫСТАН ЖАНАЙБЕКҰЛЫ.
Мобильный: . Референс:
FRT20260221-906D3CDC7813.
        """.trimIndent()

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
        assertEquals(50000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertTrue(result[0].details.contains("ТЕЛЬБАЙ"))
    }

    @Test
    fun `parse multiple transactions`() {
        val text = """
25.02.2026 -9,201.44 ₸ KZT Сумма в
обработке WOLT.COM ALMATY KZ
25.02.2026 -1,280.00 ₸ KZT Сумма в
обработке YANDEX.GO ALMATY KZ
23.02.2026 -5,749.80 ₸ KZT Покупка WOLT.COM ALMATY KZ
24.02.2026 +28,774.53 ₸ KZT Перевод
Перевод валюты Freedom на счет
KZ12551B529955307KZT. По
договору №SRV-0075537 от
05.07.2024
        """.trimIndent()

        val result = parser.parse(text, freedomConfig)

        assertEquals(4, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals(TransactionType.EXPENSE, result[1].type)
        assertEquals(TransactionType.EXPENSE, result[2].type)
        assertEquals(TransactionType.INCOME, result[3].type)
    }

    @Test
    fun `skip lines matching skip patterns`() {
        val text = """
Дата Сумма Валюта Операция Детали
25.02.2026 -1,280.00 ₸ KZT Покупка YANDEX.GO ALMATY KZ
Подлинность справки можете проверить
        """.trimIndent()

        val result = parser.parse(text, freedomConfig)

        assertEquals(1, result.size)
    }

    @Test
    fun `each transaction has unique hash`() {
        val text = """
23.02.2026 -5,749.80 ₸ KZT Покупка WOLT.COM ALMATY KZ
22.02.2026 -1,070.00 ₸ KZT Покупка YANDEX.GO ALMATY KZ
        """.trimIndent()

        val result = parser.parse(text, freedomConfig)

        assertEquals(2, result.size)
        assertTrue(result[0].uniqueHash != result[1].uniqueHash)
        assertTrue(result[0].uniqueHash.isNotEmpty())
    }

    @Test
    fun `empty text returns empty list`() {
        val result = parser.parse("", freedomConfig)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `kaspi config still works without new fields`() {
        val kaspiConfig = ParserConfig(
            bankId = "kaspi",
            bankMarkers = listOf("Kaspi Gold"),
            transactionPattern = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s*₸\\s+(Покупка|Перевод|Пополнение)\\s+(.+?)\\s*$",
            dateFormat = "dd.MM.yy",
            operationTypeMap = mapOf(
                "Покупка" to "expense",
                "Пополнение" to "income",
            ),
        )
        val text = "  13.02.26              - 500,00 ₸                  Покупка    TOO \"KASPI MAGAZIN\""

        val result = parser.parse(text, kaspiConfig)

        assertEquals(1, result.size)
        assertEquals(500.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }
}
