package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.ai.CategoryNames
import com.atelbay.money_manager.core.ai.FailedAttempt
import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.ai.TableFailedAttempt
import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.ParserConfigDao
import com.atelbay.money_manager.core.database.entity.ParserConfigEntity
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.firestore.datasource.FirestoreDataSource
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.parser.RegexParseResult
import com.atelbay.money_manager.core.parser.RegexValidator
import com.atelbay.money_manager.core.parser.StatementParser
import com.atelbay.money_manager.core.parser.TableExtractionResult
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import com.atelbay.money_manager.core.remoteconfig.ParserConfigProvider
import com.atelbay.money_manager.domain.categories.usecase.SaveCategoryUseCase
import kotlinx.coroutines.TimeoutCancellationException
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
    val categoryMap: Map<String, String> = emptyMap(),
)

class ParseStatementUseCase @Inject constructor(
    private val statementParser: StatementParser,
    private val geminiService: GeminiService,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val regexValidator: RegexValidator,
    private val parserConfigProvider: ParserConfigProvider,
    private val userIdHasher: UserIdHasher,
    private val firestoreDataSource: FirestoreDataSource,
    private val parserConfigDao: ParserConfigDao,
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
        val regexResult = statementParser.tryParsePdf(pdfBytes)
        val pdfText = regexResult?.extractedText.orEmpty()
        collector.emit(ImportStepEvent.PdfExtracted(pdfText.lines().size))

        // Step 1: Local regex (Remote Config + DataStore cached AI regex)
        tryCachedRegexConfigs(pdfBytes, regexResult, collector)?.let { return it }

        // Step 2: Firestore regex (user's own candidates)
        tryFirestoreRegexConfigs(pdfBytes, collector)?.let { return it }

        // Step 3: Local table (DataStore cached AI table configs)
        tryCachedTableConfigs(pdfBytes, collector)?.let { return it }

        // Step 4: Firestore table (user's own candidates)
        tryFirestoreTableConfigs(pdfBytes, collector)?.let { return it }

        // Classify statement type before AI generation
        val classification = try {
            geminiService.classifyStatement(pdfBytes)
        } catch (e: Exception) {
            Timber.w(e, "Statement classification failed, defaulting to interleaved")
            null
        }
        val preferTableFirst = classification?.statementType == "table"
        val expectedCount = classification?.expectedTransactionCount ?: 0
        Timber.d("Classification: type=%s, expectedCount=%d", classification?.statementType, expectedCount)
        collector.emit(ImportStepEvent.Classification(classification?.statementType, expectedCount))

        // Steps 5-6 merged: AI generation with interleaved retry
        tryAiGeneration(pdfBytes, pdfText, preferTableFirst, expectedCount, collector)?.let { return it }

        // Step 7: Full AI fallback
        return fallbackToFullAiParsing(regexResult, blobs, parseErrors, collector)
    }

    /** Tries Room-backed regex configs (seed + promoted + AI cached). */
    private suspend fun tryCachedRegexConfigs(
        pdfBytes: ByteArray,
        regexResult: RegexParseResult?,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        if (regexResult != null && regexResult.transactions.isNotEmpty()) {
            Timber.d("PDF import: bank=%s parsed %d transactions via regex",
                regexResult.bankId, regexResult.transactions.size)
            collector.emit(ImportStepEvent.RegexConfigAttempt("local_db", regexResult.bankId))
            collector.emit(ImportStepEvent.RegexConfigResult("local_db", regexResult.transactions.size))
            return RegexThenGeminiResult(assignCategories(regexResult.transactions, collector = collector))
        }
        collector.emit(ImportStepEvent.RegexConfigAttempt("local_db"))
        collector.emit(ImportStepEvent.RegexConfigResult("local_db", 0))
        return null
    }

    /** Tries Room-backed table configs (seed + promoted + AI cached). */
    private suspend fun tryCachedTableConfigs(
        pdfBytes: ByteArray,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        val tableConfigs = parserConfigProvider.getTableConfigs()
        if (tableConfigs.isEmpty()) return null

        collector.emit(ImportStepEvent.TableConfigAttempt("local_db"))
        val tableResult = try {
            statementParser.tryParseTable(pdfBytes, tableConfigs)
        } catch (e: Exception) {
            Timber.w(e, "Table config parsing failed")
            null
        }
        if (tableResult != null && tableResult.transactions.isNotEmpty()) {
            Timber.d("Table import: parsed %d transactions via local table config (bank=%s)",
                tableResult.transactions.size, tableResult.bankId)
            collector.emit(ImportStepEvent.TableConfigResult("local_db", tableResult.transactions.size))
            return RegexThenGeminiResult(
                transactions = assignCategories(tableResult.transactions, collector = collector),
                aiMethod = AiMethod.TABLE_GENERATED,
            )
        }
        collector.emit(ImportStepEvent.TableConfigResult("local_db", 0))
        return null
    }

    /** Tries user's own regex candidates from Firestore. */
    private suspend fun tryFirestoreRegexConfigs(
        pdfBytes: ByteArray,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        val firestoreConfigs = try {
            val hash = userIdHasher.computeHash(null)
            val candidates = firestoreDataSource.findCandidatesByUser(hash, "regex")
            candidates.mapNotNull { dto ->
                try {
                    json.decodeFromString<ParserConfig>(dto.parserConfigJson)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to deserialize Firestore regex candidate")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Firestore regex candidates fetch failed, falling through")
            return null
        }
        if (firestoreConfigs.isEmpty()) return null

        collector.emit(ImportStepEvent.RegexConfigAttempt("firestore"))
        val firestoreResult = statementParser.tryParsePdf(pdfBytes, additionalConfigs = firestoreConfigs)
        if (firestoreResult != null && firestoreResult.transactions.isNotEmpty()) {
            Timber.d("PDF import: parsed %d transactions via Firestore regex candidate (bank=%s)",
                firestoreResult.transactions.size, firestoreResult.bankId)
            collector.emit(ImportStepEvent.RegexConfigResult("firestore", firestoreResult.transactions.size))
            return RegexThenGeminiResult(assignCategories(firestoreResult.transactions, collector = collector))
        }
        collector.emit(ImportStepEvent.RegexConfigResult("firestore", 0))
        return null
    }

    /** Tries user's own table candidates from Firestore. */
    private suspend fun tryFirestoreTableConfigs(
        pdfBytes: ByteArray,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        val firestoreTableConfigs = try {
            val hash = userIdHasher.computeHash(null)
            val candidates = firestoreDataSource.findCandidatesByUser(hash, "table")
            candidates.mapNotNull { dto ->
                try {
                    json.decodeFromString<TableParserConfig>(dto.parserConfigJson)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to deserialize Firestore table candidate")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Firestore table candidates fetch failed, falling through")
            return null
        }
        if (firestoreTableConfigs.isEmpty()) return null

        collector.emit(ImportStepEvent.TableConfigAttempt("firestore"))
        val firestoreTableResult = try {
            statementParser.tryParseTable(pdfBytes, firestoreTableConfigs)
        } catch (e: Exception) {
            Timber.w(e, "Firestore table config parsing failed")
            null
        }
        if (firestoreTableResult != null && firestoreTableResult.transactions.isNotEmpty()) {
            Timber.d("Table import: parsed %d transactions via Firestore table candidate (bank=%s)",
                firestoreTableResult.transactions.size, firestoreTableResult.bankId)
            collector.emit(ImportStepEvent.TableConfigResult("firestore", firestoreTableResult.transactions.size))
            return RegexThenGeminiResult(
                transactions = assignCategories(firestoreTableResult.transactions, collector = collector),
                aiMethod = AiMethod.TABLE_GENERATED,
            )
        }
        collector.emit(ImportStepEvent.TableConfigResult("firestore", 0))
        return null
    }

    /** AI generation with interleaved retry — alternates regex and table attempts based on classification. */
    private suspend fun tryAiGeneration(
        pdfBytes: ByteArray,
        pdfText: String,
        preferTableFirst: Boolean,
        expectedCount: Int,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        // Load category names for Gemini's category_map
        val categoryNames = CategoryNames(
            expense = categoryDao.getByType(TYPE_EXPENSE).map { it.name },
            income = categoryDao.getByType(TYPE_INCOME).map { it.name },
        )

        // Prepare regex context
        val headerSnippet = statementParser.extractHeaderSnippet(pdfText)
        val extractedSampleRows = statementParser.extractSampleRows(pdfText)
        val allConfigs = parserConfigProvider.getConfigs()
        val regexFailedAttempts = mutableListOf<FailedAttempt>()

        // Prepare table context
        val tableExtraction = try {
            statementParser.extractSampleTableRowsWithContext(pdfBytes)
        } catch (e: Exception) {
            Timber.w(e, "Table extraction failed, table attempts will be skipped")
            null
        }
        val sampleTableRows = tableExtraction?.sampleRows
        val tableFailedAttempts = mutableListOf<TableFailedAttempt>()

        val canTryRegex = extractedSampleRows.isNotEmpty()

        repeat(MAX_AI_RETRIES) { attempt ->
            val attemptNum = attempt + 1

            if (preferTableFirst) {
                // Table first, then regex
                if (sampleTableRows != null && sampleTableRows.size >= 2) {
                    tryOneTableAttempt(
                        pdfBytes, sampleTableRows, tableExtraction, attemptNum,
                        expectedCount, tableFailedAttempts, categoryNames, collector,
                    )?.let { return it }
                }
                if (canTryRegex) {
                    tryOneRegexAttempt(
                        pdfBytes, pdfText, headerSnippet, extractedSampleRows, allConfigs,
                        attemptNum, expectedCount, regexFailedAttempts, categoryNames, collector,
                    )?.let { return it }
                }
            } else {
                // Regex first, then table
                if (canTryRegex) {
                    tryOneRegexAttempt(
                        pdfBytes, pdfText, headerSnippet, extractedSampleRows, allConfigs,
                        attemptNum, expectedCount, regexFailedAttempts, categoryNames, collector,
                    )?.let { return it }
                }
                if (sampleTableRows != null && sampleTableRows.size >= 2) {
                    tryOneTableAttempt(
                        pdfBytes, sampleTableRows, tableExtraction, attemptNum,
                        expectedCount, tableFailedAttempts, categoryNames, collector,
                    )?.let { return it }
                }
            }
        }

        val totalFailed = regexFailedAttempts.size + tableFailedAttempts.size
        if (totalFailed > 0) {
            Timber.w("All %d AI generation attempts failed (regex=%d, table=%d), falling through",
                totalFailed, regexFailedAttempts.size, tableFailedAttempts.size)
        }
        return null
    }

    /** Single regex generation attempt. Returns result on success, null to continue. */
    private suspend fun tryOneRegexAttempt(
        pdfBytes: ByteArray,
        pdfText: String,
        headerSnippet: String,
        extractedSampleRows: String,
        allConfigs: List<ParserConfig>,
        attemptNum: Int,
        expectedCount: Int,
        failedAttempts: MutableList<FailedAttempt>,
        categoryNames: CategoryNames,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        collector.emit(ImportStepEvent.AiConfigRequest(attemptNum))

        val generatedConfig = try {
            geminiService.generateParserConfig(
                headerSnippet = headerSnippet,
                sampleRows = extractedSampleRows,
                existingConfigs = allConfigs,
                previousAttempts = failedAttempts,
                pdfBlob = pdfBytes,
                categoryNames = categoryNames,
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
            return null
        }
        Timber.d("AI generated config for bank: %s (attempt %d/%d)", generatedConfig.bankId, attemptNum, MAX_AI_RETRIES)
        collector.emit(ImportStepEvent.AiConfigResponse(attemptNum, generatedConfig.bankId))

        // Validate: ReDoS + regex syntax + dateFormat
        val redosViolation = regexValidator.getReDoSViolation(generatedConfig.transactionPattern)
        if (redosViolation != null) {
            Timber.w("AI-generated regex failed ReDoS check (attempt %d/%d): %s", attemptNum, MAX_AI_RETRIES, redosViolation)
            collector.emit(ImportStepEvent.ValidationResult(attemptNum, "ReDoS", false, redosViolation))
            failedAttempts.add(FailedAttempt(generatedConfig, "Regex failed ReDoS safety check: $redosViolation"))
            return null
        }
        collector.emit(ImportStepEvent.ValidationResult(attemptNum, "ReDoS", true))

        if (!isRegexValid(generatedConfig.transactionPattern)) {
            val syntaxError = try { Regex(generatedConfig.transactionPattern); "" }
            catch (e: Exception) { e.message.orEmpty() }
            Timber.w("AI-generated regex has invalid syntax (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
            collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Regex syntax", false, syntaxError))
            failedAttempts.add(FailedAttempt(generatedConfig, "Regex syntax invalid: $syntaxError"))
            return null
        }
        collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Regex syntax", true))

        if (!isDateFormatValid(generatedConfig.dateFormat)) {
            val dateError = try { java.time.format.DateTimeFormatter.ofPattern(generatedConfig.dateFormat); "" }
            catch (e: Exception) { e.message.orEmpty() }
            Timber.w("AI-generated dateFormat is invalid (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
            collector.emit(ImportStepEvent.ValidationResult(attemptNum, "Date format", false, dateError))
            failedAttempts.add(FailedAttempt(generatedConfig, "DateFormat invalid: $dateError"))
            return null
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
            return null
        }

        if (aiResult.transactions.isEmpty()) {
            Timber.d("AI-generated config parsed 0 transactions (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
            val sampleLines = extractSampleLinesForDiagnostics(pdfText)
            val totalLines = pdfText.lines().drop(HEADER_SKIP_LINES).count { it.isNotBlank() }
            collector.emit(ImportStepEvent.AiConfigParseResult(attemptNum, 0))
            failedAttempts.add(FailedAttempt(
                generatedConfig,
                "Regex matched 0 transaction lines out of $totalLines total lines. Sample lines that should have matched:\n$sampleLines",
            ))
            return null
        }

        // Plausibility gate: reject if match rate is suspiciously high
        val totalLines = pdfText.lines().drop(HEADER_SKIP_LINES).count { it.isNotBlank() }
        if (totalLines > 0) {
            val matchRate = aiResult.transactions.size.toFloat() / totalLines
            if (matchRate > PLAUSIBILITY_MAX_MATCH_RATE && aiResult.transactions.size > PLAUSIBILITY_MIN_TRANSACTIONS) {
                Timber.w("AI config matched %d/%d lines (%.0f%%) — likely too loose (attempt %d/%d)",
                    aiResult.transactions.size, totalLines, matchRate * 100, attemptNum, MAX_AI_RETRIES)
                collector.emit(ImportStepEvent.AiConfigParseResult(attemptNum, aiResult.transactions.size))
                failedAttempts.add(FailedAttempt(
                    generatedConfig,
                    "Regex matched ${aiResult.transactions.size} of $totalLines non-blank lines (${(matchRate * 100).toInt()}%) — implausibly high match rate, regex is likely too loose",
                ))
                return null
            }
        }

        // Expected count gate: reject if parsed too few vs Gemini's estimate
        if (expectedCount > 0) {
            val ratio = aiResult.transactions.size.toFloat() / expectedCount
            if (ratio < MIN_YIELD_RATE) {
                Timber.d("AI regex matched %d but expected ~%d (%.0f%%) — low yield (attempt %d/%d)",
                    aiResult.transactions.size, expectedCount, ratio * 100, attemptNum, MAX_AI_RETRIES)
                collector.emit(ImportStepEvent.AiConfigParseResult(attemptNum, aiResult.transactions.size))
                failedAttempts.add(FailedAttempt(
                    generatedConfig,
                    "Parsed ${aiResult.transactions.size} but expected ~$expectedCount transactions (${(ratio * 100).toInt()}%) — low yield",
                ))
                return null
            }
        }

        Timber.d("AI-generated config parsed %d transactions (attempt %d/%d)", aiResult.transactions.size, attemptNum, MAX_AI_RETRIES)
        collector.emit(ImportStepEvent.AiConfigParseResult(attemptNum, aiResult.transactions.size))
        return RegexThenGeminiResult(
            transactions = assignCategories(aiResult.transactions, generatedConfig.categoryMap, collector),
            aiGeneratedConfig = generatedConfig,
            sampleRows = extractedSampleRows,
            aiMethod = AiMethod.REGEX_GENERATED,
            categoryMap = generatedConfig.categoryMap,
        )
    }

    /** Single table generation attempt. Returns result on success, null to continue. */
    @Suppress("LongParameterList")
    private suspend fun tryOneTableAttempt(
        pdfBytes: ByteArray,
        sampleTableRows: List<List<String>>,
        extraction: TableExtractionResult,
        attemptNum: Int,
        expectedCount: Int,
        tableFailedAttempts: MutableList<TableFailedAttempt>,
        categoryNames: CategoryNames,
        collector: ImportProgressCollector,
    ): RegexThenGeminiResult? {
        collector.emit(ImportStepEvent.AiTableConfigRequest(attemptNum))

        val generatedTableConfig = try {
            geminiService.generateTableParserConfig(
                sampleTableRows = sampleTableRows,
                previousAttempts = tableFailedAttempts,
                metadataRows = extraction.metadataRows,
                columnHeaderRow = extraction.columnHeaderRow,
                categoryNames = categoryNames,
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
            return null
        }
        Timber.d("AI generated table config for bank: %s (attempt %d/%d)", generatedTableConfig.bankId, attemptNum, MAX_AI_RETRIES)
        collector.emit(ImportStepEvent.AiTableConfigResponse(attemptNum, generatedTableConfig.bankId))

        if (!isDateFormatValid(generatedTableConfig.dateFormat)) {
            val dateError = try {
                java.time.format.DateTimeFormatter.ofPattern(generatedTableConfig.dateFormat); ""
            } catch (e: Exception) { e.message.orEmpty() }
            Timber.w("AI-generated table dateFormat is invalid (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
            tableFailedAttempts.add(TableFailedAttempt(generatedTableConfig, "DateFormat invalid: $dateError"))
            return null
        }

        val tableResult = try {
            withTimeout(AI_REGEX_TIMEOUT_MS) {
                runInterruptible { statementParser.tryParseWithTableConfig(pdfBytes, generatedTableConfig) }
            }
        } catch (_: TimeoutCancellationException) {
            Timber.w("AI-generated table config timed out (attempt %d/%d)", attemptNum, MAX_AI_RETRIES)
            tableFailedAttempts.add(TableFailedAttempt(generatedTableConfig, "Table parsing timed out"))
            return null
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
            return null
        }

        // Expected count gate: reject if parsed too few vs Gemini's estimate
        if (expectedCount > 0) {
            val ratio = tableResult.transactions.size.toFloat() / expectedCount
            if (ratio < MIN_YIELD_RATE) {
                Timber.d("AI table parsed %d but expected ~%d (%.0f%%) — low yield (attempt %d/%d)",
                    tableResult.transactions.size, expectedCount, ratio * 100, attemptNum, MAX_AI_RETRIES)
                tableFailedAttempts.add(
                    TableFailedAttempt(generatedTableConfig,
                        "Parsed ${tableResult.transactions.size} but expected ~$expectedCount transactions"),
                )
                return null
            }
        }

        Timber.d("AI-generated table config parsed %d transactions (attempt %d/%d)", tableResult.transactions.size, attemptNum, MAX_AI_RETRIES)
        return RegexThenGeminiResult(
            transactions = assignCategories(tableResult.transactions, generatedTableConfig.categoryMap, collector),
            aiMethod = AiMethod.TABLE_GENERATED,
            aiGeneratedTableConfig = generatedTableConfig,
            sampleTableRows = sampleTableRows,
            categoryMap = generatedTableConfig.categoryMap,
        )
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

    suspend fun cacheTableConfig(config: TableParserConfig) {
        try {
            val configJson = json.encodeToString(TableParserConfig.serializer(), config)
            val entity = ParserConfigEntity(
                id = "ai_table_${config.bankId}_${System.currentTimeMillis()}",
                bankId = config.bankId,
                configType = "table",
                configJson = configJson,
                version = 0,
                status = "active",
                source = "ai_cached",
                updatedAt = System.currentTimeMillis(),
            )
            parserConfigDao.upsertAll(listOf(entity))
            Timber.d("Cached table config for bank %s in Room", config.bankId)
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache table config")
        }
    }

    suspend fun cacheAiConfig(config: ParserConfig) {
        try {
            val configJson = json.encodeToString(ParserConfig.serializer(), config)
            val entity = ParserConfigEntity(
                id = "ai_regex_${config.bankId}_${System.currentTimeMillis()}",
                bankId = config.bankId,
                configType = "regex",
                configJson = configJson,
                version = 0,
                status = "active",
                source = "ai_cached",
                updatedAt = System.currentTimeMillis(),
            )
            parserConfigDao.upsertAll(listOf(entity))
            Timber.d("Cached AI config for bank %s in Room", config.bankId)
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
        // Halyk Bank (English) — operation names extracted from mixed-script details
        "Merchant payment transaction" to "Покупки",
        "Recharge card account through payment terminal" to "Пополнение",
        "Recharge card account" to "Пополнение",
        "Receipt to the card account" to "Пополнение",
        "Receipt of transfer" to "Пополнение",
        "Transfer on deposit" to "Перевод",
        "Transfer from deposit" to "Пополнение",
        "Transfer to another card" to "Перевод",
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
        categoryMap: Map<String, String> = emptyMap(),
        collector: ImportProgressCollector = NoOpCollector,
    ): List<ParsedTransaction> {
        val expenseCategories = categoryDao.getByType(TYPE_EXPENSE).toMutableList()
        val incomeCategories = categoryDao.getByType(TYPE_INCOME).toMutableList()

        val neededOperations = transactions
            .filter { it.categoryId == null }
            .map { Triple(it.operationType, it.details, it.type) }
            .distinct()

        for ((operation, details, type) in neededOperations) {
            val categories = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
            val resolvedName = resolveCategoryName(operation, details, categoryMap)
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
            val resolvedName = resolveCategoryName(tx.operationType, tx.details, categoryMap)
            val matched = categories.find { it.name == resolvedName }

            tx.copy(
                categoryId = matched?.id,
                suggestedCategoryName = tx.operationType,
            )
        }
        collector.emit(ImportStepEvent.CategoryAssignment(result.size))
        return result
    }

    /**
     * Resolves category name: hardcoded aliases → Gemini's categoryMap (prefix match) → raw fallback.
     */
    private fun resolveCategoryName(
        operationType: String,
        details: String,
        categoryMap: Map<String, String>,
    ): String {
        operationAliases[operationType]?.let { return it }
        categoryMap.findByPrefix(operationType)?.let { return it }
        categoryMap.findByPrefix(details)?.let { return it }
        return operationType.ifBlank { details }
    }

    private fun Map<String, String>.findByPrefix(text: String): String? {
        if (text.isBlank()) return null
        return this[text] ?: entries.firstOrNull { (key, _) ->
            key.startsWith(text) || text.startsWith(key)
        }?.value
    }

    companion object {
        private const val DEFAULT_IMPORT_CATEGORY_COLOR = 0xFFB0BEC5
        private const val AI_REGEX_TIMEOUT_MS = 5_000L
        private const val MAX_AI_RETRIES = 3
        private const val HEADER_SKIP_LINES = 10
        private const val PLAUSIBILITY_MAX_MATCH_RATE = 0.7f
        private const val PLAUSIBILITY_MIN_TRANSACTIONS = 20
        private const val MIN_YIELD_RATE = 0.5f
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
