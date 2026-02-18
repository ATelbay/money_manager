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
    fun `tryParsePdf returns null when PDF text is empty`() = runTest {
        every { pdfTextExtractor.extract(any()) } returns ""

        val result = statementParser.tryParsePdf(byteArrayOf(1))

        assertNull(result)
    }

    @Test
    fun `tryParsePdf returns null for unknown bank`() = runTest {
        every { pdfTextExtractor.extract(any()) } returns "Some random Forte Bank statement"
        coEvery { configProvider.getConfigs() } returns listOf(kaspiConfig)

        val result = statementParser.tryParsePdf(byteArrayOf(1))

        assertNull(result)
    }
}
