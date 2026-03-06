package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BerekeBankParserTest {

    private lateinit var parser: RegexStatementParser
    private lateinit var berekeConfig: ParserConfig

    @Before
    fun setUp() {
        parser = RegexStatementParser()
        berekeConfig = ParserConfig(
            bankId = "bereke",
            bankMarkers = listOf("Bereke Bank", "BRKEKZKA", "berekebank.kz"),
            transactionPattern = "^(?<date>\\d{2}\\.\\d{2}\\.\\d{4})\\s+(?<operation>Operation|Payment for goods and services|Card replenishment through Bereke Bank|Card replenishment through payment terminal|Transfer from a card through Bereke Bank)\\s+(?<details>.+?)\\s+[-]?[\\d,]+\\.\\d{2}\\s+[A-Z]{3}\\s+(?<sign>[-]?)(?<amount>[\\d,]+\\.\\d{2})(?:\\s+\\*{4}\\s+\\d+)?$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = mapOf(
                "Operation" to "expense",
                "Payment for goods and services" to "expense",
                "Card replenishment through Bereke Bank" to "income",
                "Card replenishment through payment terminal" to "income",
                "Transfer from a card through Bereke Bank" to "expense",
            ),
            skipPatterns = listOf("Transaction date", "Card account statement", "For the period"),
            joinLines = true,
            amountFormat = "comma_dot",
            useNamedGroups = true,
            negativeSignMeansExpense = true,
        )
    }

    @Test
    fun `parse expense payment for goods`() {
        val text = "26.03.2025 Payment for goods and services ROYAL PETROL AZS 10-4 -10,000.00 KZT -10,000.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2025, 3, 26), tx.date)
        assertEquals(10000.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Payment for goods and services", tx.operationType)
        assertEquals("ROYAL PETROL AZS 10-4", tx.details)
    }

    @Test
    fun `parse expense operation (transfer out)`() {
        val text = "07.03.2025 Operation JSC Eurasian Bank АРЫСТАН Т. -20,000.00 KZT -20,000.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(20000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("JSC Eurasian Bank АРЫСТАН Т.", result[0].details)
    }

    @Test
    fun `parse income operation (transfer in)`() {
        val text = "02.04.2025 Operation Перевод по номеру телефона ForteBank JSC Арыстан Жанайбекулы Тельбай 30,000.00 KZT 30,000.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(30000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    @Test
    fun `parse income card replenishment through bank`() {
        val text = "20.04.2025 Card replenishment through Bereke Bank from your deposit 20,000.00 KZT 20,000.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(20000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals("Card replenishment through Bereke Bank", result[0].operationType)
        assertEquals("from your deposit", result[0].details)
    }

    @Test
    fun `parse income card replenishment through payment terminal`() {
        val text = "06.08.2025 Card replenishment through payment terminal АРЫСТАН ЖАНАЙБЕКҰЛЫ Т. 490,000.00 KZT 490,000.00"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(490000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
    }

    @Test
    fun `parse expense transfer from card`() {
        val text = "23.04.2025 Transfer from a card through Bereke Bank to your account -500,000.00 KZT -500,000.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(500000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
    }

    @Test
    fun `parse bonus transfer (no card number)`() {
        val text = "14.03.2025 Operation Bonus Transfer 9,344.57 KZT 9,344.57"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(9344.57, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals("Bonus Transfer", result[0].details)
    }

    @Test
    fun `parse large amount with comma thousands separator`() {
        val text = "23.04.2025 Operation Перевод по номеру телефона Bank Freedom Finance JSC ТЕЛЬБАЙ АРЫСТАН ЖАНАЙБЕКҰЛЫ 500,000.00 KZT 500,000.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(500000.0, result[0].amount, 0.01)
    }

    @Test
    fun `parse starbucks coffee purchase`() {
        val text = "09.04.2025 Payment for goods and services STARBUCKS COFFEE -3,300.00 KZT -3,300.00 **** 0600"

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
        assertEquals(3300.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("STARBUCKS COFFEE", result[0].details)
    }

    @Test
    fun `skip header lines`() {
        val text = """
Transaction date Transaction Detailed description Amount in transaction currency Transaction currency Amount in account currency Card number
07.03.2025 Operation JSC Eurasian Bank АРЫСТАН Т. -20,000.00 KZT -20,000.00 **** 0600
        """.trimIndent()

        val result = parser.parse(text, berekeConfig)

        assertEquals(1, result.size)
    }

    @Test
    fun `parse multiple transactions`() {
        val text = """
07.03.2025 Operation JSC Eurasian Bank АРЫСТАН Т. -20,000.00 KZT -20,000.00 **** 0600
14.03.2025 Operation Bonus Transfer 9,344.57 KZT 9,344.57
26.03.2025 Payment for goods and services ROYAL PETROL AZS 10-4 -10,000.00 KZT -10,000.00 **** 0600
        """.trimIndent()

        val result = parser.parse(text, berekeConfig)

        assertEquals(3, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals(TransactionType.INCOME, result[1].type)
        assertEquals(TransactionType.EXPENSE, result[2].type)
    }

    @Test
    fun `each transaction has unique hash`() {
        val text = """
07.03.2025 Operation JSC Eurasian Bank АРЫСТАН Т. -20,000.00 KZT -20,000.00 **** 0600
09.03.2025 Operation «Freedom Bank Kazakhstan» JSC Арыстан Т. -30,000.00 KZT -30,000.00 **** 0600
        """.trimIndent()

        val result = parser.parse(text, berekeConfig)

        assertEquals(2, result.size)
        assertTrue(result[0].uniqueHash != result[1].uniqueHash)
    }

    @Test
    fun `empty text returns empty list`() {
        val result = parser.parse("", berekeConfig)
        assertTrue(result.isEmpty())
    }
}
