package com.atelbay.money_manager.core.ai

import com.atelbay.money_manager.core.model.TableParserProfile
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfileProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class GeminiServiceImplTest {

    private lateinit var configProvider: RegexParserProfileProvider
    private lateinit var service: GeminiServiceImpl

    // Use reflection to access private methods for testing
    private lateinit var parseRegexParserProfileResponseMethod: Method
    private lateinit var selectExamplesMethod: Method
    private lateinit var buildPromptMethod: Method
    private lateinit var parseTableParserProfileResponseMethod: Method
    private lateinit var buildTableParserProfilePromptMethod: Method

    @Before
    fun setUp() {
        configProvider = mockk()
        every { configProvider.getGeminiModelName() } returns "gemini-3-flash-preview"
        service = GeminiServiceImpl(configProvider)

        // Access private methods via reflection
        parseRegexParserProfileResponseMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "parseRegexParserProfileResponse", String::class.java
        ).apply { isAccessible = true }

        selectExamplesMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "selectExamplesForPrompt", List::class.java
        ).apply { isAccessible = true }

        buildPromptMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "buildRegexParserProfilePrompt",
            String::class.java,
            String::class.java,
            List::class.java,
            List::class.java,
            Boolean::class.java,
            CategoryNames::class.java,
        ).apply { isAccessible = true }

        parseTableParserProfileResponseMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "parseTableParserProfileResponse", String::class.java
        ).apply { isAccessible = true }

        buildTableParserProfilePromptMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "buildTableParserProfilePrompt",
            List::class.java,
            List::class.java,
            List::class.java,
            List::class.java,
            CategoryNames::class.java,
        ).apply { isAccessible = true }
    }

    // Helper to invoke parseRegexParserProfileResponse via reflection
    private fun parseResponse(json: String): RegexParserProfile =
        parseRegexParserProfileResponseMethod.invoke(service, json) as RegexParserProfile

    @Suppress("UNCHECKED_CAST")
    private fun selectExamples(configs: List<RegexParserProfile>): List<RegexParserProfile> =
        selectExamplesMethod.invoke(service, configs) as List<RegexParserProfile>

    private fun buildPrompt(
        header: String, samples: String,
        configs: List<RegexParserProfile>, attempts: List<FailedAttempt>,
        hasPdfBlob: Boolean = false,
        categoryNames: CategoryNames = CategoryNames(),
    ): String = buildPromptMethod.invoke(service, header, samples, configs, attempts, hasPdfBlob, categoryNames) as String

    // --- parseRegexParserProfileResponse tests ---

    @Test
    fun `parseRegexParserProfileResponse reads operation_type_map from JSON array`() {
        val json = """
        {
            "bank_id": "test_bank",
            "bank_markers": ["Test"],
            "transaction_pattern": "\\d+",
            "date_format": "dd.MM.yyyy",
            "operation_type_map": [
                {"key": "Purchase", "value": "expense"},
                {"key": "Top-up", "value": "income"}
            ]
        }
        """.trimIndent()

        val config = parseResponse(json)
        assertEquals(mapOf("Purchase" to "expense", "Top-up" to "income"), config.operationTypeMap)
    }

    @Test
    fun `parseRegexParserProfileResponse returns empty map when operation_type_map is missing`() {
        val json = """
        {
            "bank_id": "test_bank",
            "bank_markers": ["Test"],
            "transaction_pattern": "\\d+",
            "date_format": "dd.MM.yyyy"
        }
        """.trimIndent()

        val config = parseResponse(json)
        assertEquals(emptyMap<String, String>(), config.operationTypeMap)
    }

    @Test
    fun `parseRegexParserProfileResponse returns empty map when operation_type_map is empty array`() {
        val json = """
        {
            "bank_id": "test_bank",
            "bank_markers": ["Test"],
            "transaction_pattern": "\\d+",
            "date_format": "dd.MM.yyyy",
            "operation_type_map": []
        }
        """.trimIndent()

        val config = parseResponse(json)
        assertEquals(emptyMap<String, String>(), config.operationTypeMap)
    }

    @Test
    fun `parseRegexParserProfileResponse converts Python named groups to Java syntax`() {
        val json = """
        {
            "bank_id": "test_bank",
            "bank_markers": ["Test"],
            "transaction_pattern": "(?P<date>\\d+)\\s+(?P<amount>\\d+)",
            "date_format": "dd.MM.yyyy"
        }
        """.trimIndent()

        val config = parseResponse(json)
        assertTrue(config.transactionPattern.contains("(?<date>"))
        assertTrue(!config.transactionPattern.contains("(?P<"))
    }

    // --- selectExamplesForPrompt tests ---

    @Test
    fun `selectExamplesForPrompt returns all configs when 3 or fewer`() {
        val configs = listOf(
            makeConfig("bank1"), makeConfig("bank2"),
        )
        val result = selectExamples(configs)
        assertEquals(2, result.size)
    }

    @Test
    fun `selectExamplesForPrompt selects at most 3 from 5 configs with diversity`() {
        val configs = listOf(
            makeConfig("bank1", amountFormat = "space_comma"),
            makeConfig("bank2", amountFormat = "comma_dot"),
            makeConfig("bank3", amountFormat = "dot"),
            makeConfig("bank4", amountFormat = "space_comma", useSignForType = true),
            makeConfig("bank5", amountFormat = "space_comma"),
        )
        val result = selectExamples(configs)
        assertEquals(3, result.size)
    }

    // --- buildRegexParserProfilePrompt tests ---

    @Test
    fun `prompt with 0 existing configs has no Working examples section`() {
        val prompt = buildPrompt("header", "samples", emptyList(), emptyList())
        assertTrue(!prompt.contains("## Working examples"))
    }

    @Test
    fun `prompt with 2 existing configs contains Working examples section`() {
        val configs = listOf(makeConfig("bank1"), makeConfig("bank2"))
        val prompt = buildPrompt("header", "samples", configs, emptyList())
        assertTrue(prompt.contains("## Working examples"))
        assertTrue(prompt.contains("bank1"))
        assertTrue(prompt.contains("bank2"))
    }

    @Test
    fun `prompt with failed attempts contains Previous failed attempts section`() {
        val attempts = listOf(
            FailedAttempt(makeConfig("bad_bank"), "Regex syntax invalid: unclosed group"),
        )
        val prompt = buildPrompt("header", "samples", emptyList(), attempts)
        assertTrue(prompt.contains("## Previous failed attempts"))
        assertTrue(prompt.contains("Regex syntax invalid: unclosed group"))
    }

    @Test
    fun `prompt with empty failed attempts has no Previous failed attempts section`() {
        val prompt = buildPrompt("header", "samples", emptyList(), emptyList())
        assertTrue(!prompt.contains("## Previous failed attempts"))
    }

    @Test
    fun `prompt is entirely in English`() {
        val prompt = buildPrompt("header", "samples", emptyList(), emptyList())
        // Should NOT contain Russian text from old prompt
        assertTrue(!prompt.contains("Ты —"))
        assertTrue(!prompt.contains("ВАЖНО"))
        assertTrue(!prompt.contains("Правила"))
        // Should contain English markers
        assertTrue(prompt.contains("You are an expert"))
        assertTrue(prompt.contains("Rules for"))
    }

    // Helper invokers for table config methods
    private fun parseTableResponse(json: String): TableParserProfile =
        parseTableParserProfileResponseMethod.invoke(service, json) as TableParserProfile

    private fun buildTablePrompt(
        sampleTableRows: List<List<String>>,
        previousAttempts: List<TableFailedAttempt>,
        metadataRows: List<List<String>> = emptyList(),
        columnHeaderRow: List<String>? = null,
        categoryNames: CategoryNames = CategoryNames(),
    ): String = buildTableParserProfilePromptMethod.invoke(
        service, sampleTableRows, previousAttempts, metadataRows, columnHeaderRow, categoryNames,
    ) as String

    // --- generateTableParserProfile tests (via reflection on private methods) ---

    @Test
    fun `generateTableParserProfile returns valid config from AI response`() {
        val json = """
        {
            "bank_id": "forte",
            "bank_markers": ["Forte Bank", "АО Форте"],
            "date_column": 0,
            "amount_column": 3,
            "operation_column": 1,
            "details_column": 2,
            "date_format": "dd.MM.yyyy",
            "amount_format": "space_comma",
            "negative_sign_means_expense": true,
            "skip_header_rows": 1,
            "deduplicate_max_amount": false
        }
        """.trimIndent()

        val config = parseTableResponse(json)

        assertEquals("forte", config.bankId)
        assertEquals(listOf("Forte Bank", "АО Форте"), config.bankMarkers)
        assertEquals(0, config.dateColumn)
        assertEquals(3, config.amountColumn)
        assertEquals(1, config.operationColumn)
        assertEquals(2, config.detailsColumn)
        assertNull(config.signColumn)
        assertNull(config.currencyColumn)
        assertEquals("dd.MM.yyyy", config.dateFormat)
        assertEquals("space_comma", config.amountFormat)
        assertTrue(config.negativeSignMeansExpense)
        assertEquals(1, config.skipHeaderRows)
        assertEquals(false, config.deduplicateMaxAmount)
    }

    @Test
    fun `generateTableParserProfile retry prompt includes previous attempts`() {
        val failedConfig = TableParserProfile(
            bankId = "bereke",
            bankMarkers = listOf("Bereke"),
            dateColumn = 0,
            amountColumn = 1,
            dateFormat = "MM/dd/yyyy",
        )
        val attempts = listOf(
            TableFailedAttempt(
                config = failedConfig,
                error = "Column index 1 out of bounds for row with 1 column(s)",
                failedRows = listOf("[\"01/15/2024\"]"),
            ),
        )
        val sampleRows = listOf(
            listOf("Date", "Debit", "Credit", "Balance", "Description"),
            listOf("01/15/2024", "1 000,00", "", "50 000,00", "Purchase"),
        )

        val prompt = buildTablePrompt(sampleRows, attempts)

        assertTrue(prompt.contains("## Previous failed attempts"))
        assertTrue(prompt.contains("Column index 1 out of bounds for row with 1 column(s)"))
        assertTrue(prompt.contains("bereke"))
        assertTrue(prompt.contains("Column index 1 out of bounds"))
        assertTrue(prompt.contains("01/15/2024"))
    }

    @Test
    fun `parseTableParserProfileResponse throws when date_column is missing`() {
        val json = """
        {
            "bank_id": "forte",
            "bank_markers": ["Forte Bank"],
            "amount_column": 3,
            "date_format": "dd.MM.yyyy"
        }
        """.trimIndent()

        try {
            parseTableResponse(json)
            fail("Expected an exception when date_column is missing")
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            assertNotNull(cause)
            assertTrue(
                "Expected IllegalArgumentException but got ${cause?.javaClass?.simpleName}",
                cause is IllegalArgumentException,
            )
            assertTrue(cause!!.message!!.contains("date_column"))
        }
    }

    @Test
    fun `parseTableParserProfileResponse throws when amount_column is missing`() {
        val json = """
        {
            "bank_id": "forte",
            "bank_markers": ["Forte Bank"],
            "date_column": 0,
            "date_format": "dd.MM.yyyy"
        }
        """.trimIndent()

        try {
            parseTableResponse(json)
            fail("Expected an exception when amount_column is missing")
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            assertNotNull(cause)
            assertTrue(
                "Expected IllegalArgumentException but got ${cause?.javaClass?.simpleName}",
                cause is IllegalArgumentException,
            )
            assertTrue(cause!!.message!!.contains("amount_column"))
        }
    }

    @Test
    fun `generateTableParserProfile handles malformed AI response`() {
        val malformedJson = "{ this is not valid json }"

        try {
            parseTableResponse(malformedJson)
            fail("Expected an exception for malformed JSON")
        } catch (e: InvocationTargetException) {
            // The private method throws through reflection wrapper; unwrap and verify
            assertNotNull(e.cause)
        } catch (e: Exception) {
            // Any exception from invalid JSON is acceptable
            assertNotNull(e)
        }
    }

    // Helper
    private fun makeConfig(
        bankId: String,
        amountFormat: String = "space_comma",
        useSignForType: Boolean = false,
    ) = RegexParserProfile(
        bankId = bankId,
        bankMarkers = listOf(bankId),
        transactionPattern = "\\d+",
        dateFormat = "dd.MM.yyyy",
        operationTypeMap = emptyMap(),
        amountFormat = amountFormat,
        useSignForType = useSignForType,
    )
}
