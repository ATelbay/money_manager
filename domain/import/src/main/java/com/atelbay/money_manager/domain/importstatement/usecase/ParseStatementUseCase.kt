package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.StatementParser
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigList
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import com.atelbay.money_manager.domain.categories.usecase.SaveCategoryUseCase
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@Serializable
private data class GeminiResponse(
    @SerialName("tx") val transactions: List<GeminiTransaction>,
)

@Serializable
private data class GeminiTransaction(
    @SerialName("d") val date: String,
    @SerialName("a") val amount: Double,
    @SerialName("t") val type: String,
    @SerialName("det") val details: String,
    @SerialName("cid") val categoryId: Long? = null,
    @SerialName("cn") val suggestedCategoryName: String? = null,
    @SerialName("conf") val confidence: Float,
)

data class ParseResult(
    val importResult: ImportResult,
    val aiGeneratedConfig: ParserConfig? = null,
    val sampleRows: String? = null,
    val aiMethod: AiMethod = AiMethod.NONE,
)

enum class AiMethod { NONE, REGEX_GENERATED, FULL_PARSE }

private data class RegexThenGeminiResult(
    val transactions: List<ParsedTransaction>,
    val aiGeneratedConfig: ParserConfig? = null,
    val sampleRows: String? = null,
    val aiMethod: AiMethod = AiMethod.NONE,
)

class ParseStatementUseCase @Inject constructor(
    private val statementParser: StatementParser,
    private val geminiService: GeminiService,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val userPreferences: UserPreferences,
    private val regexValidator: RegexValidator,
    private val parserConfigProvider: ParserConfigProvider,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(blobs: List<Pair<ByteArray, String>>): ParseResult {
        val parseErrors = mutableListOf<String>()
        val pdfBlob = blobs.firstOrNull { it.second == "application/pdf" }

        var aiGeneratedConfig: ParserConfig? = null
        var sampleRows: String? = null
        var aiMethod = AiMethod.NONE

        val transactions = if (pdfBlob != null) {
            val result = tryRegexThenGemini(pdfBlob.first, blobs, parseErrors)
            aiGeneratedConfig = result.aiGeneratedConfig
            sampleRows = result.sampleRows
            aiMethod = result.aiMethod
            result.transactions
        } else if (parserConfigProvider.isAiFullParseEnabled()) {
            aiMethod = AiMethod.FULL_PARSE
            parseWithGemini(blobs, parseErrors)
        } else {
            parseErrors.add("AI-парсинг отключён. Поддерживаются только PDF-выписки.")
            emptyList()
        }

        return ParseResult(
            importResult = deduplicateAndBuildResult(transactions, parseErrors),
            aiGeneratedConfig = aiGeneratedConfig,
            sampleRows = sampleRows,
            aiMethod = aiMethod,
        )
    }

    private suspend fun tryRegexThenGemini(
        pdfBytes: ByteArray,
        blobs: List<Pair<ByteArray, String>>,
        parseErrors: MutableList<String>,
    ): RegexThenGeminiResult {
        // Step 1: Try Remote Config regex (existing behavior)
        val regexResult = statementParser.tryParsePdf(pdfBytes)
        if (regexResult != null && regexResult.transactions.isNotEmpty()) {
            Timber.d(
                "PDF import: bank=%s parsed %d transactions via regex",
                regexResult.bankId,
                regexResult.transactions.size,
            )
            return RegexThenGeminiResult(assignCategories(regexResult.transactions))
        }

        // Reuse extracted text from step 1 to avoid double PDF extraction
        val pdfText = regexResult?.extractedText.orEmpty()

        // Step 2: Try cached AI configs from DataStore
        val cachedConfigs = loadCachedAiConfigs()
        if (cachedConfigs.isNotEmpty()) {
            val cachedResult = statementParser.tryParsePdf(pdfBytes, additionalConfigs = cachedConfigs)
            if (cachedResult != null && cachedResult.transactions.isNotEmpty()) {
                Timber.d(
                    "PDF import: bank=%s parsed %d transactions via cached AI config",
                    cachedResult.bankId,
                    cachedResult.transactions.size,
                )
                return RegexThenGeminiResult(assignCategories(cachedResult.transactions))
            }
        }

        // Step 3: Extract sample rows + call Gemini to generate ParserConfig
        val headerSnippet = statementParser.extractHeaderSnippet(pdfText)
        val extractedSampleRows = statementParser.extractSampleRows(pdfText)
        if (extractedSampleRows.isNotEmpty()) {
            try {
                val generatedConfig = geminiService.generateParserConfig(
                    headerSnippet = headerSnippet,
                    sampleRows = extractedSampleRows,
                )
                Timber.d("AI generated config for bank: %s", generatedConfig.bankId)

                // Step 4: Validate — ReDoS check + regex syntax + dateFormat
                if (!regexValidator.isReDoSSafe(generatedConfig.transactionPattern)) {
                    Timber.w("AI-generated regex failed ReDoS check, falling back to full AI")
                } else if (!isRegexValid(generatedConfig.transactionPattern)) {
                    Timber.w("AI-generated regex has invalid syntax, falling back to full AI")
                } else if (!isDateFormatValid(generatedConfig.dateFormat)) {
                    Timber.w("AI-generated dateFormat is invalid, falling back to full AI")
                } else {
                    // Step 5: Parse with generated config (with timeout + runInterruptible guard)
                    val aiResult = try {
                        withTimeout(AI_REGEX_TIMEOUT_MS) {
                            runInterruptible {
                                statementParser.tryParseWithConfig(pdfBytes, generatedConfig)
                            }
                        }
                    } catch (_: TimeoutCancellationException) {
                        Timber.w("AI-generated regex timed out (possible ReDoS), falling back to full AI")
                        null
                    }

                    if (aiResult != null && aiResult.transactions.isNotEmpty()) {
                        Timber.d("AI-generated config parsed %d transactions", aiResult.transactions.size)
                        // Step 6: Cache config in DataStore
                        cacheAiConfig(generatedConfig)
                        return RegexThenGeminiResult(
                            transactions = assignCategories(aiResult.transactions),
                            aiGeneratedConfig = generatedConfig,
                            sampleRows = extractedSampleRows,
                            aiMethod = AiMethod.REGEX_GENERATED,
                        )
                    } else {
                        Timber.d("AI-generated config parsed 0 transactions, falling back to full AI")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "AI config generation failed, falling back to full AI")
            }
        }

        // Step 7: Fall back to existing full-AI parsing (if enabled)
        if (!parserConfigProvider.isAiFullParseEnabled()) {
            Timber.d("PDF import: AI full parse disabled, returning empty")
            parseErrors.add("Не удалось распознать формат выписки. AI-парсинг отключён.")
            return RegexThenGeminiResult(transactions = emptyList())
        }

        if (regexResult?.bankId == null) {
            Timber.d("PDF import: bank not detected, using AI fallback")
        } else {
            Timber.d(
                "PDF import: bank=%s detected but 0 transactions parsed, using AI fallback",
                regexResult.bankId,
            )
        }
        return RegexThenGeminiResult(
            transactions = parseWithGemini(blobs, parseErrors),
            aiMethod = AiMethod.FULL_PARSE,
        )
    }

    private fun isRegexValid(pattern: String): Boolean {
        return try {
            Regex(pattern)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isDateFormatValid(dateFormat: String): Boolean {
        return try {
            java.time.format.DateTimeFormatter.ofPattern(dateFormat)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun loadCachedAiConfigs(): List<ParserConfig> {
        val cachedJson = userPreferences.cachedAiParserConfigs.firstOrNull()
        if (cachedJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<ParserConfigList>(cachedJson).banks
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse cached AI configs")
            emptyList()
        }
    }

    private suspend fun cacheAiConfig(config: ParserConfig) {
        try {
            val existing = loadCachedAiConfigs().toMutableList()
            // Replace only the exact same variant; keep other configs for the same bank.
            existing.removeAll {
                it.bankId == config.bankId &&
                    it.transactionPattern == config.transactionPattern
            }
            existing.add(config)
            val configList = ParserConfigList(banks = existing)
            val jsonStr = json.encodeToString(ParserConfigList.serializer(), configList)
            userPreferences.setCachedAiParserConfigs(jsonStr)
            Timber.d("Cached AI config for bank %s", config.bankId)
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache AI config")
        }
    }

    private suspend fun parseWithGemini(
        blobs: List<Pair<ByteArray, String>>,
        parseErrors: MutableList<String>,
    ): List<ParsedTransaction> {
        val expenseCategories = categoryDao.getByType("expense")
        val incomeCategories = categoryDao.getByType("income")

        val prompt = buildPrompt(expenseCategories, incomeCategories)
        val responseText = geminiService.parseContent(blobs, prompt)

        val jsonString = extractJson(responseText)
        val parsed = try {
            json.decodeFromString<GeminiResponse>(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Gemini response")
            parseErrors.add("Gemini не смог распознать документ: ${e.message}")
            return emptyList()
        }

        val errors = mutableListOf<String>()
        val result = parsed.transactions.mapNotNull { tx ->
            try {
                val date = LocalDate.parse(tx.date)
                val type = when (tx.type) {
                    "i" -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                val hash = generateTransactionHash(date, tx.amount, type.value, tx.details)
                ParsedTransaction(
                    date = date,
                    amount = tx.amount,
                    type = type,
                    details = tx.details,
                    categoryId = tx.categoryId,
                    suggestedCategoryName = tx.suggestedCategoryName,
                    confidence = tx.confidence,
                    needsReview = tx.confidence < 0.7f,
                    uniqueHash = hash,
                )
            } catch (e: Exception) {
                errors.add("Ошибка парсинга транзакции '${tx.details}': ${e.message}")
                null
            }
        }
        parseErrors.addAll(errors)
        return result
    }

    // Maps raw operation names from bank statements to existing default category names.
    // Handles both Russian and English variants across all supported banks.
    // IMPORTANT: target names must exactly match DefaultCategories (e.g. "Перевод", not "Переводы").
    // Operations NOT listed here fall back to their raw name; if it matches a default category
    // exactly, it is reused — otherwise a new custom category is created on import.
    private val operationAliases = mapOf(
        // Generic — covers Gemini-parsed results and future bank support
        "Покупка" to "Покупки",
        "Purchase" to "Покупки",
        "Оплата" to "Покупки",
        "Payment" to "Покупки",
        // Forte Bank
        "Покупка бонусами" to "Покупки",
        "Пополнение счета" to "Пополнение",
        "Платеж" to "Перевод",
        "Платёж" to "Перевод",
        "Списание средств в рамках сервиса быстрых платежей" to "Перевод",
        "Снятие" to "Другое",
        "Комиссия" to "Другое",
        // Bereke Bank (English) — full names
        "Payment for goods and services" to "Покупки",
        "Card replenishment through Bereke Bank" to "Пополнение",
        "Card replenishment through payment terminal" to "Пополнение",
        "Transfer from a card through Bereke Bank" to "Перевод",
        "Operation" to "Перевод",
        // Bereke Bank (English) — truncated names captured when PDF line wraps (services/Bank/terminal on continuation)
        "Payment for goods and" to "Покупки",
        "Card replenishment through" to "Пополнение",
        "Transfer from a card" to "Перевод",
        // Eurasian Bank — map to nearest default categories.
        // "Транспорт", "Связь", "Развлечения" match DefaultCategories exactly → no alias needed.
        "Продукты" to "Еда",
        "Кафе и рестораны" to "Еда",
        "Здоровье и красота" to "Здоровье",
        "Государственные услуги" to "Другое",
        "Услуги" to "Другое",
        "Путешествия" to "Другое",
        "Финансы" to "Пополнение",
    )

    private suspend fun assignCategories(
        transactions: List<ParsedTransaction>,
    ): List<ParsedTransaction> {
        val expenseCategories = categoryDao.getByType("expense").toMutableList()
        val incomeCategories = categoryDao.getByType("income").toMutableList()

        val neededOperations = transactions
            .filter { it.categoryId == null }
            .map { it.operationType to it.type }
            .distinct()

        for ((operation, type) in neededOperations) {
            val categories = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
            val resolvedName = operationAliases[operation] ?: operation
            if (categories.any { it.name == resolvedName }) continue

            val dbType = if (type == TransactionType.EXPENSE) "expense" else "income"
            val newCategory = CategoryEntity(
                name = resolvedName,
                icon = "label",
                color = DEFAULT_IMPORT_CATEGORY_COLOR,
                type = dbType,
                isDefault = false,
            )
            val domainCategory = Category(
                name = resolvedName,
                icon = "label",
                color = DEFAULT_IMPORT_CATEGORY_COLOR,
                type = type,
                isDefault = false,
            )
            val id = saveCategoryUseCase(domainCategory)
            val created = newCategory.copy(id = id)
            categories.add(created)
            Timber.d("Created category '%s' (%s) with id=%d", resolvedName, dbType, id)
        }

        return transactions.map { tx ->
            if (tx.categoryId != null) return@map tx

            val categories = if (tx.type == TransactionType.EXPENSE) expenseCategories else incomeCategories
            val resolvedName = operationAliases[tx.operationType] ?: tx.operationType
            val matched = categories.find { it.name == resolvedName }

            tx.copy(
                categoryId = matched?.id,
                suggestedCategoryName = tx.operationType,
            )
        }
    }

    companion object {
        private const val DEFAULT_IMPORT_CATEGORY_COLOR = 0xFFB0BEC5
        private const val AI_REGEX_TIMEOUT_MS = 5_000L
    }

    private suspend fun deduplicateAndBuildResult(
        transactions: List<ParsedTransaction>,
        parseErrors: List<String> = emptyList(),
    ): ImportResult {
        val hashes = transactions.map { it.uniqueHash }
        val existingHashes = if (hashes.isNotEmpty()) {
            transactionDao.getExistingHashes(hashes)
        } else {
            emptyList()
        }.toSet()

        val newTransactions = transactions.filter { it.uniqueHash !in existingHashes }
        val duplicates = transactions.size - newTransactions.size

        return ImportResult(
            total = transactions.size,
            newTransactions = newTransactions,
            duplicates = duplicates,
            errors = parseErrors,
        )
    }

    private fun buildPrompt(
        expenseCategories: List<CategoryEntity>,
        incomeCategories: List<CategoryEntity>,
    ): String = buildString {
        appendLine("Распарси банковскую выписку. Верни JSON.")
        appendLine("Расход: ${expenseCategories.joinToString(", ") { "${it.id}:${it.name}" }}")
        appendLine("Доход: ${incomeCategories.joinToString(", ") { "${it.id}:${it.name}" }}")
        append(
            """
            Формат: {"tx":[{"d":"YYYY-MM-DD","a":сумма,"t":"e"|"i","det":"описание","cid":id|null,"cn":"подсказка"|null,"conf":0.0-1.0}]}
            a>0. t: e=расход, i=доход. cid из списка или null+cn. conf>0.8 если уверен. Переводы физлицам→"Переводы".
            """.trimIndent(),
        )
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            text.substring(start, end + 1)
        } else {
            text
        }
    }
}
