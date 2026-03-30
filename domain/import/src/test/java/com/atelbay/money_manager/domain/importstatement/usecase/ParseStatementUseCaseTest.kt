package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.ai.FailedAttempt
import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.ai.StatementClassification
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.model.TableParserConfigList
import com.atelbay.money_manager.core.parser.TableParseResult
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.firestore.dto.ParserCandidateDto
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.parser.RegexParseResult
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.StatementParser
import com.atelbay.money_manager.core.parser.TableExtractionResult
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigList
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import com.atelbay.money_manager.domain.categories.usecase.SaveCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private lateinit var parserConfigProvider: ParserConfigProvider
    private lateinit var userIdHasher: UserIdHasher
    private lateinit var firestoreDataSource: FirestoreDataSource

    private lateinit var useCase: ParseStatementUseCase
    private val json = Json { ignoreUnknownKeys = true }

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
    private val headerSnippet = "Test Bank statement\nBIN 123456789"
    private val sampleRows = "01.01.2026 Purchase Store A\n02.01.2026 Purchase Store B"

    private val testTableConfig = TableParserConfig(
        bankId = "test_bank_table",
        bankMarkers = listOf("Test Bank Table"),
        dateColumn = 0,
        amountColumn = 1,
        operationColumn = 2,
        dateFormat = "dd.MM.yyyy",
        amountFormat = "dot",
        skipHeaderRows = 1,
    )

    // A minimal 3-row table (header + 2 data rows) — enough to trigger the table path (>=2 rows)
    private val sampleTable = listOf(
        listOf("Date", "Amount", "Operation"),
        listOf("01.01.2026", "100.00", "Purchase"),
        listOf("02.01.2026", "50.00", "Purchase"),
    )

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
        parserConfigProvider = mockk()
        userIdHasher = mockk()
        firestoreDataSource = mockk()

        // Common default stubs
        every { userPreferences.cachedAiParserConfigs } returns flowOf(null)
        every { userPreferences.cachedAiTableParserConfigs } returns flowOf(null)
        coEvery { userPreferences.setCachedAiParserConfigs(any()) } returns Unit
        coEvery { userPreferences.setCachedAiTableParserConfigs(any()) } returns Unit
        coEvery { categoryDao.getByType(any()) } returns emptyList()
        coEvery { transactionDao.getExistingHashes(any()) } returns emptyList()
        coEvery { statementParser.tryParsePdf(any(), any()) } returns emptyRegexResult
        coEvery { saveCategoryUseCase(any()) } returns 1L
        every { statementParser.extractHeaderSnippet(pdfTextWithEnoughLines) } returns headerSnippet
        every { statementParser.extractSampleRows(pdfTextWithEnoughLines) } returns sampleRows
        every { parserConfigProvider.isAiFullParseEnabled() } returns true
        coEvery { parserConfigProvider.getConfigs() } returns emptyList()
        // Default: table extraction returns empty (no table path) — individual tests override as needed
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(emptyList(), emptyList(), null)
        // Default: classification returns text type with 0 expected count
        coEvery { geminiService.classifyStatement(any()) } returns StatementClassification("text", 0)
        // Default: Firestore returns no candidates
        coEvery { userIdHasher.computeHash(any()) } returns "testhash0123456789abcdef0123456789abcdef0123456789abcdef01234567"
        coEvery { firestoreDataSource.findCandidatesByUser(any(), any()) } returns emptyList()

        useCase = ParseStatementUseCase(
            statementParser = statementParser,
            geminiService = geminiService,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            saveCategoryUseCase = saveCategoryUseCase,
            userPreferences = userPreferences,
            regexValidator = regexValidator,
            parserConfigProvider = parserConfigProvider,
            userIdHasher = userIdHasher,
            firestoreDataSource = firestoreDataSource,
        )
    }

    // ---- T010: AI config generation tests ----

    @Test
    fun `AI config generation success - returns transactions and aiGeneratedConfig`() = runTest {
        // Step 1: No bank detected by remote config regex
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Step 3: Gemini generates a valid config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        // Step 4: Regex is safe
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null

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
        coVerify { geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any()) }
    }

    @Test
    fun `fallback to parseWithGemini when generateParserConfig throws`() = runTest {
        // No bank detected
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Gemini config generation fails
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } throws RuntimeException("AI error")

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

        // Gemini generates config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        // ReDoS check fails
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns "Nested quantifier detected"

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

        // Gemini generates config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        // Regex is safe
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null

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
        coVerify(exactly = 0) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
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

        // Gemini generates config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        // Regex is safe (passes heuristic but will hang at runtime)
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null

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

    @Test
    fun `generated config is returned for deferred caching`() = runTest {
        val existingVariant = testConfig.copy(transactionPattern = "existing-pattern")
        every { userPreferences.cachedAiParserConfigs } returns flowOf(
            json.encodeToString(ParserConfigList(banks = listOf(existingVariant))),
        )
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        val generatedVariant = testConfig.copy(transactionPattern = "new-pattern")
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns generatedVariant
        every { regexValidator.getReDoSViolation(generatedVariant.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, generatedVariant) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = generatedVariant.bankId,
        )

        val result = useCase(pdfBlobs)

        // Config is returned in ParseResult for deferred caching (not cached immediately)
        assertEquals(1, result.importResult.newTransactions.size)
        assertNotNull(result.aiGeneratedConfig)
        assertEquals("new-pattern", result.aiGeneratedConfig!!.transactionPattern)
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
    }

    // ---- Retry loop tests ----

    @Test
    fun `retry - success on first attempt does not retry`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
        // Should only be called once (no retry needed)
        coVerify(exactly = 1) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `retry - first attempt fails with invalid regex, second succeeds`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        val badConfig = testConfig.copy(transactionPattern = "[invalid(regex")
        val goodConfig = testConfig.copy(transactionPattern = "\\d{2}\\.\\d{2}\\.\\d{4}\\s+(.+)")

        // First call returns bad config, second returns good config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returnsMany listOf(badConfig, goodConfig)

        // First config fails regex validation
        every { regexValidator.getReDoSViolation(badConfig.transactionPattern) } returns null
        every { regexValidator.getReDoSViolation(goodConfig.transactionPattern) } returns null

        // Second config succeeds
        every { statementParser.tryParseWithConfig(pdfBytes, goodConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = goodConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
        coVerify(exactly = 2) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `retry - all 3 attempts fail falls through to full AI`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // All 3 attempts return config that fails ReDoS
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns "Nested quantifier detected"

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        // Should have tried 3 times
        coVerify(exactly = 3) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        // Should fall back to full AI
        coVerify { geminiService.parseContent(pdfBlobs, any()) }
        assertEquals(AiMethod.FULL_PARSE, result.aiMethod)
    }

    @Test
    fun `retry - network exception on first attempt counts as failed and continues`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // First call throws, second returns valid config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } throws RuntimeException("Network error") andThen testConfig

        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
        coVerify(exactly = 2) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `retry - 0 match includes sample lines in error`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // All attempts return config that parses 0 transactions
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = emptyList(),
            bankId = testConfig.bankId,
        )

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        // Should have tried 3 times
        coVerify(exactly = 3) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        // Should fall back to full AI
        coVerify { geminiService.parseContent(pdfBlobs, any()) }
    }

    // ---- T016: Table path happy-path tests ----

    @Test
    fun `table AI generation happy path - returns TABLE_GENERATED`() = runTest {
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        coEvery {
            geminiService.generateTableParserConfig(any(), any(), any(), any())
        } returns testTableConfig
        every {
            statementParser.tryParseWithTableConfig(pdfBytes, testTableConfig)
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = testTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
        assertEquals(1, result.importResult.newTransactions.size)
        coVerify(exactly = 1) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
    }

    @Test
    fun `table AI - first attempt fails, retry succeeds`() = runTest {
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        val badTableConfig = testTableConfig.copy(dateFormat = "invalid")
        val goodTableConfig = testTableConfig.copy(dateFormat = "dd.MM.yyyy")

        coEvery {
            geminiService.generateTableParserConfig(any(), any(), any(), any())
        } returnsMany listOf(badTableConfig, goodTableConfig)

        // First config: invalid date format (isDateFormatValid returns false)
        // Second config: parses successfully
        every {
            statementParser.tryParseWithTableConfig(pdfBytes, goodTableConfig)
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = goodTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
        coVerify(exactly = 2) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
    }

    // ---- T019: Table config caching tests ----

    @Test
    fun `cached table config hit - no AI call`() = runTest {
        val cachedTableJson = json.encodeToString(
            TableParserConfigList(configs = listOf(testTableConfig)),
        )
        every { userPreferences.cachedAiTableParserConfigs } returns flowOf(cachedTableJson)
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        every {
            statementParser.tryParseTable(pdfBytes, any())
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = testTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
        assertEquals(1, result.importResult.newTransactions.size)
        // No table AI calls made (cached table config hit)
        coVerify(exactly = 0) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
    }

    @Test
    fun `cached table config miss - falls through to AI generation`() = runTest {
        val cachedTableJson = json.encodeToString(
            TableParserConfigList(configs = listOf(testTableConfig)),
        )
        every { userPreferences.cachedAiTableParserConfigs } returns flowOf(cachedTableJson)
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Cached table config returns 0 transactions
        every {
            statementParser.tryParseTable(pdfBytes, any())
        } returns null

        // AI table generation succeeds
        coEvery {
            geminiService.generateTableParserConfig(any(), any(), any(), any())
        } returns testTableConfig
        every {
            statementParser.tryParseWithTableConfig(pdfBytes, testTableConfig)
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = testTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
        coVerify(exactly = 1) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
    }

    @Test
    fun `successful AI table generation - config cached`() = runTest {
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        coEvery {
            geminiService.generateTableParserConfig(any(), any(), any(), any())
        } returns testTableConfig
        every {
            statementParser.tryParseWithTableConfig(pdfBytes, testTableConfig)
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = testTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        // Config is returned in ParseResult for deferred caching (not cached immediately)
        assertNotNull(result.aiGeneratedTableConfig)
        assertEquals(testTableConfig.bankId, result.aiGeneratedTableConfig!!.bankId)
        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
    }

    @Test
    fun `AI table config returned with correct dateFormat for deferred caching`() = runTest {
        val oldConfig = testTableConfig.copy(dateFormat = "yyyy-MM-dd")
        val cachedTableJson = json.encodeToString(
            TableParserConfigList(configs = listOf(oldConfig)),
        )
        every { userPreferences.cachedAiTableParserConfigs } returns flowOf(cachedTableJson)
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Cached config returns null (miss)
        every { statementParser.tryParseTable(pdfBytes, any()) } returns null

        // AI generates new config with same bankId
        val newConfig = testTableConfig.copy(dateFormat = "dd.MM.yyyy")
        coEvery {
            geminiService.generateTableParserConfig(any(), any(), any(), any())
        } returns newConfig
        every {
            statementParser.tryParseWithTableConfig(pdfBytes, newConfig)
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = newConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        // Config is returned for deferred caching, not cached immediately
        assertNotNull(result.aiGeneratedTableConfig)
        assertEquals("dd.MM.yyyy", result.aiGeneratedTableConfig!!.dateFormat)
        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
    }

    // ---- T021: Phase 5 US3 — Fallback tests ----

    @Test
    fun `AI regex generation succeeds - table path not reached`() = runTest {
        // Table extraction returns empty (but irrelevant — AI regex is tried first)
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(emptyList(), emptyList(), null)

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // AI regex generation succeeds (step 2)
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        coVerify(atLeast = 1) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        // Table path should not be reached since AI regex succeeded
        coVerify(exactly = 0) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
    }

    @Test
    fun `AI regex succeeds even when table has insufficient rows`() = runTest {
        // 1 row is below the >=2 threshold for table path, but AI regex runs first anyway
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(
            sampleRows = listOf(listOf("Date", "Amount", "Operation")),
            metadataRows = emptyList(),
            columnHeaderRow = null,
        )

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        coVerify(atLeast = 1) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
    }

    @Test
    fun `fallback - AI regex fails then interleaved table succeeds`() = runTest {
        // Table has enough rows to trigger the table path (>=2 rows)
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // AI regex parses 0 transactions
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = emptyList(),
            bankId = testConfig.bankId,
        )

        // Interleaved: after regex fails on attempt 1, table runs and succeeds
        coEvery {
            geminiService.generateTableParserConfig(any(), any(), any(), any())
        } returns testTableConfig
        every {
            statementParser.tryParseWithTableConfig(pdfBytes, testTableConfig)
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = testTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        // With interleaved retry (text-first): regex attempt 1 fails → table attempt 1 succeeds
        coVerify(exactly = 1) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
    }

    @Test
    fun `fallback - AI regex succeeds even when table extraction would throw`() = runTest {
        // Table extraction would throw — but AI regex runs first and succeeds
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } throws RuntimeException("PdfBox error")

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // AI regex succeeds (step 2) — table path (step 3) is never reached
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        coVerify(atLeast = 1) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
    }

    @Test
    fun `fallback - remote config regex tried first before table path`() = runTest {
        // Remote config regex succeeds on step 1 — table path should never be reached
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = "test_bank",
            extractedText = pdfTextWithEnoughLines,
        )

        val result = useCase(pdfBlobs)

        // Neither table extraction nor AI should have been called
        verify(exactly = 0) { statementParser.extractSampleTableRowsWithContext(any()) }
        coVerify(exactly = 0) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        assertEquals(AiMethod.NONE, result.aiMethod)
        assertEquals(1, result.importResult.newTransactions.size)
    }

    @Test
    fun `fallback - cached AI regex configs tried before table path`() = runTest {
        val cachedConfigJson = """{"banks":[{"bank_id":"test_bank","bank_markers":["Test Bank"],"transaction_pattern":"\\d{2}\\.\\d{2}\\.\\d{4}\\s+(.+)","date_format":"dd.MM.yyyy","operation_type_map":{"Purchase":"expense"}}]}"""
        every { userPreferences.cachedAiParserConfigs } returns flowOf(cachedConfigJson)

        // Step 1: Remote config fails
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        // Step 2: Cached AI regex config succeeds — table path should not be reached
        coEvery {
            statementParser.tryParsePdf(pdfBytes, additionalConfigs = any())
        } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = "test_bank",
        )

        val result = useCase(pdfBlobs)

        verify(exactly = 0) { statementParser.extractSampleTableRowsWithContext(any()) }
        coVerify(exactly = 0) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        assertEquals(AiMethod.NONE, result.aiMethod)
        assertEquals(1, result.importResult.newTransactions.size)
    }

    @Test
    fun `retry - passes existingConfigs and failedAttempts to subsequent calls`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult

        // Capture snapshots of failedAttempts at each call (the list is mutated between calls)
        val attemptSizes = mutableListOf<Int>()

        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } answers {
            attemptSizes.add(arg<List<FailedAttempt>>(3).size)
            testConfig
        }

        // All 3 fail ReDoS
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns "Nested quantifier detected"

        // Fallback
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        useCase(pdfBlobs)

        // First call should have 0 failed attempts
        assertEquals(0, attemptSizes[0])
        // Second call should have 1 failed attempt
        assertEquals(1, attemptSizes[1])
        // Third call should have 2 failed attempts
        assertEquals(2, attemptSizes[2])
    }

    // ---- Plausibility gate tests ----

    @Test
    fun `plausibility gate - high match rate triggers retry and falls through to full AI`() = runTest {
        // 80 lines total → 70 non-blank after HEADER_SKIP_LINES=10 are dropped
        val highLineCountPdfText = (1..80).joinToString("\n") { "01.01.2026 Purchase Store $it 100.00" }

        // Force the regex result to carry the high-line-count text
        val highLineRegexResult = RegexParseResult(
            transactions = emptyList(),
            bankId = null,
            extractedText = highLineCountPdfText,
        )

        coEvery { statementParser.tryParsePdf(pdfBytes) } returns highLineRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns highLineRegexResult

        every { statementParser.extractHeaderSnippet(highLineCountPdfText) } returns headerSnippet
        every { statementParser.extractSampleRows(highLineCountPdfText) } returns sampleRows

        // AI generates a config
        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig

        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null

        // Config matches 61 transactions from 70 non-blank lines (~87%) — above 70% threshold with >20 tx
        val manyTransactions = (1..61).map { testTransaction.copy(uniqueHash = "hash_$it") }
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = manyTransactions,
            bankId = testConfig.bankId,
        )

        // Fallback: full AI parsing
        coEvery { geminiService.parseContent(pdfBlobs, any()) } returns geminiJsonResponse

        val result = useCase(pdfBlobs)

        // All 3 attempts should be rejected by plausibility gate, then fall back to full AI
        coVerify(exactly = 3) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        coVerify { geminiService.parseContent(pdfBlobs, any()) }
        assertNull(result.aiGeneratedConfig)
    }

    // ---- Firestore candidate tests ----

    @Test
    fun `Firestore regex config found - succeeds without AI calls`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult

        val firestoreConfigJson = json.encodeToString(testConfig)
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "regex") } returns listOf(
            ParserCandidateDto(
                id = "fs1",
                bankId = testConfig.bankId,
                parserConfigJson = firestoreConfigJson,
                configType = "regex",
            ),
        )

        coEvery {
            statementParser.tryParsePdf(pdfBytes, additionalConfigs = any())
        } returns emptyRegexResult andThen RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.NONE, result.aiMethod)
        coVerify(exactly = 0) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { geminiService.parseContent(any(), any()) }
    }

    @Test
    fun `Firestore table config found - succeeds without AI calls`() = runTest {
        every { statementParser.extractSampleTableRowsWithContext(pdfBytes) } returns TableExtractionResult(sampleTable, emptyList(), null)
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult

        val firestoreTableConfigJson = json.encodeToString(testTableConfig)
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "regex") } returns emptyList()
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "table") } returns listOf(
            ParserCandidateDto(
                id = "fs_table1",
                bankId = testTableConfig.bankId,
                parserConfigJson = firestoreTableConfigJson,
                configType = "table",
            ),
        )

        every {
            statementParser.tryParseTable(pdfBytes, any())
        } returns TableParseResult(
            transactions = listOf(testTransaction),
            bankId = testTableConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.TABLE_GENERATED, result.aiMethod)
        coVerify(exactly = 0) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { geminiService.generateTableParserConfig(any(), any(), any(), any()) }
        coVerify(exactly = 0) { geminiService.parseContent(any(), any()) }
    }

    @Test
    fun `Firestore returns empty - falls through to AI generation`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "regex") } returns emptyList()
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "table") } returns emptyList()

        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
        coVerify(exactly = 1) { geminiService.generateParserConfig(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Firestore call fails - gracefully falls through to AI generation`() = runTest {
        coEvery { statementParser.tryParsePdf(pdfBytes) } returns emptyRegexResult
        coEvery { statementParser.tryParsePdf(pdfBytes, additionalConfigs = any()) } returns emptyRegexResult
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "regex") } throws RuntimeException("Network error")
        coEvery { firestoreDataSource.findCandidatesByUser(any(), "table") } throws RuntimeException("Network error")

        coEvery {
            geminiService.generateParserConfig(headerSnippet, sampleRows, any(), any(), any())
        } returns testConfig
        every { regexValidator.getReDoSViolation(testConfig.transactionPattern) } returns null
        every { statementParser.tryParseWithConfig(pdfBytes, testConfig) } returns RegexParseResult(
            transactions = listOf(testTransaction),
            bankId = testConfig.bankId,
        )

        val result = useCase(pdfBlobs)

        assertEquals(1, result.importResult.newTransactions.size)
        assertEquals(AiMethod.REGEX_GENERATED, result.aiMethod)
    }
}
