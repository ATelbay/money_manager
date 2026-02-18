package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegexStatementParserTest {

    private lateinit var parser: RegexStatementParser
    private lateinit var kaspiConfig: ParserConfig

    @Before
    fun setUp() {
        parser = RegexStatementParser()
        kaspiConfig = ParserConfig(
            bankId = "kaspi",
            bankMarkers = listOf("Kaspi Gold"),
            transactionPattern = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s*₸\\s+(Покупка|Перевод|Пополнение)\\s+(.+?)\\s*$",
            dateFormat = "dd.MM.yy",
            operationTypeMap = mapOf(
                "Покупка" to "expense",
                "Перевод" to "expense",
                "Пополнение" to "income",
            ),
            skipPatterns = listOf(
                "АО «Kaspi Bank»",
                "Краткое содержание",
                "Сумма заблокирована",
            ),
        )
    }

    // ==================== UNIT TESTS ====================

    @Test
    fun `parse expense purchase line`() {
        val text = "  13.02.26              - 500,00 ₸                  Покупка    TOO \"KASPI MAGAZIN\""

        val result = parser.parse(text, kaspiConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2026, 2, 13), tx.date)
        assertEquals(500.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Покупка", tx.operationType)
        assertEquals("TOO \"KASPI MAGAZIN\"", tx.details)
        assertEquals(1.0f, tx.confidence)
        assertEquals(false, tx.needsReview)
    }

    @Test
    fun `parse expense transfer line`() {
        val text = "  13.02.26            - 5 720,00 ₸                  Перевод    Карлыгаш Е."

        val result = parser.parse(text, kaspiConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(5720.0, tx.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Перевод", tx.operationType)
        assertEquals("Карлыгаш Е.", tx.details)
    }

    @Test
    fun `parse income line`() {
        val text = "  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б."

        val result = parser.parse(text, kaspiConfig)

        assertEquals(1, result.size)
        val tx = result[0]
        assertEquals(LocalDate(2026, 2, 3), tx.date)
        assertEquals(7300.0, tx.amount, 0.01)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals("Пополнение", tx.operationType)
        assertEquals("Рымжан Б.", tx.details)
    }

    @Test
    fun `parse large amount with spaces`() {
        val text = "  02.02.26          - 517 500,00 ₸                  Покупка    КазНИТУ им. К.И.Сатпаева"

        val result = parser.parse(text, kaspiConfig)

        assertEquals(1, result.size)
        assertEquals(517500.0, result[0].amount, 0.01)
    }

    @Test
    fun `parse multiple lines returns all transactions`() {
        val text = """
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
  13.02.26              - 533,00 ₸                  Покупка    R_style
  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б.
        """.trimIndent()

        val result = parser.parse(text, kaspiConfig)

        assertEquals(3, result.size)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals(TransactionType.EXPENSE, result[1].type)
        assertEquals(TransactionType.INCOME, result[2].type)
    }

    @Test
    fun `skip lines matching skip patterns`() {
        val text = """
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
      АО «Kaspi Bank», БИК CASPKZKA, www.kaspi.kz
  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б.
        """.trimIndent()

        val result = parser.parse(text, kaspiConfig)

        assertEquals(2, result.size)
    }

    @Test
    fun `ignore non-matching lines`() {
        val text = """
ВЫПИСКА

        """.trimIndent()

        val result = parser.parse(text, kaspiConfig)

        assertEquals(1, result.size)
    }

    @Test
    fun `empty text returns empty list`() {
        val result = parser.parse("", kaspiConfig)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `each transaction has unique hash`() {
        val text = """
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
  13.02.26              - 533,00 ₸                  Покупка    R_style
        """.trimIndent()

        val result = parser.parse(text, kaspiConfig)

        assertEquals(2, result.size)
        assertTrue(result[0].uniqueHash != result[1].uniqueHash)
        assertTrue(result[0].uniqueHash.isNotEmpty())
    }
}
