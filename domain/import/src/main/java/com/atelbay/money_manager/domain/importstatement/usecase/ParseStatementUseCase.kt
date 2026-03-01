package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.database.entity.CategoryEntity
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.parser.StatementParser
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

class ParseStatementUseCase @Inject constructor(
    private val statementParser: StatementParser,
    private val geminiService: GeminiService,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(blobs: List<Pair<ByteArray, String>>): ImportResult {
        val pdfBlob = blobs.firstOrNull { it.second == "application/pdf" }

        val transactions = if (pdfBlob != null) {
            tryRegexThenGemini(pdfBlob.first, blobs)
        } else {
            parseWithGemini(blobs)
        }

        return deduplicateAndBuildResult(transactions)
    }

    private suspend fun tryRegexThenGemini(
        pdfBytes: ByteArray,
        blobs: List<Pair<ByteArray, String>>,
    ): List<ParsedTransaction> {
        val regexResult = statementParser.tryParsePdf(pdfBytes)

        if (regexResult != null && regexResult.transactions.isNotEmpty()) {
            Timber.d(
                "RegEx parsed %d transactions for bank %s",
                regexResult.transactions.size,
                regexResult.bankId,
            )
            return assignCategories(regexResult.transactions)
        }

        Timber.d("RegEx parsing failed or empty, falling back to Gemini")
        return parseWithGemini(blobs)
    }

    private suspend fun parseWithGemini(blobs: List<Pair<ByteArray, String>>): List<ParsedTransaction> {
        val expenseCategories = categoryDao.getByType("expense")
        val incomeCategories = categoryDao.getByType("income")

        val prompt = buildPrompt(expenseCategories, incomeCategories)
        val responseText = geminiService.parseContent(blobs, prompt)

        val jsonString = extractJson(responseText)
        val parsed = try {
            json.decodeFromString<GeminiResponse>(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Gemini response")
            return emptyList()
        }

        val errors = mutableListOf<String>()
        return parsed.transactions.mapNotNull { tx ->
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
    }

    // Maps raw operation names from bank statements to existing default category names.
    // Handles both Russian and English variants.
    private val operationAliases = mapOf(
        "Покупка" to "Покупки",
        "Purchase" to "Покупки",
        "Оплата" to "Покупки",
        "Payment" to "Покупки",
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
                name = operation,
                icon = "label",
                color = DEFAULT_IMPORT_CATEGORY_COLOR,
                type = dbType,
                isDefault = false,
            )
            val id = categoryDao.insert(newCategory)
            val created = newCategory.copy(id = id)
            categories.add(created)
            Timber.d("Created category '%s' (%s) with id=%d", operation, dbType, id)
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
    }

    private suspend fun deduplicateAndBuildResult(
        transactions: List<ParsedTransaction>,
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
            errors = emptyList(),
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
