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
        val skipPatterns = config.skipPatterns.map { Regex(Regex.escape(it)) }
        val filteredText = text.lines()
            .filterNot { line -> skipPatterns.any { it.containsMatchIn(line) } }
            .joinToString("\n")
        val processedText = if (config.joinLines) joinContinuationLines(filteredText) else filteredText
        val pattern = Regex(config.transactionPattern, RegexOption.MULTILINE)
        val dateFormatter = DateTimeFormatter.ofPattern(config.dateFormat)

        val transactions = mutableListOf<ParsedTransaction>()

        for (line in processedText.lines()) {

            val match = pattern.find(line) ?: continue

            try {
                val transaction = matchToTransaction(match, dateFormatter, config)
                transactions.add(transaction)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse line: %s", line.trim())
            }
        }

        Timber.d("RegEx parsed %d transactions from %d lines", transactions.size, processedText.lines().size)

        return if (config.deduplicateMaxAmount) deduplicateByMaxAmount(transactions) else transactions
    }

    /**
     * Removes duplicate transaction rows by keeping the entry with the largest amount for each
     * (date, details) group. Used for banks like Eurasian that emit multiple rows per transaction
     * (e.g. card debit + account mirror + actual KZT debit).
     *
     * Known limitation: two separate charges to the same merchant on the same date will be
     * collapsed into one. Users should review and add the second transaction manually.
     */
    private fun deduplicateByMaxAmount(transactions: List<ParsedTransaction>): List<ParsedTransaction> {
        val result = transactions
            .groupBy { Pair(it.date, it.details) }
            .map { (_, group) -> group.maxBy { it.amount } }
        Timber.d("Dedup reduced %d → %d transactions", transactions.size, result.size)
        return result
    }

    private fun matchToTransaction(
        match: MatchResult,
        dateFormatter: DateTimeFormatter,
        config: ParserConfig,
    ): ParsedTransaction {
        val dateStr: String
        val sign: String
        val amountStr: String
        val operation: String
        val details: String

        if (config.useNamedGroups) {
            dateStr = match.groups["date"]?.value ?: error("Named group 'date' not found in match")
            sign = match.groups["sign"]?.value ?: ""
            amountStr = match.groups["amount"]?.value ?: error("Named group 'amount' not found in match")
            operation = match.groups["operation"]?.value ?: ""
            details = match.groups["details"]?.value ?: ""
        } else {
            val (d, s, a, op, det) = match.destructured
            dateStr = d; sign = s; amountStr = a; operation = op; details = det
        }

        val javaParsed = java.time.LocalDate.parse(dateStr.trim(), dateFormatter)
        val date = LocalDate(javaParsed.year, javaParsed.monthValue, javaParsed.dayOfMonth)

        val amount = parseAmount(amountStr, config.amountFormat)

        // NOTE: operationTypeMap is only consulted in the else branch.
        // When useSignForType=true or negativeSignMeansExpense=true, the sign drives type
        // classification and operationTypeMap entries have no effect.
        val type = when {
            config.useSignForType -> if (sign == "+") TransactionType.INCOME else TransactionType.EXPENSE
            config.negativeSignMeansExpense -> if (sign == "-") TransactionType.EXPENSE else TransactionType.INCOME
            else -> {
                val typeValue = config.operationTypeMap[operation] ?: "expense"
                if (typeValue == "income") TransactionType.INCOME else TransactionType.EXPENSE
            }
        }

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

    private fun parseAmount(amountStr: String, format: String): Double = when (format) {
        "comma_dot" -> amountStr.replace(",", "").toDouble()
        else -> amountStr.replace("\\s".toRegex(), "").replace(",", ".").toDouble()
    }

    /**
     * Joins continuation lines that don't start with a date pattern to the previous line.
     * Used for bank statements where transaction details span multiple lines.
     */
    private fun joinContinuationLines(text: String): String {
        val datePattern = Regex("""^\s*\d{2}\.\d{2}\.\d{2,4}""")
        val lines = text.lines()
        val joined = mutableListOf<String>()

        for (line in lines) {
            if (line.isBlank()) continue
            if (datePattern.containsMatchIn(line) || joined.isEmpty()) {
                joined.add(line)
            } else {
                joined[joined.lastIndex] = joined.last() + " " + line.trim()
            }
        }

        return joined.joinToString("\n")
    }
}
