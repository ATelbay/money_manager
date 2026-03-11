package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.parser.RegexParseResult
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.StatementParser
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.domain.categories.usecase.SaveCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParseStatementUseCaseTest {

    private lateinit var statementParser: StatementParser
    private lateinit var geminiService: GeminiService
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var saveCategoryUseCase: SaveCategoryUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var regexValidator: RegexValidator

    private lateinit var useCase: ParseStatementUseCase

    private val testConfig = ParserConfig(
        bankId = "test_bank",
        bankMarkers = listOf("Test Bank"),
        transactionPattern = "\\d{2}\\.\\d{2}\\.\\d{4}\\s+(.+)",
        dateFormat = "dd.MM.yyyy",
        operationTypeMap = mapOf("Purchase" to "expense"),
    )

    private val testTransaction = ParsedTransaction(
        date = LocalDate(2026, 1, 1),
        amount = 100.0,
        type = TransactionType.EXPENSE,
        details = "Test purchase",
        categoryId = null,
        suggestedCategoryName = null,
        confidence = 1.0f,
        needsReview = false,
        uniqueHash = "test_hash",
    )

    private val pdfBytes = "fake pdf content".toByteArray()
    private val pdfBlobs = listOf(pdfBytes to "application/pdf")

    // Enough lines so extractSampleRows returns non-empty
    private val pdfTextWithEnoughLines = (1..20).joinToString("\n") { "Line $it with data" }

    private val emptyRegexResult = RegexParseResult(
        transactions = emptyList(),
        bankId = null,
        extractedText = pdfTextWithEnoughLines,
    )

    private val geminiJsonResponse =
        """{"tx":[{"d":"2026-01-01","a":100.0,"t":"e","det":"Test","conf":0.9}]}"""

    @Before
    fun setUp() {
        statementParser = mockk()
        geminiService = mockk()
        categoryDao = mockk()
        transactionDao = mockk()
        saveCategoryUseCase = mockk()
        userPreferences = mockk()
        regexValidator = mockk()

        // Common default stubs
        every { userPreferences.cachedAiParserConfigs } returns flowOf(null)
        coEvery { categoryDao.getByType(any()) } returns emptyList()
        coEvery { transactionDao.getExistingHashes(any()) } returns emptyList()
        coEvery { statementParser.tryParsePdf(any(), any()) } returns emptyRegexResult
        coEvery { saveCategoryUseCase(any()) } returns 1L

        useCase = ParseStatementUseCase(
            statementParser = statementParser,
            geminiService = geminiService,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            saveCategoryUseCase = saveCategoryUseCase,
            userPreferences = userPreferences,
            regexValidator = regexValidator,
        )
    }

    // ---- T010: AI config generation tests ----

    @Test
    fun `AI config generation success - returns transactions and aiGeneratedConfig`() = runTest {
        // Step 1: No bank detected by remote config regex
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Step 2: Sample rows extracted from text already in regexResult
        every { statementParser.extractSampleRows(pdfTextWithEnoughLines) } returns "sample row data"

        // Step 3: Gemini generates a valid config
        coEvery { geminiService.generateParserConfig("sample row data") } returns testConfig

        // Step 4: Regex is safe
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true

        // Step 5: Config parses transactions successfully
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        // Step 6: Cache config (stub the setter)
        coEvery { userPreferences.setCachedAiParserConfigs(any()) } returns Unit

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertNotNull(result.aiGeneratedConfig)
        assertEquals("test_bank", result.aiGeneratedConfig?.bankId)
        assertNotNull(result.sampleRows)
    }

    @Test
    fun `fallback to parseWithGemini when generateParserConfig throws`() = runTest {
        // No bank detected
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Sample rows
        every { statementParser.extractSampleRows(pdfTextWithEnoughLines) } returns "sample row data"

        // Gemini config generation fails
        coEvery { geminiService.generateParserConfig("sample row data") } throws RuntimeException("AI error")

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        // Should fall back to Gemini parseContent
        coVerify { geminiService.parseContent(pdfBlobs, any()) }
        assertNull(result.aiGeneratedConfig)
        assertTrue(result.importResult.newTransactions.isNotEmpty())
    }

    @Test
    fun `fallback to parseWithGemini when regex is ReDoS-vulnerable`() = runTest {
        // No bank detected
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Sample rows
        every { statementParser.extractSampleRows(pdfTextWithEnoughLines) } returns "sample row data"

        // Gemini generates config
        coEvery { geminiService.generateParserConfig("sample row data") } returns testConfig

        // ReDoS check fails
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns false

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        coVerify { geminiService.parseContent(pdfBlobs, any()) }
        coVerify(exactly = 0) { statementParser.tryParseWithConfig(any(), any()) }
        assertNull(result.aiGeneratedConfig)
        assertTrue(result.importResult.newTransactions.isNotEmpty())
    }

    @Test
    fun `fallback to parseWithGemini when generated config parses 0 transactions`() = runTest {
        // No bank detected
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Sample rows
        every { statementParser.extractSampleRows(pdfTextWithEnoughLines) } returns "sample row data"

        // Gemini generates config
        coEvery { geminiService.generateParserConfig("sample row data") } returns testConfig

        // Regex is safe
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true

        // Config parses 0 transactions
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = emptyList(),
            bankId = testConfig.bankId,
        )

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        coVerify { geminiService.parseContent(pdfBlobs, any()) }
        assertNull(result.aiGeneratedConfig)
        assertTrue(result.importResult.newTransactions.isNotEmpty())
    }

    @Test
    fun `cached config is used on second call - AI is NOT called`() = runTest {
        val cachedConfigJson = """{"banks":[{"bank_id":"test_bank","bank_markers":["Test Bank"],"transaction_pattern":"\\d{2}\\.\\d{2}\\.\\d{4}\\s+(.+)","date_format":"dd.MM.yyyy","operation_type_map":{"Purchase":"expense"}}]}"""

        // Cached configs exist in DataStore
        every { userPreferences.cachedAiParserConfigs } returns flowOf(cachedConfigJson)

        // Step 1: Remote config regex returns empty (no bank)
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult

        // Step 2: Cached config matches and parses
        coEvery {
            statementParser.tryParsePdf(pdfBytes, additionalConfigs = any())
        } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = "test_bank",
        )

        val result = useCase(pdfBlobs)

        // AI should NOT be called
        coVerify(exactly = 0) { geminiService.generateParserConfig(any()) }
        coVerify(exactly = 0) { geminiService.parseContent(any(), any()) }

        assertEquals(1, result.importResult.newTransactions.size)
        assertNull(result.aiGeneratedConfig)
    }

    // ---- T012: Timeout guard tests ----

    @Test
    fun `timeout guard - regex times out falls back to Gemini`() = runTest {
        // No bank detected
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Sample rows
        every { statementParser.extractSampleRows(pdfTextWithEnoughLines) } returns "sample row data"

        // Gemini generates config
        coEvery { geminiService.generateParserConfig("sample row data") } returns testConfig

        // Regex is safe (passes heuristic but will hang at runtime)
        every { regexValidator.isReDoSSafe(testConfig.transactionPattern) } returns true

        // tryParseWithConfig blocks via Thread.sleep — runInterruptible will interrupt it
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } answers {
            Thread.sleep(10_000) // blocks longer than the 5s timeout
            RegexParseResult(transactions = emptyList(), bankId = "test_bank")
        }

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        // Should fall back to full AI parsing due to timeout
        coVerify { geminiService.parseContent(pdfBlobs, any()) }
        assertNull(result.aiGeneratedConfig)
        assertTrue(result.importResult.newTransactions.isNotEmpty())
    }
}
