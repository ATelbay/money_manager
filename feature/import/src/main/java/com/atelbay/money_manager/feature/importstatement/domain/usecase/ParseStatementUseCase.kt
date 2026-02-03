package com.atelbay.money_manager.feature.importstatement.domain.usecase

import com.atelbay.money_manager.core.ai.GeminiService
import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.database.dao.CategoryDao
import com.atelbay.money_manager.core.database.dao.TransactionDao
import com.atelbay.money_manager.core.model.ImportResult
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
private data class GeminiResponse(
    val transactions: List<GeminiTransaction>,
)

@Serializable
private data class GeminiTransaction(
    val date: String,
    val amount: Double,
    val type: String,
    val operationType: String,
    val details: String,
    val categoryId: Long? = null,
    val suggestedCategoryName: String? = null,
    val confidence: Float,
)

class ParseStatementUseCase @Inject constructor(
    private val geminiService: GeminiService,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(pages: List<ByteArray>): ImportResult {
        val expenseCategories = categoryDao.getByType("expense")
        val incomeCategories = categoryDao.getByType("income")

        val prompt = buildPrompt(expenseCategories, incomeCategories)
        val responseText = geminiService.parseContent(pages, prompt)

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
                    "income" -> TransactionType.INCOME
                    "expense" -> TransactionType.EXPENSE
                    else -> TransactionType.EXPENSE
                }
                val hash = generateTransactionHash(date, tx.amount, tx.type, tx.details)
                ParsedTransaction(
                    date = date,
                    amount = tx.amount,
                    type = type,
                    operationType = tx.operationType,
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
        appendLine("Распарси банковскую выписку и верни JSON.")
        appendLine()
        appendLine("Доступные категории расходов:")
        expenseCategories.forEach { appendLine("- id: ${it.id}, name: \"${it.name}\"") }
        appendLine()
        appendLine("Доступные категории доходов:")
        incomeCategories.forEach { appendLine("- id: ${it.id}, name: \"${it.name}\"") }
        appendLine()
        appendLine(
            """
            Формат ответа (только JSON, без markdown):
            {
              "transactions": [
                {
                  "date": "2026-01-28",
                  "amount": 24990.00,
                  "type": "expense",
                  "operationType": "Покупка",
                  "details": "Xiaomi Official Store",
                  "categoryId": 4,
                  "suggestedCategoryName": null,
                  "confidence": 0.95
                }
              ]
            }

            Правила:
            - amount всегда положительное число
            - type: "income" для пополнений, "expense" для покупок/переводов/снятий
            - Если уверен в категории — ставь categoryId и confidence > 0.8
            - Если не уверен — categoryId: null, suggestedCategoryName: "предложение", confidence < 0.7
            - Переводы физлицам (имена) — категория "Переводы"
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
