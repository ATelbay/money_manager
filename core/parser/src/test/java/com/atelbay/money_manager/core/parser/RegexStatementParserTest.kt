package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegexStatementParserTest {

    private lateinit var parser: RegexStatementParser
    private lateinit var kaspiConfig: RegexParserProfile

    @Before
    fun setUp() {
        parser = RegexStatementParser()
        kaspiConfig = RegexParserProfile(
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
    fun `amount with embedded minus sign is stored as positive magnitude`() {
        // If the amount group captures a leading '-' (sign not split into its own group),
        // the stored amount must still be a positive magnitude — type is decided separately.
        val config = RegexParserProfile(
            bankId = "test",
            bankMarkers = listOf("TestBank"),
            transactionPattern = "^(\\d{2}\\.\\d{2}\\.\\d{4})()\\s+(-?\\d+\\.\\d{2})\\s+(\\w+)\\s+(.+)$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = mapOf("Purchase" to "expense"),
            amountFormat = "dot",
        )
        val text = "01.03.2024 -500.00 Purchase Shop"

        val result = parser.parse(text, config)

        assertEquals(1, result.size)
        assertEquals(500.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
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
        val halykConfig = RegexParserProfile(
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

    // ==================== RE2 ENGINE: ReDoS / COMPATIBILITY ====================

    @Test(timeout = 5_000)
    fun `catastrophic backtracking pattern completes in linear time without matching`() {
        // (a+)+$ is the textbook catastrophic-backtracking pattern: on kotlin.text.Regex /
        // java.util.regex this input pegs a CPU core for effectively forever (exponential).
        // On the RE2 engine matching is linear and finishes instantly. The @Test timeout fails
        // the build if the old backtracking engine ever sneaks back into this hot path.
        val evilConfig = kaspiConfig.copy(
            transactionPattern = "(a+)+\$",
            skipPatterns = emptyList(),
        )
        val input = "a".repeat(100) + "!" // long, non-matching (trailing '!' blocks the anchor)

        val result = parser.parse(input, evilConfig)

        assertTrue(result.isEmpty())
    }

    @Test(timeout = 5_000)
    fun `catastrophic skip pattern completes in linear time and still parses transactions`() {
        // Skip patterns run find() over every line and are never validated upstream. A
        // catastrophic skip pattern must not be able to hang the parse.
        val config = kaspiConfig.copy(skipPatterns = listOf("(x+)+\$"))
        val text = "  13.02.26              - 500,00 ₸                  Покупка    TOO \"KASPI MAGAZIN\""

        val result = parser.parse(text, config)

        assertEquals(1, result.size)
    }

    @Test
    fun `incompatible transaction pattern (lookahead) returns empty result for AI fallback`() {
        // RE2 has no lookaround. An incompatible transactionPattern must yield an empty result
        // (which triggers the AI fallback upstream) rather than throwing or silently falling back
        // to a backtracking engine.
        val config = kaspiConfig.copy(
            transactionPattern =
                "(?=.*₸)(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s*₸\\s+(\\w+)\\s+(.+)",
        )
        val text = "  13.02.26              - 500,00 ₸                  Покупка    TOO \"KASPI MAGAZIN\""

        val result = parser.parse(text, config)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `incompatible skip pattern (lookahead) degrades to literal and does not crash`() {
        // A lookaround skip pattern can't compile on RE2; it degrades to an inert literal filter.
        // The parse must still succeed and return the valid transaction.
        val config = kaspiConfig.copy(skipPatterns = listOf("(?=foo)bar"))
        val text = "  13.02.26              - 500,00 ₸                  Покупка    TOO \"KASPI MAGAZIN\""

        val result = parser.parse(text, config)

        assertEquals(1, result.size)
    }

    @Test
    fun `named group config in Java syntax is normalized and parsed by RE2`() {
        // Configs store Java/Kotlin (?<name>) syntax; RE2 requires (?P<name>). This proves the
        // in-parser normalization works end-to-end on the real matching path.
        val config = RegexParserProfile(
            bankId = "named_test",
            bankMarkers = listOf("NamedBank"),
            transactionPattern =
                "^(?<date>\\d{2}\\.\\d{2}\\.\\d{4})\\s+(?<sign>[-+]?)(?<amount>\\d+\\.\\d{2})\\s+(?<operation>\\w+)\\s+(?<details>.+)\$",
            dateFormat = "dd.MM.yyyy",
            operationTypeMap = emptyMap(),
            useNamedGroups = true,
            negativeSignMeansExpense = true,
            amountFormat = "dot",
        )
        val text = "01.03.2024 -500.00 Purchase Coffee Shop"

        val result = parser.parse(text, config)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2024, 3, 1), result[0].date)
        assertEquals(500.0, result[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result[0].type)
        assertEquals("Coffee Shop", result[0].details)
    }

    @Test
    fun `reference bank patterns compile on RE2 after named-group normalization`() {
        // The production bank patterns (mirrors of RegexValidatorTest) must all be RE2-compatible.
        // If any starts to require lookaround/backreferences, this fails loudly so we can react.
        val referencePatterns = mapOf(
            "kaspi" to "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s*₸\\s+(Покупка|Перевод|Пополнение)\\s+(.+?)\\s*\$",
            "freedom" to "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s+(Покупка|Перевод|Пополнение|Выпуск карты)\\s+(.*?)\\s*\$",
            "forte" to "^\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s+([+-]?)([\\d\\s]+,\\d{2})\\s+KZT\\s+(.+?)\\s{2,}(.+?)\\s*\$",
            "bereke" to "(?<date>\\d{2}/\\d{2}/\\d{4})\\s+(?<sign>[+-]?)(?<amount>[\\d,]+\\.\\d{2})\\s+KZT\\s+(?<operation>.+?)\\s{2,}(?<details>.+)",
            "eurasian" to "(?<date>\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(?<sign>[+-])\\s*(?<amount>[\\d\\s]+,\\d{2})\\s+KZT\\s+(?<operation>.+?)\\s{2,}(?<details>.+)",
        )

        referencePatterns.forEach { (bank, pattern) ->
            val normalized = RegexStatementParser.normalizeNamedGroups(pattern)
            // No Java-style named-group opener should survive normalization.
            assertFalse("$bank still has Java named groups", normalized.contains("(?<"))
            // Should not throw — all five are RE2-compatible (no lookaround/backreferences).
            com.google.re2j.Pattern.compile(normalized, com.google.re2j.Pattern.MULTILINE)
        }
    }

    @Test
    fun `normalizeNamedGroups rewrites named groups but leaves lookbehind untouched`() {
        assertEquals("(?P<date>\\d+)", RegexStatementParser.normalizeNamedGroups("(?<date>\\d+)"))
        // Lookbehind must NOT be converted (RE2 rejects it as a whole anyway).
        assertEquals("(?<=X)\\d+", RegexStatementParser.normalizeNamedGroups("(?<=X)\\d+"))
        assertEquals("(?<!X)\\d+", RegexStatementParser.normalizeNamedGroups("(?<!X)\\d+"))
    }

    @Test
    fun `two-phase skip removes page headers without corrupting transactions`() {
        val config = RegexParserProfile(
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
