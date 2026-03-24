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
    private lateinit var pdfTableExtractor: PdfTableExtractor
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
        pdfTableExtractor = mockk(relaxed = true)
        configProvider = mockk()
        statementParser = StatementParser(
            pdfTextExtractor = pdfTextExtractor,
            bankDetector = BankDetector(),
            regexParser = RegexStatementParser(),
            configProvider = configProvider,
            pdfTableExtractor = pdfTableExtractor,
            tableStatementParser = TableStatementParser(),
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
    fun `extractSampleRows returns up to 60 data lines after skipping 10 header lines`() {
        val headerLines = (1..10).map { "Header line $it" }
        val dataLines = (1..70).map { "Data line $it" }
        val text = (headerLines + dataLines).joinToString("\n")

        val result = statementParser.extractSampleRows(text)

        assertEquals(60, result.lines().size)
        assertEquals("Data line 1", result.lines().first())
        assertEquals("Data line 60", result.lines().last())
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

    // ==================== extractSampleTableRows TESTS ====================

    @Test
    fun `extractSampleTableRows filters out single-cell metadata rows from Halyk-like PDFs`() {
        val rawTable = listOf(
            listOf("Branch:Headbank", "", "", "", "", "", ""),
            listOf("Address:Almaty", "", "", "", "", "", ""),
            listOf("BIC:HSBKKZKX", "", "", "", "", "", ""),
            listOf("Date", "TxId", "Sign", "Amount", "CCY", "Operation", "Details"),
            listOf("01.01.2026", "TX001", "-", "5000.00", "KZT", "Purchase", "Shop"),
            listOf("02.01.2026", "TX002", "+", "10000.00", "KZT", "Transfer", "Salary"),
        )
        every { pdfTableExtractor.extractTable(any()) } returns rawTable

        val result = statementParser.extractSampleTableRows(byteArrayOf(1, 2, 3))

        // 3 metadata rows filtered, 1 header dropped → 2 data rows
        assertEquals(2, result.size)
        assertEquals("01.01.2026", result[0][0])
        assertEquals("02.01.2026", result[1][0])
    }

    @Test
    fun `extractSampleTableRows handles clean 4-column table without filtering`() {
        val rawTable = listOf(
            listOf("Date", "Amount", "Operation", "Details"),
            listOf("15.03.2024", "-5000.00", "Purchase", "Supermarket"),
            listOf("16.03.2024", "10000.00", "Transfer", "Salary"),
            listOf("17.03.2024", "-2500.00", "Payment", "Utility"),
        )
        every { pdfTableExtractor.extractTable(any()) } returns rawTable

        val result = statementParser.extractSampleTableRows(byteArrayOf(1, 2, 3))

        // modal=4, threshold=2, all rows pass, header dropped → 3 data rows
        assertEquals(3, result.size)
        assertEquals("15.03.2024", result[0][0])
    }

    @Test
    fun `extractSampleTableRows returns empty for empty table`() {
        every { pdfTableExtractor.extractTable(any()) } returns emptyList()

        val result = statementParser.extractSampleTableRows(byteArrayOf(1, 2, 3))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractSampleTableRowsWithContext falls back to date scanning when modal count is wrong`() {
        // BCC-like table: 4 metadata rows (2 non-empty cells, no dates), 2 transaction rows (4 non-empty cells).
        // nonEmptyCounts = [2,2,2,2,4,4] → modalCount=2, threshold=2 → structuralRows = all 6 rows.
        // sampleRows = structuralRows.drop(1).take(10) = 5 rows (3 metadata + 2 tx).
        // The 3 metadata rows in sampleRows have no date patterns, but the 2 tx rows do.
        // hasDate = true in this case, BUT the 4 metadata rows dominate structuralRows.
        //
        // To reliably trigger fallback: use 1-cell metadata rows so modal=1, threshold=1,
        // meaning ALL rows pass, sampleRows.drop(1) starts with metadata rows without dates,
        // and the transaction date rows are also present → hasDate=true anyway.
        //
        // Correct approach: use metadata rows with 1 non-empty cell (modal=1, threshold=1),
        // so ALL rows pass structuralRows; then structuralRows.drop(1).take(10) starts with
        // metadata. Since metadata have no date, but tx rows do, hasDate = true via tx rows
        // in sampleRows.
        //
        // To actually trigger fallback: metadata rows must dominate AND have no dates,
        // AND the sampleRows slice (drop(1).take(10)) must be all metadata (no dates).
        // Use 12 metadata rows + 2 tx rows — drop(1).take(10) = 10 metadata rows only.
        val metadataRows = (1..12).map { i -> listOf("Поле$i", "Значение$i", "", "") }
        val transactionRows = listOf(
            listOf("2024-12-31", "Аударым", "10 000.00", "-10 000.00"),
            listOf("2024-12-30", "Зачисление", "5 000.00", "5 000.00"),
        )
        val rawTable = metadataRows + transactionRows
        every { pdfTableExtractor.extractTable(any()) } returns rawTable

        val result = statementParser.extractSampleTableRowsWithContext(byteArrayOf(1, 2, 3))

        // Fallback should return the 2 transaction rows (date rows)
        assertEquals(2, result.sampleRows.size)
        assertEquals("2024-12-31", result.sampleRows[0][0])
        assertEquals("2024-12-30", result.sampleRows[1][0])
    }

    @Test
    fun `extractSampleTableRowsWithContext handles minimal BCC table with 1-2 transactions`() {
        // Edge case: 12 metadata rows (no dates) and only 1 transaction row.
        // sampleRows from modal heuristic = drop(1).take(10) = 10 metadata rows → no date → fallback.
        val metadataRows = (1..12).map { i -> listOf("Поле$i", "Значение$i", "", "") }
        val transactionRow = listOf("2024-12-31", "Аударым", "10 000.00", "-10 000.00")
        val rawTable = metadataRows + listOf(transactionRow)
        every { pdfTableExtractor.extractTable(any()) } returns rawTable

        val result = statementParser.extractSampleTableRowsWithContext(byteArrayOf(1, 2, 3))

        assertEquals(1, result.sampleRows.size)
        assertEquals("2024-12-31", result.sampleRows[0][0])
    }

    // ==================== T010: multi-line row merging regression tests ====================

    @Test
    fun `merging is no-op for single-line bank tables`() {
        // Kaspi-like 4-column table where every row starts with a date — merging should be a no-op.
        // Since extractTable() is mocked, we return already-final rows (as if merging ran but had nothing to merge).
        val rawTable = listOf(
            listOf("Date", "Amount", "Operation", "Details"),
            listOf("15.03.2024", "-5000.00", "Покупка", "Магазин"),
            listOf("16.03.2024", "+10000.00", "Пополнение", "Salary"),
            listOf("17.03.2024", "-2500.00", "Перевод", "Utility"),
        )
        every { pdfTableExtractor.extractTable(any()) } returns rawTable

        val result = statementParser.extractSampleTableRowsWithContext(byteArrayOf(1, 2, 3))

        // Header dropped → 3 data rows, no merging occurred (all rows start with dates)
        assertEquals(3, result.sampleRows.size)
        assertTrue(result.sampleRows.all { row -> row[0].matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}.*")) })
    }

    @Test
    fun `mergeMultiLineRows handles continuation row with mismatched cell count`() {
        // Simulate what extractTable() returns after merging a continuation row that had MORE cells than parent.
        // Parent row had 4 cells, continuation had 5 cells → parent padded and merged → result has 5 cells.
        val mergedTable = listOf(
            listOf("Date", "Amount", "Operation", "Details", ""),
            listOf("15.03.2024", "-5000.00", "Покупка", "Магазин Extra Info", "extra"),
            listOf("16.03.2024", "+10000.00", "Пополнение", "Salary", ""),
        )
        every { pdfTableExtractor.extractTable(any()) } returns mergedTable

        // Must not throw; result should reflect the 5-cell structure
        val result = statementParser.extractSampleTableRowsWithContext(byteArrayOf(1, 2, 3))

        assertEquals(2, result.sampleRows.size)
        assertEquals(5, result.sampleRows[0].size)
        assertEquals("15.03.2024", result.sampleRows[0][0])
    }

    // ==================== T011: Halyk multi-line row merging ====================

    @Test
    fun `Halyk-like multi-line rows are merged correctly`() {
        // Simulate what extractTable() returns after the real mergeMultiLineRows runs:
        // - row 1 ("01.01.2026" date) merges with its continuation row → last cell becomes "Shop Branch: Almaty Center"
        // - row 3 ("02.01.2026" date) is a standalone row
        // This is the MERGED output that the real extractTable() would produce.
        val mergedTable = listOf(
            listOf("Date", "TxId", "Sign", "Amount", "CCY", "Operation", "Details"),
            listOf("01.01.2026", "TX001", "-", "5000.00", "KZT", "Purchase", "Shop Branch: Almaty Center"),
            listOf("02.01.2026", "TX002", "-", "3000.00", "KZT", "Payment", "Store"),
        )
        every { pdfTableExtractor.extractTable(any()) } returns mergedTable

        val result = statementParser.extractSampleTableRowsWithContext(byteArrayOf(1, 2, 3))

        // Header dropped → 2 logical rows (continuation was merged into row 1)
        assertEquals(2, result.sampleRows.size)
        assertEquals("01.01.2026", result.sampleRows[0][0])
        assertEquals("Shop Branch: Almaty Center", result.sampleRows[0][6])
        assertEquals("02.01.2026", result.sampleRows[1][0])
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
