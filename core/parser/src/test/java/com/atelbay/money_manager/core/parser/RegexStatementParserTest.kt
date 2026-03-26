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
по Kaspi Gold за период с 14.01.26 по 14.02.26
Дата              Сумма                     Операция        Детали
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
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

    // ==================== SKIP PATTERN TESTS ====================

    @Test
    fun `regex skip patterns filter header lines`() {
        val config = kaspiConfig.copy(
            skipPatterns = listOf("^Statement of account:.*$", "^Total:$"),
        )
        val text = """
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
Statement of account: Multicurrency contract №123
  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б.
Total:
        """.trimIndent()

        val result = parser.parse(text, config)

        assertEquals(2, result.size)
    }

    @Test
    fun `invalid regex skip pattern falls back to literal match`() {
        val config = kaspiConfig.copy(
            skipPatterns = listOf("Amount [KZT"),
        )
        val text = """
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
Amount [KZT
  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б.
        """.trimIndent()

        val result = parser.parse(text, config)

        assertEquals(2, result.size)
    }

    @Test
    fun `plain text skip patterns still work after fix`() {
        val text = """
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
АО «Kaspi Bank», БИК CASPKZKA, www.kaspi.kz
  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б.
        """.trimIndent()

        val result = parser.parse(text, kaspiConfig)

        assertEquals(2, result.size)
    }

    @Test
    fun `multi-page PDF with regex skip patterns parses all transactions`() {
        val halykConfig = ParserConfig(
            bankId = "halyk_test",
            bankMarkers = listOf("Halyk"),
            transactionPattern = "^\\s*(?<date>\\d{2}\\.\\d{2}\\.\\d{4})\\s+\\d{2}\\.\\d{2}\\.\\d{4}\\s+(?<details>.+?)\\s+(?<sign>-?)(?<amount>[\\d\\s]+,\\d{2})\\s+KZT.*$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = emptyMap(),
            useNamedGroups = true,
            negativeSignMeansExpense = true,
            amountFormat = "space_comma",
            joinLines = true,
            skipPatterns = listOf(
                "^Statement of account:.*$",
                "^Date of Date of.*$",
                "^transaction transaction.*$",
            ),
        )
        val text = """
01.06.2025 01.06.2025 Receipt 50 000,00 KZT 50 000,00 0,00 0,00
440563******4874
CASH TO CARD ATM
Statement of account: Multicurrency contract №013 1
Date of Date of ion Transaction Credit
transaction transaction Transaction description
02.06.2025 02.06.2025 Payment -15 058,29 KZT 0,00 -15 058,29 0,00
KZ696010002029688291
Погашение кредита (KZT)
        """.trimIndent()

        val result = parser.parse(text, halykConfig)

        assertEquals(2, result.size)
        assertEquals(LocalDate(2025, 6, 1), result[0].date)
        assertEquals(50000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(LocalDate(2025, 6, 2), result[1].date)
        assertEquals(15058.29, result[1].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[1].type)
    }

    @Test
    fun `two-phase skip removes page headers without corrupting transactions`() {
        val config = ParserConfig(
            bankId = "halyk_test",
            bankMarkers = listOf("Halyk"),
            transactionPattern = "^\\s*(?<date>\\d{2}\\.\\d{2}\\.\\d{4})\\s+\\d{2}\\.\\d{2}\\.\\d{4}\\s+(?<operation>.+?)\\s+(?<sign>-?)(?<amount>\\d{1,3}(?:\\s\\d{3})*,\\d{2})\\s+KZT.*$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = emptyMap(),
            useNamedGroups = true,
            negativeSignMeansExpense = true,
            amountFormat = "space_comma",
            joinLines = true,
            skipPatterns = listOf(
                "Statement of account:",
                "Date of Date of",
                "transaction transaction",
            ),
        )
        // Multi-page PDF with page headers between transactions.
        // Phase 1 removes non-date continuation lines matching skip patterns before join,
        // preventing them from attaching to adjacent transactions.
        val text = """
01.06.2025 01.06.2025 Receipt 50 000,00 KZT 50 000,00 0,00 0,00
440563******4874
CASH TO CARD ATM
Statement of account: Multicurrency contract №013 1
Date of Date of ion Transaction Credit
transaction transaction Transaction description
02.06.2025 02.06.2025 Payment -15 058,29 KZT 0,00 -15 058,29 0,00
KZ696010002029688291
Погашение кредита (KZT)
        """.trimIndent()

        val result = parser.parse(text, config)

        assertEquals(2, result.size)
        assertEquals(LocalDate(2025, 6, 1), result[0].date)
        assertEquals(50000.0, result[0].amount, 0.01)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(LocalDate(2025, 6, 2), result[1].date)
        assertEquals(15058.29, result[1].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[1].type)
    }
}
