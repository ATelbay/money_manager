package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.ParserConfig
import kotlinx.datetime.LocalDate
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RegexStatementParser @Inject constructor() {

    fun parse(text: String, config: ParserConfig): List<ParsedTransaction> {
        val pattern = Regex(config.transactionPattern, RegexOption.MULTILINE)
        val dateFormatter = DateTimeFormatter.ofPattern(config.dateFormat)
        val skipPatterns = config.skipPatterns.map { Regex(Regex.escape(it)) }

        val transactions = mutableListOf<ParsedTransaction>()

        for (line in text.lines()) {
            if (skipPatterns.any { it.containsMatchIn(line) }) continue

            val match = pattern.find(line) ?: continue

            try {
                val transaction = matchToTransaction(match, dateFormatter, config)
                transactions.add(transaction)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse line: %s", line.trim())
            }
        }

        Timber.d("RegEx parsed %d transactions from %d lines", transactions.size, text.lines().size)
        return transactions
    }

    private fun matchToTransaction(
        match: MatchResult,
        dateFormatter: DateTimeFormatter,
        config: ParserConfig,
    ): ParsedTransaction {
        val (dateStr, sign, amountStr, operation, details) = match.destructured

        val javaParsed = java.time.LocalDate.parse(dateStr, dateFormatter)
        val date = LocalDate(javaParsed.year, javaParsed.monthValue, javaParsed.dayOfMonth)

        val amount = amountStr.replace("\\s".toRegex(), "").replace(",", ".").toDouble()

        val typeValue = config.operationTypeMap[operation] ?: "expense"
        val type = if (typeValue == "income") TransactionType.INCOME else TransactionType.EXPENSE

        val hash = generateTransactionHash(date, amount, type.value, details.trim())

        return ParsedTransaction(
            date = date,
            amount = amount,
            type = type,
            operationType = operation,
            details = details.trim(),
            categoryId = null,
            suggestedCategoryName = operation,
            confidence = 1.0f,
            needsReview = false,
            uniqueHash = hash,
        )
    }
}
