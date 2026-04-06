package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForteBankParserTest {

    private lateinit var parser: RegexStatementParser
    private lateinit var forteConfig: RegexParserProfile

    @Before
    fun setUp() {
        parser = RegexStatementParser()
        forteConfig = RegexParserProfile(
            bankId = "forte",
            bankMarkers = listOf("IRTYKZKA", "forte.kz", "АО «ForteBank»", "ForteBank"),
            transactionPattern = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s+([-]?)(\\d+\\.\\d{2})\\s+KZT(?:\\s*\\([^)]+\\))?\\s+(Перевод|Пополнение счета|Списание средств в рамках сервиса быстрых платежей|Покупка|Покупка бонусами|Платёж|Платеж|Комиссия|Списание)\\s+(.+?)\\s*$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = mapOf(
                "Перевод" to "expense",
                "Пополнение счета" to "income",
                "Списание средств в рамках сервиса быстрых платежей" to "expense",
                "Покупка" to "expense",
                "Покупка бонусами" to "income",
                "Платёж" to "expense",
                "Платеж" to "expense",
                "Комиссия" to "expense",
                "Списание" to "expense",
            ),
            skipPatterns = listOf(
                "Дата Сумма",
                "Задолженность на",
                "Сервисные Комиссии",
                "Реквизиты:",
                "Сформировано в Интернет Банкинге",
                "^рамках сервиса\\s*$",
                "^быстрых платежей\\s*$",
            ),
            joinLines = true,
            negativeSignMeansExpense = true,
        )
    }

    @Test
    fun `parse expense purchase`() {
        val text = "19.02.2026 -10230.00 KZT Покупка SAHARA LOUNGE BAR ALMATY KZ"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2026, 2, 19), tx.date)
        assertEquals(10230.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Покупка", tx.operationType)
        assertEquals("SAHARA LOUNGE BAR ALMATY KZ", tx.details)
    }

    @Test
    fun `parse income top-up`() {
        val text = "03.03.2026 157319.66 KZT Пополнение счета Выплата на карт.счет работника Головной Офис АО ForteBaNk"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2026, 3, 3), tx.date)
        assertEquals(157319.66, tx.amount, 0.01)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals("Пополнение счета", tx.operationType)
    }

    @Test
    fun `parse expense transfer with minus sign`() {
        val text = "04.03.2026 -55000.00 KZT Перевод Получатель: 521700*******7777"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(55000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Получатель: 521700*******7777", result[0].details)
    }

    @Test
    fun `parse income has no sign prefix`() {
        val text = "02.03.2026 100000.00 KZT Пополнение счета Снятие со вклада (КНП 322) ;ISJUR"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(100000.0, result[0].amount, 0.01)
    }

    @Test
    fun `parse fast payment debit (Списание средств)`() {
        val text = "04.03.2026 -50000.00 KZT Списание средств в рамках сервиса быстрых платежей Списание средств в рамках сервиса быстрых платежей"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(50000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Списание средств в рамках сервиса быстрых платежей", result[0].operationType)
    }

    @Test
    fun `parse purchase with foreign currency annotation`() {
        val text = "11.01.2026 -13647.50 KZT (26.50 USD) Покупка AIRALO SINGAPORE SG, Stripe Payments Singapore Pte Ltd"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(13647.50, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("AIRALO SINGAPORE SG, Stripe Payments Singapore Pte Ltd", result[0].details)
    }

    @Test
    fun `parse commission (Комиссия)`() {
        val text = "09.12.2025 -10000.00 KZT Комиссия Погашение комиссии"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Комиссия", result[0].operationType)
    }

    @Test
    fun `parse bonus purchase (Покупка бонусами) as income`() {
        val text = "15.02.2026 10236.40 KZT Покупка бонусами MON AMIE SHOP ALMATY KZ"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(10236.40, result[0].amount, 0.01)
    }

    @Test
    fun `join multiline fast payment description`() {
        val text = """
04.03.2026 -50000.00 KZT Списание средств в
рамках сервиса быстрых платежей Списание средств в рамках сервиса быстрых платежей
        """.trimIndent()

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(50000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `skip header lines`() {
        val text = """
Дата Сумма Описание Детализация
04.03.2026 -55000.00 KZT Перевод Получатель: 521700*******7777
        """.trimIndent()

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
    }

    @Test
    fun `parse multiple transactions`() {
        val text = """
04.03.2026 -55000.00 KZT Перевод Получатель: 521700*******7777
03.03.2026 157319.66 KZT Пополнение счета Выплата на карт.счет
02.03.2026 -40000.00 KZT Перевод Получатель: 521700*******7777
        """.trimIndent()

        val result = parser.parse(text, forteConfig)

        assertEquals(3, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals(TransactionType.INCOME, result[1].type)
        assertEquals(TransactionType.EXPENSE, result[2].type)
    }

    @Test
    fun `each transaction has unique hash`() {
        val text = """
04.03.2026 -55000.00 KZT Перевод Получатель: 521700*******7777
04.03.2026 -50000.00 KZT Списание средств в рамках сервиса быстрых платежей Списание средств в рамках сервиса быстрых платежей
        """.trimIndent()

        val result = parser.parse(text, forteConfig)

        assertEquals(2, result.size)
        assertTrue(result[0].uniqueHash != result[1].uniqueHash)
    }

    @Test
    fun `parse expense withdrawal (Списание)`() {
        val text = "05.03.2026 -20000.00 KZT Списание Комиссия за обслуживание карты"

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
        assertEquals(20000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Списание", result[0].operationType)
    }

    @Test
    fun `skip footer lines (Реквизиты and Сформировано)`() {
        val text = """
04.03.2026 -55000.00 KZT Перевод Получатель: 521700*******7777
Реквизиты: БИН 123456789012 БИК IRTYKZKA
Сформировано в Интернет Банкинге fortebank.kz
        """.trimIndent()

        val result = parser.parse(text, forteConfig)

        assertEquals(1, result.size)
    }

    @Test
    fun `empty text returns empty list`() {
        val result = parser.parse("", forteConfig)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `skip orphan fragments before fast payment`() {
        val text = """
04.03.2026 -50000.00 KZT
рамках сервиса
Списание средств в рамках сервиса быстрых платежей Выплата на карт.счет работника
        """.trimIndent()
        val result = parser.parse(text, forteConfig)
        assertEquals(1, result.size)
        assertEquals(50000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Списание средств в рамках сервиса быстрых платежей", result[0].operationType)
        assertEquals("Выплата на карт.счет работника", result[0].details)
    }
}
