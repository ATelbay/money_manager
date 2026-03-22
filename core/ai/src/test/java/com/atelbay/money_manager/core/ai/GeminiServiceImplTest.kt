package com.atelbay.money_manager.core.ai

import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class GeminiServiceImplTest {

    private lateinit var configProvider: ParserConfigProvider
    private lateinit var service: GeminiServiceImpl

    // Use reflection to access private methods for testing
    private lateinit var parseParserConfigResponseMethod: Method
    private lateinit var selectExamplesMethod: Method
    private lateinit var buildPromptMethod: Method

    @Before
    fun setUp() {
        configProvider = mockk()
        every { configProvider.getGeminiModelName() } returns "gemini-2.5-flash"
        service = GeminiServiceImpl(configProvider)

        // Access private methods via reflection
        parseParserConfigResponseMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "parseParserConfigResponse", String::class.java
        ).apply { isAccessible = true }

        selectExamplesMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "selectExamplesForPrompt", List::class.java
        ).apply { isAccessible = true }

        buildPromptMethod = GeminiServiceImpl::class.java.getDeclaredMethod(
            "buildParserConfigPrompt",
            String::class.java,
            String::class.java,
            List::class.java,
            List::class.java,
        ).apply { isAccessible = true }
    }

    // Helper to invoke parseParserConfigResponse via reflection
    private fun parseResponse(json: String): ParserConfig =
        parseParserConfigResponseMethod.invoke(service, json) as ParserConfig

    @Suppress("UNCHECKED_CAST")
    private fun selectExamples(configs: List<ParserConfig>): List<ParserConfig> =
        selectExamplesMethod.invoke(service, configs) as List<ParserConfig>

    private fun buildPrompt(
        header: String, samples: String,
        configs: List<ParserConfig>, attempts: List<FailedAttempt>,
    ): String = buildPromptMethod.invoke(service, header, samples, configs, attempts) as String

    // --- parseParserConfigResponse tests ---

    @Test
    fun `parseParserConfigResponse reads operation_type_map from JSON array`() {
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
    fun `parseParserConfigResponse returns empty map when operation_type_map is missing`() {
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
    fun `parseParserConfigResponse returns empty map when operation_type_map is empty array`() {
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
    fun `parseParserConfigResponse converts Python named groups to Java syntax`() {
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

    // --- buildParserConfigPrompt tests ---

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

    // Helper
    private fun makeConfig(
        bankId: String,
        amountFormat: String = "space_comma",
        useSignForType: Boolean = false,
    ) = ParserConfig(
        bankId = bankId,
        bankMarkers = listOf(bankId),
        transactionPattern = "\\d+",
        dateFormat = "dd.MM.yyyy",
        operationTypeMap = emptyMap(),
        amountFormat = amountFormat,
        useSignForType = useSignForType,
    )
}
