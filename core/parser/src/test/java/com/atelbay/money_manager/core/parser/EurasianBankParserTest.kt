package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EurasianBankParserTest {

    private lateinit var parser: RegexStatementParser
    private lateinit var eurasianConfig: ParserConfig

    @Before
    fun setUp() {
        parser = RegexStatementParser()
        eurasianConfig = ParserConfig(
            bankId = "eurasian",
            bankMarkers = listOf("EURIKZKA", "eubank.kz", "АО \"Евразийский Банк\"", "Евразийский Банк"),
            transactionPattern = "^(?<date>\\d{2}\\.\\d{2}\\.\\d{4})(?:\\s+\\d{2}:\\d{2}:\\d{2})?\\s+(?<operation>Путешествия|Финансы|Продукты|Кафе и рестораны|Услуги|Развлечения|Государственные услуги|Здоровье и красота|Транспорт|Связь)\\s+(?<details>.+?)\\s+[\\d.]+\\s+[A-Z]{3}\\s+(?<sign>[+-]?)(?<amount>[\\d.]+)(?:\\s+(?:Карта|Счёт):\\s+\\*{2}\\d{4})?(?:\\s+\\d{2}:\\d{2}:\\d{2})?$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = mapOf(
                "Путешествия" to "expense",
                "Финансы" to "income",
                "Продукты" to "expense",
                "Кафе и рестораны" to "expense",
                "Услуги" to "expense",
                "Развлечения" to "expense",
                "Государственные услуги" to "expense",
                "Здоровье и красота" to "expense",
                "Транспорт" to "expense",
                "Связь" to "expense",
            ),
            skipPatterns = listOf("Дата Тип операции", "Выписка по счёту", "Приход за период", "Расход за период"),
            joinLines = true,
            useNamedGroups = true,
            negativeSignMeansExpense = true,
            deduplicateMaxAmount = true,
        )
    }

    @Test
    fun `parse expense travel (negative KZT amount)`() {
        val text = "12.01.2026 04:08:45 Путешествия Headout Ferrari World 97.28 GBP -66819.63 Счёт: **3108"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2026, 1, 12), tx.date)
        assertEquals(66819.63, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Путешествия", tx.operationType)
        assertEquals("Headout Ferrari World", tx.details)
    }

    @Test
    fun `parse income finance (positive KZT amount)`() {
        val text = "12.01.2026 09:03:12 Финансы MC World 80000 KZT +80000 Счёт: **3108"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(80000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals("MC World", result[0].details)
    }

    @Test
    fun `parse income transfer from another bank`() {
        val text = "12.01.2026 09:27:23 Финансы С карты другого банка 120000 KZT +120000 Карта: **7777"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(120000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    @Test
    fun `parse expense food (Продукты)`() {
        val text = "12.01.2026 15:35:24 Продукты YAS WATER WORLD LLC 18 AED -2502.87 Счёт: **3108"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(2502.87, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Продукты", result[0].operationType)
    }

    @Test
    fun `parse expense entertainment`() {
        val text = "17.01.2026 20:50:04 Развлечения STEAMGAMES.COM 4259522 2280 KZT -2284.23 Счёт: **3108"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(2284.23, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `parse without time in date`() {
        val text = "12.01.2026 Финансы MC World 80000 KZT +80000 Счёт: **3108"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2026, 1, 12), result[0].date)
    }

    @Test
    fun `parse without card or account suffix`() {
        val text = "17.01.2026 20:50:04 Развлечения STEAMGAMES.COM 4259522 2280 KZT +4.46"

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(4.46, result[0].amount, 0.01)
    }

    @Test
    fun `deduplicate keeps max amount row from triplet`() {
        // Foreign currency triplet: card debit, account mirror, actual KZT debit
        val text = """
12.01.2026 04:08:45 Путешествия Headout Ferrari World 97.28 GBP -131.06 Карта: **7777
12.01.2026 04:08:45 Путешествия Headout Ferrari World 97.28 GBP +131.06 Счёт: **3108
12.01.2026 04:08:45 Путешествия Headout Ferrari World 97.28 GBP -66819.63 Счёт: **3108
        """.trimIndent()

        val result = parser.parse(text, eurasianConfig)

        // After dedup: only the row with max amount (66819.63) is kept
        assertEquals(1, result.size)
        assertEquals(66819.63, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `deduplicate keeps income singlet unchanged`() {
        val text = "12.01.2026 09:03:12 Финансы MC World 80000 KZT +80000 Счёт: **3108"

        val result = parser.parse(text, eurasianConfig)

        // Single income row — no dedup needed, kept as-is
        assertEquals(1, result.size)
        assertEquals(80000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    @Test
    fun `deduplicate KZT triplet keeps large account debit`() {
        // KZT triplet: card debit, account mirror, actual KZT debit (slightly different due to fees)
        val text = """
17.01.2026 20:50:04 Развлечения STEAMGAMES.COM 4259522 2280 KZT -4.46 Карта: **7777
17.01.2026 20:50:04 Развлечения STEAMGAMES.COM 4259522 2280 KZT +4.46 Счёт: **3108
17.01.2026 20:50:04 Развлечения STEAMGAMES.COM 4259522 2280 KZT -2284.23 Счёт: **3108
        """.trimIndent()

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
        assertEquals(2284.23, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `parse multiple independent transactions`() {
        val text = """
12.01.2026 09:03:12 Финансы MC World 80000 KZT +80000 Счёт: **3108
13.01.2026 12:09:02 Кафе и рестораны MIRAL EXPERIENCES LLC 20 AED -2778.14 Счёт: **3108
14.01.2026 08:44:27 Услуги DUTCH ORIENTAL MEGA YA 100 AED -13919.43 Счёт: **3108
        """.trimIndent()

        val result = parser.parse(text, eurasianConfig)

        assertEquals(3, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(TransactionType.EXPENSE, result[1].type)
        assertEquals(TransactionType.EXPENSE, result[2].type)
    }

    @Test
    fun `skip header lines`() {
        val text = """
Дата Тип операции Детали операции Сумма операции Валюта операции Сумма в валюте счета Номер карты/счёта
12.01.2026 09:03:12 Финансы MC World 80000 KZT +80000 Счёт: **3108
        """.trimIndent()

        val result = parser.parse(text, eurasianConfig)

        assertEquals(1, result.size)
    }

    @Test
    fun `empty text returns empty list`() {
        val result = parser.parse("", eurasianConfig)
        assertTrue(result.isEmpty())
    }
}
