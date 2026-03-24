package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TableParserConfig
import com.atelbay.money_manager.core.model.TransactionType
import kotlinx.datetime.LocalDate
import timber.log.Timber
import javax.inject.Inject

class TableStatementParser @Inject constructor() {

    fun parse(table: List<List<String>>, config: TableParserConfig): List<ParsedTransaction> {
        require(config.dateColumn >= 0) { "dateColumn must be non-negative, got ${config.dateColumn}" }
        require(config.amountColumn >= 0) { "amountColumn must be non-negative, got ${config.amountColumn}" }
        require(config.skipHeaderRows >= 0) { "skipHeaderRows must be non-negative, got ${config.skipHeaderRows}" }

        val maxColumnIndex = table.firstOrNull()?.size?.minus(1) ?: return emptyList()
        if (config.dateColumn > maxColumnIndex || config.amountColumn > maxColumnIndex) {
            Timber.w(
                "Column index out of bounds: dateColumn=%d, amountColumn=%d, maxColumn=%d",
                config.dateColumn, config.amountColumn, maxColumnIndex,
            )
            return emptyList()
        }

        val dataRows = table.drop(config.skipHeaderRows)
        val transactions = mutableListOf<ParsedTransaction>()

        for ((rowIndex, row) in dataRows.withIndex()) {
            try {
                val transaction = parseRow(row, config) ?: continue
                transactions.add(transaction)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse table row %d: %s", rowIndex, row.joinToString(" | "))
            }
        }

        Timber.d("TableStatementParser parsed %d transactions from %d rows", transactions.size, dataRows.size)

        return if (config.deduplicateMaxAmount) deduplicateByMaxAmount(transactions) else transactions
    }

    private fun parseRow(row: List<String>, config: TableParserConfig): ParsedTransaction? {
        val dateStr = row.getOrNull(config.dateColumn)?.trim()
        val amountStr = row.getOrNull(config.amountColumn)?.trim()

        if (dateStr.isNullOrBlank() || amountStr.isNullOrBlank()) return null

        val javaParsed = try {
            AmountParser.parseDateString(extractFirstDate(dateStr, config.dateFormat), config.dateFormat)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse date '%s' with format '%s'", dateStr, config.dateFormat)
            return null
        }
        val date = LocalDate(javaParsed.year, javaParsed.monthValue, javaParsed.dayOfMonth)

        val rawAmount = try {
            AmountParser.parseAmount(amountStr, config.amountFormat)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse amount '%s' with format '%s'", amountStr, config.amountFormat)
            return null
        }
        val amount = kotlin.math.abs(rawAmount)

        val operation = config.operationColumn?.let { row.getOrNull(it)?.trim() }.orEmpty()
        val details = config.detailsColumn?.let { row.getOrNull(it)?.trim() }.orEmpty()
        val signCell = config.signColumn?.let { row.getOrNull(it)?.trim() }.orEmpty()

        val type = when {
            config.negativeSignMeansExpense -> {
                val isNegative = rawAmount < 0 || signCell == "-"
                if (isNegative) TransactionType.EXPENSE else TransactionType.INCOME
            }
            signCell.isNotBlank() -> {
                if (signCell == "+") TransactionType.INCOME else TransactionType.EXPENSE
            }
            else -> {
                val typeValue = config.operationTypeMap[operation] ?: "expense"
                if (typeValue == "income") TransactionType.INCOME else TransactionType.EXPENSE
            }
        }

        val descriptionForHash = details.ifBlank { operation }
        val hash = generateTransactionHash(date, amount, type.value, descriptionForHash)

        return ParsedTransaction(
            date = date,
            amount = amount,
            type = type,
            operationType = operation,
            details = descriptionForHash,
            categoryId = null,
            suggestedCategoryName = operation.ifBlank { null },
            confidence = 1.0f,
            needsReview = false,
            uniqueHash = hash,
        )
    }

    private fun extractFirstDate(cell: String, dateFormat: String): String {
        val pattern = dateFormat
            .replace(".", "\\.")
            .replace("-", "\\-")
            .replace(Regex("[yMd]+"), "\\\\d+")
        val match = Regex(pattern).find(cell)
        return match?.value ?: cell.trim()
    }

    private fun deduplicateByMaxAmount(transactions: List<ParsedTransaction>): List<ParsedTransaction> {
        val result = transactions
            .groupBy { Pair(it.date, it.details) }
            .map { (_, group) -> group.maxBy { it.amount } }
        Timber.d("Table dedup reduced %d → %d transactions", transactions.size, result.size)
        return result
    }
}
