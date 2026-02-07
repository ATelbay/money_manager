package com.atelbay.money_manager.feature.importstatement.domain.usecase

import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    private val geminiService: GeminiService,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(blobs: List<Pair<ByteArray, String>>): ImportResult {
        val expenseCategories = categoryDao.getByType("expense")
        val incomeCategories = categoryDao.getByType("income")

        val prompt = buildPrompt(expenseCategories, incomeCategories)
        val responseText = geminiService.parseContent(blobs, prompt)

        val jsonString = extractJson(responseText)
        val parsed = try {
            json.decodeFromString<GeminiResponse>(jsonString)
        } catch (e: Exception) {
            return ImportResult(
                total = 0,
                newTransactions = emptyList(),
                duplicates = 0,
                errors = listOf("Не удалось распарсить ответ AI: ${e.message}"),
            )
        }

        val errors = mutableListOf<String>()
        val transactions = parsed.transactions.mapNotNull { tx ->
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

        val hashes = transactions.map { it.uniqueHash }
        val existingHashes = if (hashes.isNotEmpty()) {
            transactionDao.getExistingHashes(hashes)
        } else {
            emptyList()
        }.toSet()

        val newTransactions = transactions.filter { it.uniqueHash !in existingHashes }
        val duplicates = transactions.size - newTransactions.size

        return ImportResult(
            total = parsed.transactions.size,
            newTransactions = newTransactions,
            duplicates = duplicates,
            errors = errors,
        )
    }

    private fun buildPrompt(
        expenseCategories: List<com.atelbay.money_manager.core.database.entity.CategoryEntity>,
        incomeCategories: List<com.atelbay.money_manager.core.database.entity.CategoryEntity>,
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
