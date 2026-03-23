package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.ai.FailedAttempt
import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.ai.TableFailedAttempt
import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.model.TableParserConfigList
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.parser.RegexParseResult
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
    val aiGeneratedTableConfig: TableParserConfig? = null,
    val sampleTableRows: List<List<String>>? = null,
)

enum class AiMethod { NONE, REGEX_GENERATED, FULL_PARSE, TABLE_GENERATED }

private data class RegexThenGeminiResult(
    val transactions: List<ParsedTransaction>,
    val aiGeneratedConfig: ParserConfig? = null,
    val sampleRows: String? = null,
    val aiMethod: AiMethod = AiMethod.NONE,
    val aiGeneratedTableConfig: TableParserConfig? = null,
    val sampleTableRows: List<List<String>>? = null,
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

    suspend operator fun invoke(
        blobs: List<Pair<ByteArray, String>>,
        collector: ImportProgressCollector = NoOpCollector,
    ): ParseResult {
        val parseErrors = mutableListOf<String>()
        val pdfBlob = blobs.firstOrNull { it.second == "application/pdf" }

        var aiGeneratedConfig: ParserConfig? = null
        var sampleRows: String? = null
        var aiMethod = AiMethod.NONE
        var aiGeneratedTableConfig: TableParserConfig? = null
        var sampleTableRows: List<List<String>>? = null

        val transactions = if (pdfBlob != null) {
            val result = tryRegexThenGemini(pdfBlob.first, blobs, parseErrors, collector)
            aiGeneratedConfig = result.aiGeneratedConfig
            sampleRows = result.sampleRows
            aiMethod = result.aiMethod
            aiGeneratedTableConfig = result.aiGeneratedTableConfig
            sampleTableRows = result.sampleTableRows
            result.transactions
        } else if (parserConfigProvider.isAiFullParseEnabled()) {
            aiMethod = AiMethod.FULL_PARSE
            collector.emit(ImportStepEvent.FullAiParse(enabled = true))
            parseWithGemini(blobs, parseErrors)
        } else {
            collector.emit(ImportStepEvent.Error("AI parsing disabled, only PDF statements supported"))
            parseErrors.add("AI-парсинг отключён. Поддерживаются только PDF-выписки.")
            emptyList()
        }

        val importResult = deduplicateAndBuildResult(transactions, parseErrors, collector)
        collector.emit(ImportStepEvent.Complete(importResult.newTransactions.size, aiMethod.name))

        return ParseResult(
            importResult = importResult,
            aiGeneratedConfig = aiGeneratedConfig,
            sampleRows = sampleRows,
            aiMethod = aiMethod,
            aiGeneratedTableConfig = aiGeneratedTableConfig,
            sampleTableRows = sampleTableRows,
        )
    }

    private suspend fun tryRegexThenGemini(
        pdfBytes: ByteArray,
        blobs: List<Pair<ByteArray, String>>,
        parseErrors: MutableList<String>,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult {
        // Step 1: Try Remote Config + cached regex configs
        val regexResult = statementParser.tryParsePdf(pdfBytes)
        val pdfText = regexResult?.extractedText.orEmpty()
        collector.emit(ImportStepEvent.PdfExtracted(pdfText.lines().size))

        tryCachedRegexConfigs(pdfBytes, regexResult, collector)?.let { return it }

        // Step 2: Try table-based parsing (cached → AI generation)
        tryTableBasedParsing(pdfBytes, collector)?.let { return it }

        // Step 3: Try AI regex generation with retries
        tryAiRegexGeneration(pdfBytes, pdfText, collector)?.let { return it }

        // Step 4: Fall back to full-AI parsing (if enabled)
        return fallbackToFullAiParsing(regexResult, blobs, parseErrors, collector)
    }

    /** Tries Remote Config regex configs and cached AI regex configs. */
    private suspend fun tryCachedRegexConfigs(
        pdfBytes: ByteArray,
        regexResult: RegexParseResult?,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        if (regexResult != null && regexResult.transactions.isNotEmpty()) {
            Timber.d("PDF import: bank=%s parsed %d transactions via regex",
                regexResult.bankId, regexResult.transactions.size)
            collector.emit(ImportStepEvent.RegexConfigAttempt("remote_config", regexResult.bankId))
            collector.emit(ImportStepEvent.RegexConfigResult("remote_config", regexResult.transactions.size))
            return RegexThenGeminiResult(assignCategories(regexResult.transactions, collector))
        }
        collector.emit(ImportStepEvent.RegexConfigAttempt("remote_config"))
        collector.emit(ImportStepEvent.RegexConfigResult("remote_config", 0))

        val cachedConfigs = loadCachedAiConfigs()
        if (cachedConfigs.isNotEmpty()) {
            collector.emit(ImportStepEvent.RegexConfigAttempt("cached_ai"))
            val cachedResult = statementParser.tryParsePdf(pdfBytes, additionalConfigs = cachedConfigs)
            if (cachedResult != null && cachedResult.transactions.isNotEmpty()) {
                Timber.d("PDF import: bank=%s parsed %d transactions via cached AI config",
                    cachedResult.bankId, cachedResult.transactions.size)
                collector.emit(ImportStepEvent.RegexConfigResult("cached_ai", cachedResult.transactions.size))
                return RegexThenGeminiResult(assignCategories(cachedResult.transactions, collector))
            }
            collector.emit(ImportStepEvent.RegexConfigResult("cached_ai", 0))
        }
        return null
    }

    /** Tries cached table configs, then AI-generated table configs with retries. */
    private suspend fun tryTableBasedParsing(
        pdfBytes: ByteArray,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        val sampleTableRows = try {
            statementParser.extractSampleTableRows(pdfBytes)
        } catch (e: Exception) {
            Timber.w(e, "Table extraction failed, falling through to AI regex")
            emptyList()
        }
        if (sampleTableRows.size < 2) return null

        collector.emit(ImportStepEvent.TableExtracted(
            rowCount = sampleTableRows.size,
            columnCount = sampleTableRows.firstOrNull()?.size ?: 0,
        ))

        // Try cached table configs first
        val cachedTableConfigs = loadCachedTableConfigs()
        if (cachedTableConfigs.isNotEmpty()) {
            collector.emit(ImportStepEvent.TableConfigAttempt("cached_table"))
            val cachedTableResult = try {
                statementParser.tryParseTable(pdfBytes, cachedTableConfigs)
            } catch (e: Exception) {
                Timber.w(e, "Cached table config parsing failed")
                null
            }
            if (cachedTableResult != null && cachedTableResult.transactions.isNotEmpty()) {
                Timber.d("Table import: parsed %d transactions via cached table config (bank=%s)",
                    cachedTableResult.transactions.size, cachedTableResult.bankId)
                collector.emit(ImportStepEvent.TableConfigResult("cached_table", cachedTableResult.transactions.size))
                return RegexThenGeminiResult(
                    transactions = assignCategories(cachedTableResult.transactions, collector),
                    aiMethod = AiMethod.TABLE_GENERATED,
                )
            }
            collector.emit(ImportStepEvent.TableConfigResult("cached_table", 0))
        }

        // AI table config generation with retries
        val tableFailedAttempts = mutableListOf<TableFailedAttempt>()
        repeat(MAX_AI_RETRIES) { attempt ->
            val attemptNum = attempt + 1
            collector.emit(ImportStepEvent.AiTableConfigRequest(attemptNum))

            val generatedTableConfig = try {
                geminiService.generateTableParserConfig(
                    sampleTableRows = sampleTableRows,
                    previousAttempts = tableFailedAttempts,
                )
            } catch (e: Exception) {
                Timber.w(e, "AI table config generation failed (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                tableFailedAttempts.add(
                    TableFailedAttempt(
                        config = TableParserConfig(
                            bankId = "", bankMarkers = emptyList(),
                            dateColumn = 0, amountColumn = 1, dateFormat = "",
                        ),
                        error = "AI generation failed: ${e.message}",
                    ),
                )
                return@repeat
            }
            Timber.d("AI generated table config for bank: %s (attempt %d/%d)", generatedTableConfig.bankId, attemptNum, MAX_AI_RETRIES)
            collector.emit(ImportStepEvent.AiTableConfigResponse(attemptNum, generatedTableConfig.bankId))

            if (!isDateFormatValid(generatedTableConfig.dateFormat)) {
                val dateError = try {
                    java.time.format.DateTimeFormatter.ofPattern(generatedTableConfig.dateFormat); ""
                } catch (e: Exception) { e.message.orEmpty() }
                Timber.w("AI-generated table dateFormat is invalid (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                tableFailedAttempts.add(TableFailedAttempt(generatedTableConfig, "DateFormat invalid: $dateError"))
                return@repeat
            }

            val tableResult = try {
                withTimeout(AI_REGEX_TIMEOUT_MS) {
                    runInterruptible { statementParser.tryParseWithTableConfig(pdfBytes, generatedTableConfig) }
                }
            } catch (_: TimeoutCancellationException) {
                Timber.w("AI-generated table config timed out (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                tableFailedAttempts.add(TableFailedAttempt(generatedTableConfig, "Table parsing timed out"))
                return@repeat
            }

            collector.emit(ImportStepEvent.AiTableConfigParseResult(attemptNum, tableResult.transactions.size))
            if (tableResult.transactions.isEmpty()) {
                Timber.d("AI-generated table config parsed 0 transactions (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                val failedRows = tableResult.extractedTable
                    .drop(generatedTableConfig.skipHeaderRows).take(5)
                    .map { it.joinToString(" | ") }
                tableFailedAttempts.add(
                    TableFailedAttempt(generatedTableConfig, "Table config parsed 0 transactions", failedRows),
                )
                return@repeat
            }

            Timber.d("AI-generated table config parsed %d transactions (attempt %d/%d)", tableResult.transactions.size, attemptNum, MAX_AI_RETRIES)
            return RegexThenGeminiResult(
                transactions = assignCategories(tableResult.transactions, collector),
                aiMethod = AiMethod.TABLE_GENERATED,
                aiGeneratedTableConfig = generatedTableConfig,
                sampleTableRows = sampleTableRows,
            )
        }

        if (tableFailedAttempts.isNotEmpty()) {
            Timber.w("All %d AI table config attempts failed, falling through to AI regex", tableFailedAttempts.size)
        }
        return null
    }

    /** Tries AI-generated regex config with retries. */
    private suspend fun tryAiRegexGeneration(
        pdfBytes: ByteArray,
        pdfText: String,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        val headerSnippet = statementParser.extractHeaderSnippet(pdfText)
        val extractedSampleRows = statementParser.extractSampleRows(pdfText)
        if (extractedSampleRows.isEmpty()) return null

        val allConfigs = parserConfigProvider.getConfigs() + loadCachedAiConfigs()
        val failedAttempts = mutableListOf<FailedAttempt>()

        repeat(MAX_AI_RETRIES) { attempt ->
            val attemptNum = attempt + 1
            collector.emit(ImportStepEvent.AiConfigRequest(attemptNum))

            val generatedConfig = try {
                geminiService.generateParserConfig(
                    headerSnippet = headerSnippet,
                    sampleRows = extractedSampleRows,
                    existingConfigs = allConfigs,
                    previousAttempts = failedAttempts,
                )
            } catch (e: Exception) {
                Timber.w(e, "AI config generation failed (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                collector.emit(ImportStepEvent.Error("AI generation failed (attempt $attemptNum): ${e.message}"))
                failedAttempts.add(
                    FailedAttempt(
                        config = ParserConfig(
                            bankId = "", bankMarkers = emptyList(),
                            transactionPattern = "", dateFormat = "",
                            operationTypeMap = emptyMap(),
                        ),
                        error = "AI generation failed: ${e.message}",
                    ),
                )
                return@repeat
            }
            Timber.d("AI generated config for bank: %s (attempt %d/%d)", generatedConfig.bankId, attemptNum, MAX_AI_RETRIES)
            collector.emit(ImportStepEvent.AiConfigResponse(attemptNum, generatedConfig.bankId))

            // Validate: ReDoS + regex syntax + dateFormat
            val redosViolation = regexValidator.getReDoSViolation(generatedConfig.transactionPattern)
            if (redosViolation != null) {
                Timber.w("AI-generated regex failed ReDoS check (attempt %d/%d): %s", attemptNum, MAX_AI_RETRIES, redosViolation)
                collector.emit(ImportStepEvent.ValidationResult(attemptNum, "ReDoS", false, redosViolation))
                failedAttempts.add(FailedAttempt(generatedConfig, "Regex failed ReDoS safety check: $redosViolation"))
                return@repeat
            }
            collector.emit(ImportStepEvent.ValidationResult(attemptNum, "ReDoS", true))

            if (!isRegexValid(generatedConfig.transactionPattern)) {
                val syntaxError = try { Regex(generatedConfig.transactionPattern); "" }
                    catch (e: Exception) { e.message.orEmpty() }
                Timber.w("AI-generated regex has invalid syntax (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Regex syntax", false, syntaxError))
                failedAttempts.add(FailedAttempt(generatedConfig, "Regex syntax invalid: $syntaxError"))
                return@repeat
            }
            collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Regex syntax", true))

            if (!isDateFormatValid(generatedConfig.dateFormat)) {
                val dateError = try { java.time.format.DateTimeFormatter.ofPattern(generatedConfig.dateFormat); "" }
                    catch (e: Exception) { e.message.orEmpty() }
                Timber.w("AI-generated dateFormat is invalid (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Date format", false, dateError))
                failedAttempts.add(FailedAttempt(generatedConfig, "DateFormat invalid: $dateError"))
                return@repeat
            }
            collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Date format", true))

            // Parse with generated config (with timeout)
            val aiResult = try {
                withTimeout(AI_REGEX_TIMEOUT_MS) {
                    runInterruptible { statementParser.tryParseWithConfig(pdfBytes, generatedConfig) }
                }
            } catch (_: TimeoutCancellationException) {
                Timber.w("AI-generated regex timed out (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Regex timeout", false, "Possible catastrophic backtracking"))
                failedAttempts.add(FailedAttempt(generatedConfig, "Regex timed out (possible catastrophic backtracking)"))
                return@repeat
            }

            if (aiResult == null || aiResult.transactions.isEmpty()) {
                Timber.d("AI-generated config parsed 0 transactions (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
                val sampleLines = extractSampleLinesForDiagnostics(pdfText)
                val totalLines = pdfText.lines().drop(HEADER_SKIP_LINES).count { it.isNotBlank() }
                collector.emit(ImportStepEvent.AiConfigParseResult(attemptNum, 0))
                failedAttempts.add(FailedAttempt(
                    generatedConfig,
                    "Regex matched 0 transaction lines out of $totalLines total lines. Sample lines that should have matched:\n$sampleLines",
                ))
                return@repeat
            }

            Timber.d("AI-generated config parsed %d transactions (attempt %d/%d)", aiResult.transactions.size, attemptNum, MAX_AI_RETRIES)
            collector.emit(ImportStepEvent.AiConfigParseResult(attemptNum, aiResult.transactions.size))
            return RegexThenGeminiResult(
                transactions = assignCategories(aiResult.transactions, collector),
                aiGeneratedConfig = generatedConfig,
                sampleRows = extractedSampleRows,
                aiMethod = AiMethod.REGEX_GENERATED,
            )
        }

        if (failedAttempts.isNotEmpty()) {
            Timber.w("All %d AI config generation attempts failed, falling back to full AI", failedAttempts.size)
        }
        return null
    }

    /** Falls back to full-AI parsing when all structured approaches fail. */
    private suspend fun fallbackToFullAiParsing(
        regexResult: RegexParseResult?,
        blobs: List<Pair<ByteArray, String>>,
        parseErrors: MutableList<String>,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult {
        if (!parserConfigProvider.isAiFullParseEnabled()) {
            Timber.d("PDF import: AI full parse disabled, returning empty")
            collector.emit(ImportStepEvent.FullAiParse(enabled = false))
            parseErrors.add("Не удалось распознать формат выписки. AI-парсинг отключён.")
            return RegexThenGeminiResult(transactions = emptyList())
        }

        collector.emit(ImportStepEvent.FullAiParse(enabled = true))
        if (regexResult?.bankId == null) {
            Timber.d("PDF import: bank not detected, using AI fallback")
        } else {
            Timber.d("PDF import: bank=%s detected but 0 transactions parsed, using AI fallback",
                regexResult.bankId)
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

    private fun extractSampleLinesForDiagnostics(pdfText: String): String =
        pdfText.lines()
            .drop(HEADER_SKIP_LINES)
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString("\n")

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

    private suspend fun loadCachedTableConfigs(): List<TableParserConfig> {
        val cachedJson = userPreferences.cachedAiTableParserConfigs.firstOrNull()
        if (cachedJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<TableParserConfigList>(cachedJson).configs
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse cached table configs")
            emptyList()
        }
    }

    suspend fun cacheTableConfig(config: TableParserConfig) {
        try {
            val existing = loadCachedTableConfigs().toMutableList()
            existing.removeAll { it.bankId == config.bankId }
            existing.add(config)
            val configList = TableParserConfigList(configs = existing)
            val jsonStr = json.encodeToString(TableParserConfigList.serializer(), configList)
            userPreferences.setCachedAiTableParserConfigs(jsonStr)
            Timber.d("Cached table config for bank %s", config.bankId)
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache table config")
        }
    }

    suspend fun cacheAiConfig(config: ParserConfig) {
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
        val expenseCategories = categoryDao.getByType(TYPE_EXPENSE)
        val incomeCategories = categoryDao.getByType(TYPE_INCOME)

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
        collector: ImportProgressCollector = NoOpCollector,
    ): List<ParsedTransaction> {
        val expenseCategories = categoryDao.getByType(TYPE_EXPENSE).toMutableList()
        val incomeCategories = categoryDao.getByType(TYPE_INCOME).toMutableList()

        val neededOperations = transactions
            .filter { it.categoryId == null }
            .map { it.operationType to it.type }
            .distinct()

        for ((operation, type) in neededOperations) {
            val categories = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
            val resolvedName = operationAliases[operation] ?: operation
            if (categories.any { it.name == resolvedName }) continue

            val dbType = if (type == TransactionType.EXPENSE) TYPE_EXPENSE else TYPE_INCOME
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

        val result = transactions.map { tx ->
            if (tx.categoryId != null) return@map tx

            val categories = if (tx.type == TransactionType.EXPENSE) expenseCategories else incomeCategories
            val resolvedName = operationAliases[tx.operationType] ?: tx.operationType
            val matched = categories.find { it.name == resolvedName }

            tx.copy(
                categoryId = matched?.id,
                suggestedCategoryName = tx.operationType,
            )
        }
        collector.emit(ImportStepEvent.CategoryAssignment(result.size))
        return result
    }

    companion object {
        private const val DEFAULT_IMPORT_CATEGORY_COLOR = 0xFFB0BEC5
        private const val AI_REGEX_TIMEOUT_MS = 5_000L
        private const val MAX_AI_RETRIES = 3
        private const val HEADER_SKIP_LINES = 10
        private const val TYPE_EXPENSE = "expense"
        private const val TYPE_INCOME = "income"
    }

    private suspend fun deduplicateAndBuildResult(
        transactions: List<ParsedTransaction>,
        parseErrors: List<String> = emptyList(),
        collector: ImportProgressCollector = NoOpCollector,
    ): ImportResult {
        val hashes = transactions.map { it.uniqueHash }
        val existingHashes = if (hashes.isNotEmpty()) {
            transactionDao.getExistingHashes(hashes)
        } else {
            emptyList()
        }.toSet()

        val newTransactions = transactions.filter { it.uniqueHash !in existingHashes }
        val duplicates = transactions.size - newTransactions.size
        collector.emit(ImportStepEvent.Deduplication(transactions.size, newTransactions.size))

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
