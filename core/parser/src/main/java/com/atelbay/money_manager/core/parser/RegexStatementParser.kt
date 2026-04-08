package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import kotlinx.datetime.LocalDate
import timber.log.Timber
import java.util.regex.PatternSyntaxException
import javax.inject.Inject

class RegexStatementParser @Inject constructor() {

    fun parse(text: String, config: RegexParserProfile): List<ParsedTransaction> {
        val skipPatterns = config.skipPatterns.map { pattern ->
            try {
                Regex(pattern)
            } catch (_: PatternSyntaxException) {
                Regex(Regex.escape(pattern))
            }
        }
        // Two-phase skip+join: first remove non-date continuation lines that match skip
        // patterns (so they don't get joined to the previous transaction), then join
        // remaining continuation lines, then skip again on joined lines (catches
        // date-starting header rows whose continuation lines are now merged).
        val datePattern = Regex("""^\s*${ParserPatterns.DATE_CORE}""")
        val preFiltered = text.lines().filterNot { line ->
            line.isNotBlank() && !datePattern.containsMatchIn(line) &&
                skipPatterns.any { it.containsMatchIn(line) }
        }
        val joinedText = if (config.joinLines) joinContinuationLines(preFiltered.joinToString("\n")) else preFiltered.joinToString("\n")
        val processedText = joinedText.lines()
            .filterNot { line -> skipPatterns.any { it.containsMatchIn(line) } }
            .joinToString("\n")
        val pattern = Regex(config.transactionPattern, RegexOption.MULTILINE)
        val fixups = config.lineFixups.mapNotNull { entry ->
            if (entry.size == 2) {
                try { Regex(entry[0]) to entry[1] } catch (e: PatternSyntaxException) {
                    Timber.w(e, "lineFixup dropped — invalid regex: %s", entry[0])
                    null
                }
            } else {
                Timber.w("lineFixup dropped — wrong arity %d (expected 2): %s", entry.size, entry)
                null
            }
        }

        val transactions = mutableListOf<ParsedTransaction>()

        for (line in processedText.lines()) {
            val fixedLine = try {
                fixups.fold(line) { current, (pat, replacement) ->
                    current.replace(pat, replacement)
                }
            } catch (e: Exception) {
                Timber.w(e, "lineFixup replacement failed on line: %s", line.trim())
                line
            }

            val match = pattern.find(fixedLine) ?: continue

            try {
                val transaction = matchToTransaction(match, config)
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
        config: RegexParserProfile,
    ): ParsedTransaction {
        val dateStr: String
        val sign: String
        val amountStr: String
        val operation: String
        val details: String

        if (config.useNamedGroups) {
            dateStr = match.groups["date"]?.value ?: error("Named group 'date' not found in match")
            sign = safeNamedGroup(match, "sign")
            amountStr = match.groups["amount"]?.value ?: error("Named group 'amount' not found in match")
            operation = safeNamedGroup(match, "operation")
            details = safeNamedGroup(match, "details")
        } else {
            val (d, s, a, op, det) = match.destructured
            dateStr = d; sign = s; amountStr = a; operation = op; details = det
        }

        val javaParsed = AmountParser.parseDateString(dateStr, config.dateFormat)
        val date = LocalDate(javaParsed.year, javaParsed.monthValue, javaParsed.dayOfMonth)

        val amount = AmountParser.parseAmount(amountStr, config.amountFormat)

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

    /**
     * Safely extract a named group that may not exist in the pattern.
     * On Android JVM, `match.groups[name]` throws IllegalArgumentException
     * if the group name is not defined in the regex (as opposed to returning null).
     */
    private fun safeNamedGroup(match: MatchResult, name: String): String =
        try { match.groups[name]?.value ?: "" } catch (_: IllegalArgumentException) { "" }

    /**
     * Joins continuation lines that don't start with a date pattern to the previous line.
     * Used for bank statements where transaction details span multiple lines.
     */
    private fun joinContinuationLines(text: String): String {
        val datePattern = Regex("""^\s*${ParserPatterns.DATE_CORE}""")
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
