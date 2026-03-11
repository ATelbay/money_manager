package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatementParserTest {

    private lateinit var pdfTextExtractor: PdfTextExtractor
    private lateinit var configProvider: ParserConfigProvider
    private lateinit var statementParser: StatementParser

    private val kaspiConfig = ParserConfig(
        bankId = "kaspi",
        bankMarkers = listOf("Kaspi Gold", "АО «Kaspi Bank»"),
        transactionPattern = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s*₸\\s+(Покупка|Перевод|Пополнение)\\s+(.+?)\\s*$",
        dateFormat = "dd.MM.yy",
        operationTypeMap = mapOf(
            "Покупка" to "expense",
            "Перевод" to "expense",
            "Пополнение" to "income",
        ),
        skipPatterns = listOf("АО «Kaspi Bank»"),
    )

    private val kaspiStatementText = """
ВЫПИСКА
по Kaspi Gold за период с 14.01.26 по 14.02.26
   Дата              Сумма                     Операция        Детали
  13.02.26              - 500,00 ₸                  Покупка    TOO "KASPI MAGAZIN"
  13.02.26            - 4 020,00 ₸                  Покупка    ТОО ОЗАР ФАРМ
  03.02.26           + 7 300,00 ₸            Пополнение        Рымжан Б.
      АО «Kaspi Bank», БИК CASPKZKA, www.kaspi.kz
  01.02.26          - 150 000,00 ₸                  Перевод    Жания Б.
    """.trimIndent()

    @Before
    fun setUp() {
        pdfTextExtractor = mockk()
        configProvider = mockk()
        statementParser = StatementParser(
            pdfTextExtractor = pdfTextExtractor,
            bankDetector = BankDetector(),
            regexParser = RegexStatementParser(),
            configProvider = configProvider,
        )
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    fun `tryParsePdf extracts and parses Kaspi statement end to end`() = runTest {
        val pdfBytes = byteArrayOf(1, 2, 3)
        every { pdfTextExtractor.extract(pdfBytes) } returns kaspiStatementText
        coEvery { configProvider.getConfigs() } returns listOf(kaspiConfig)

        val result = statementParser.tryParsePdf(pdfBytes)

        assertNotNull(result)
        assertEquals("kaspi", result!!.bankId)
        assertEquals(4, result.transactions.size)
        assertEquals(500.0, result.transactions[0].amount, 0.01)
        assertEquals(4020.0, result.transactions[1].amount, 0.01)
        assertEquals(7300.0, result.transactions[2].amount, 0.01)
        assertEquals(150000.0, result.transactions[3].amount, 0.01)
    }

    @Test
    fun `tryParsePdf returns correct transaction types`() = runTest {
        val pdfBytes = byteArrayOf(1, 2, 3)
        every { pdfTextExtractor.extract(pdfBytes) } returns kaspiStatementText
        coEvery { configProvider.getConfigs() } returns listOf(kaspiConfig)

        val result = statementParser.tryParsePdf(pdfBytes)!!

        assertEquals("expense", result.transactions[0].type.value) // Покупка
        assertEquals("expense", result.transactions[1].type.value) // Покупка
        assertEquals("income", result.transactions[2].type.value)  // Пополнение
        assertEquals("expense", result.transactions[3].type.value) // Перевод
    }

    // ==================== NEGATIVE TEST ====================

    @Test
    fun `tryParsePdf returns empty transactions when PDF text is empty`() = runTest {
        every { pdfTextExtractor.extract(any()) } returns ""

        val result = statementParser.tryParsePdf(byteArrayOf(1))

        assertNotNull(result)
        assertTrue(result!!.transactions.isEmpty())
        assertNull(result.bankId)
    }

    @Test
    fun `tryParsePdf returns empty transactions for unknown bank`() = runTest {
        every { pdfTextExtractor.extract(any()) } returns "Some random Forte Bank statement"
        coEvery { configProvider.getConfigs() } returns listOf(kaspiConfig)

        val result = statementParser.tryParsePdf(byteArrayOf(1))

        assertNotNull(result)
        assertTrue(result!!.transactions.isEmpty())
        assertNull(result.bankId)
    }

    // ==================== extractSampleRows TESTS ====================

    @Test
    fun `extractSampleRows returns 10 data lines after skipping 10 header lines`() {
        val headerLines = (1..10).map { "Header line $it" }
        val dataLines = (1..15).map { "Data line $it" }
        val text = (headerLines + dataLines).joinToString("\n")

        val result = statementParser.extractSampleRows(text)

        assertEquals(10, result.lines().size)
        assertEquals("Data line 1", result.lines().first())
        assertEquals("Data line 10", result.lines().last())
    }

    @Test
    fun `extractSampleRows skips blank lines in data section`() {
        val headerLines = (1..10).map { "Header line $it" }
        val dataLines = listOf("Data 1", "", "Data 2", "  ", "Data 3", "Data 4", "Data 5", "Data 6", "Data 7")
        val text = (headerLines + dataLines).joinToString("\n")

        val result = statementParser.extractSampleRows(text)

        assertEquals(7, result.lines().size)
        assertTrue(result.lines().none { it.isBlank() })
    }

    @Test
    fun `extractSampleRows returns all available data lines when fewer than 5 exist`() {
        val headerLines = (1..10).map { "Header line $it" }
        val dataLines = listOf("Data 1", "Data 2", "Data 3", "Data 4")
        val text = (headerLines + dataLines).joinToString("\n")

        val result = statementParser.extractSampleRows(text)

        assertEquals(4, result.lines().size)
        assertEquals("Data 1", result.lines().first())
        assertEquals("Data 4", result.lines().last())
    }

    @Test
    fun `extractSampleRows returns empty for empty text`() {
        val result = statementParser.extractSampleRows("")
        assertEquals("", result)
    }

    @Test
    fun `extractSampleRows returns empty for header-only PDF`() {
        val text = (1..10).joinToString("\n") { "Header $it" }
        val result = statementParser.extractSampleRows(text)
        assertEquals("", result)
    }

    @Test
    fun `extractHeaderSnippet keeps non blank header lines from the first 10 rows`() {
        val headerLines = listOf("Bank statement", "", "АО Test Bank", "BIN 123456", "  ")
        val paddingLines = List(6) { "" }
        val text = (headerLines + paddingLines + listOf("Data 1")).joinToString("\n")

        val result = statementParser.extractHeaderSnippet(text)

        assertEquals(listOf("Bank statement", "АО Test Bank", "BIN 123456"), result.lines())
    }
}
