package com.atelbay.money_manager.core.parser

import com.atelbay.money_manager.core.common.generateTransactionHash
import com.atelbay.money_manager.core.model.money.majorToMinor
import com.atelbay.money_manager.core.model.ParsedTransaction
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.core.remoteconfig.RegexParserProfile
import com.google.re2j.Matcher
import com.google.re2j.Pattern
import com.google.re2j.PatternSyntaxException
import kotlinx.datetime.LocalDate
import timber.log.Timber
import javax.inject.Inject

/**
 * Parses bank-statement rows using regex patterns from an untrusted [RegexParserProfile]
 * (AI-generated and user-supplied configs synced Firestore → Room).
 *
 * The hot matching path — `transactionPattern`, `skipPatterns`, `lineFixups` — runs on the
 * RE2 engine (`com.google.re2j`) rather than `kotlin.text.Regex` / `java.util.regex`. RE2 matches
 * in guaranteed **linear time with no backtracking**, so a maliciously- or accidentally-crafted
 * catastrophic pattern (e.g. `(a+)+$`) can no longer peg a CPU core. java.util.regex is NOT
 * interruptible — a coroutine timeout cancels the wrapper but the worker thread keeps burning —
 * so RE2 removes ReDoS as a class for config patterns, not merely as a palliative.
 *
 * ### Named-group syntax (single source of truth)
 * Configs store **Java/Kotlin** named-group syntax `(?<name>...)` (see GeminiServiceImpl, which
 * normalizes any Python `(?P<name>...)` the model emits back to `(?<name>` before persisting).
 * RE2J accepts only the **Python** form `(?P<name>...)`. We therefore convert `(?<name>` →
 * `(?P<name>` at compile time in [normalizeNamedGroups], **without** mutating what is stored in
 * the config / Firestore. Lookbehind `(?<=` / `(?<!` is never rewritten (and is unsupported by
 * RE2J anyway — it surfaces as an incompatible pattern, see below).
 *
 * ### Incompatibility policy (RE2 has no backreferences / lookaround)
 * A pattern using `(?=...)`, `(?<=...)`, `\1`, etc. fails [Pattern.compile] with
 * [PatternSyntaxException]. For the **matching** path we never silently fall back to
 * `kotlin.text.Regex` (that would reintroduce the ReDoS vector). Instead:
 *  - incompatible **transactionPattern** → return empty result, which triggers the AI fallback
 *    higher up the stack (same as today's behavior for an unusable pattern);
 *  - uncompilable **skipPattern** → fall back to a *literal* match (RE2 `Pattern.quote`), matching
 *    the long-standing "invalid skip pattern still filters its literal text" behavior; a lookaround
 *    skip pattern thus degrades into an inert literal filter rather than crashing the parse;
 *  - incompatible **lineFixup** → fall back to a **ReDoS-guarded `kotlin.Regex`**. Fixups are pure
 *    text-repair `replace` operations (they never feed the type/amount logic), and several real
 *    bank configs legitimately need lookaround that RE2 cannot express — e.g. Freedom's
 *    "Сумма в …обработке" reorder and Eurasian's misplaced-time reorder (a tempered-greedy token
 *    `(?:(?!\d{2}:\d{2}:\d{2}).)+?`). Each fixup has already passed
 *    [RegexValidator.getReDoSViolation] before this fallback and input is capped at
 *    [MAX_LINE_LENGTH], so the residual backtracking surface is bounded. A fixup that is *also*
 *    an invalid `kotlin.Regex` is dropped.
 */
class RegexStatementParser @Inject constructor(
    private val regexValidator: RegexValidator,
) {

    /**
     * Test-only convenience constructor. Kept separate (and un-annotated) instead of a default on
     * the @Inject constructor: an all-default primary makes Kotlin emit a synthetic no-arg
     * constructor that Dagger also treats as @Inject, which fails with "may only contain one
     * injected constructor". Production wiring always goes through Hilt via the primary constructor.
     */
    constructor() : this(RegexValidator())

    companion object {
        /**
         * Hard cap on the length of any single line fed to the regex engine. Retained as cheap
         * defense-in-depth: RE2 already guarantees linear time, but bounding input still caps
         * absolute work and protects the trusted `kotlin.text.Regex` date pre-filters below.
         * Real statement rows are far shorter than this.
         */
        private const val MAX_LINE_LENGTH = 2_000

        /**
         * Rewrites Java/Kotlin named-group openers `(?<name>` to RE2J's `(?P<name>`. Only fires when
         * `<` is followed by a name char, so lookbehind `(?<=` / `(?<!` is left untouched. This is a
         * fixed, linear (ReDoS-free) regex applied to the untrusted pattern *string* — it never
         * matches statement content, so running it on `kotlin.text.Regex` is safe.
         */
        private val NAMED_GROUP_JAVA_TO_RE2J = Regex("""\(\?<(?=[A-Za-z])""")

        /**
         * Converts a config pattern's Java/Kotlin named-group syntax to the RE2J dialect. The
         * single source of truth for the `(?<name>` → `(?P<name>` decision; exercised directly by
         * tests. Does not mutate what is stored in the config/Firestore.
         */
        internal fun normalizeNamedGroups(pattern: String): String =
            NAMED_GROUP_JAVA_TO_RE2J.replace(pattern, "(?P<")
    }

    /**
     * Compiles an untrusted content pattern on the RE2 engine after named-group normalization.
     * Returns `null` (instead of throwing) when the pattern is incompatible with RE2 — callers
     * decide the per-pattern fallback policy (see class kdoc).
     */
    private fun compileContentPattern(pattern: String, multiline: Boolean): Pattern? =
        try {
            val flags = if (multiline) Pattern.MULTILINE else 0
            Pattern.compile(normalizeNamedGroups(pattern), flags)
        } catch (e: PatternSyntaxException) {
            Timber.w(e, "Pattern incompatible with RE2 — rejected: %s", pattern)
            null
        }

    /**
     * Builds a single line-fixup as a `(line) -> line` replacement, applying the policy from the
     * class kdoc: ReDoS-flag drop → RE2 (preferred) → ReDoS-guarded `kotlin.Regex` fallback for
     * RE2-incompatible-but-safe patterns (e.g. lookaround reorders) → drop if even that fails.
     * Returns `null` when the fixup cannot be applied at all.
     */
    private fun buildFixup(entry: List<String>): ((String) -> String)? {
        if (entry.size != 2) {
            Timber.w("lineFixup dropped — wrong arity %d (expected 2): %s", entry.size, entry)
            return null
        }
        val (patternStr, replacement) = entry
        regexValidator.getReDoSViolation(patternStr)?.let {
            Timber.w("lineFixup dropped — ReDoS risk (%s): %s", it, patternStr)
            return null
        }
        compileContentPattern(patternStr, multiline = false)?.let { re2 ->
            return { line -> re2.matcher(line).replaceAll(replacement) }
        }
        // RE2-incompatible (lookaround/backreference) but ReDoS-heuristic-clean: run on the
        // backtracking engine, bounded by MAX_LINE_LENGTH. See class kdoc for the rationale.
        val kotlinRegex = try {
            Regex(patternStr)
        } catch (e: Exception) {
            Timber.w(e, "lineFixup dropped — invalid regex: %s", patternStr)
            return null
        }
        Timber.d("lineFixup using kotlin.Regex fallback (RE2-incompatible): %s", patternStr)
        return { line -> kotlinRegex.replace(line, replacement) }
    }

    fun parse(text: String, config: RegexParserProfile): List<ParsedTransaction> {
        // Drop ReDoS-flagged skip patterns first (defense-in-depth; RE2 itself can't backtrack),
        // then compile on RE2. Skip patterns run with find() over every line and, unlike
        // transactionPattern, are never validated upstream. An uncompilable skip pattern degrades
        // to a literal match so it still filters its own literal text (and a lookaround skip
        // pattern becomes an inert literal rather than crashing the parse).
        val skipPatterns = config.skipPatterns.mapNotNull { pattern ->
            regexValidator.getReDoSViolation(pattern)?.let {
                Timber.w("skipPattern dropped — ReDoS risk (%s): %s", it, pattern)
                return@mapNotNull null
            }
            compileContentPattern(pattern, multiline = false)
                ?: Pattern.compile(Pattern.quote(pattern))
        }
        // Two-phase skip+join: first remove non-date continuation lines that match skip
        // patterns (so they don't get joined to the previous transaction), then join
        // remaining continuation lines, then skip again on joined lines (catches
        // date-starting header rows whose continuation lines are now merged).
        // datePattern is a trusted internal constant — stays on kotlin.text.Regex.
        val datePattern = Regex("""^\s*${ParserPatterns.DATE_CORE}""")
        val preFiltered = text.lines().filterNot { line ->
            line.isNotBlank() && !datePattern.containsMatchIn(line) &&
                skipPatterns.any { it.containsMatchIn(line) }
        }
        val joinedText = if (config.joinLines) joinContinuationLines(preFiltered.joinToString("\n")) else preFiltered.joinToString("\n")
        val processedText = joinedText.lines()
            .filterNot { line -> skipPatterns.any { it.containsMatchIn(line) } }
            .joinToString("\n")
        // Incompatible transactionPattern → empty result → AI fallback kicks in upstream.
        val pattern = compileContentPattern(config.transactionPattern, multiline = true) ?: run {
            Timber.w(
                "transactionPattern incompatible with RE2 — returning empty (AI fallback): %s",
                config.transactionPattern,
            )
            return emptyList()
        }
        val fixups = config.lineFixups.mapNotNull { entry -> buildFixup(entry) }

        val transactions = mutableListOf<ParsedTransaction>()

        for (rawLine in processedText.lines()) {
            // Bound regex input length — see MAX_LINE_LENGTH (defense-in-depth).
            val line = if (rawLine.length > MAX_LINE_LENGTH) rawLine.take(MAX_LINE_LENGTH) else rawLine
            val fixedLine = try {
                fixups.fold(line) { current, fixup -> fixup(current) }
            } catch (e: Exception) {
                Timber.w(e, "lineFixup replacement failed on line: %s", line.trim())
                line
            }

            val matcher = pattern.matcher(fixedLine)
            if (!matcher.find()) continue

            try {
                val transaction = matchToTransaction(matcher, config)
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
        // Group by (date, details) only — NOT type. Banks like Eurasian emit a currency-conversion
        // triplet (card debit, mirrored credit, actual KZT debit) with mixed +/- signs for ONE
        // transaction; including type would stop the mirror credit from collapsing. Amounts are
        // already non-negative magnitudes (see matchToTransaction), so max picks the real KZT row.
        val result = transactions
            .groupBy { Pair(it.date, it.details) }
            .map { (_, group) -> group.maxByOrNull { it.amount }!! }
        Timber.d("Dedup reduced %d → %d transactions", transactions.size, result.size)
        return result
    }

    private fun matchToTransaction(
        match: Matcher,
        config: RegexParserProfile,
    ): ParsedTransaction {
        val dateStr: String
        val sign: String
        val amountStr: String
        val operation: String
        val details: String

        if (config.useNamedGroups) {
            dateStr = match.group("date") ?: error("Named group 'date' not found in match")
            sign = safeNamedGroup(match, "sign")
            amountStr = match.group("amount") ?: error("Named group 'amount' not found in match")
            operation = safeNamedGroup(match, "operation")
            details = safeNamedGroup(match, "details")
        } else {
            // Positional groups 1..5 = date, sign, amount, operation, details. RE2J group(int)
            // returns null for a group that did not participate (e.g. an empty `()` sign group),
            // so coalesce to "" — mirrors kotlin destructured's empty-string behavior.
            dateStr = match.group(1) ?: ""
            sign = match.group(2) ?: ""
            amountStr = match.group(3) ?: ""
            operation = match.group(4) ?: ""
            details = match.group(5) ?: ""
        }

        val javaParsed = AmountParser.parseDateString(dateStr, config.dateFormat)
        val date = LocalDate(javaParsed.year, javaParsed.monthValue, javaParsed.dayOfMonth)

        // Magnitude only — sign/type is decided separately below. Mirrors TableStatementParser,
        // and guards against configs whose amount group accidentally captures a leading '-'.
        val amount = kotlin.math.abs(AmountParser.parseAmount(amountStr, config.amountFormat))

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

        val amountMinor = majorToMinor(amount)
        val hash = generateTransactionHash(date, amountMinor, type.value, details.trim())

        return ParsedTransaction(
            date = date,
            amount = amountMinor,
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
     * RE2J `Matcher.group(name)` throws IllegalArgumentException if the group name is not defined
     * in the regex, and returns null if the group is defined but did not participate in the match —
     * both collapse to "".
     */
    private fun safeNamedGroup(match: Matcher, name: String): String =
        try { match.group(name) ?: "" } catch (_: IllegalArgumentException) { "" }

    /** RE2 analogue of kotlin.text.Regex.containsMatchIn — true if the pattern matches anywhere. */
    private fun Pattern.containsMatchIn(input: String): Boolean = matcher(input).find()

    /**
     * Joins continuation lines that don't start with a date pattern to the previous line.
     * Used for bank statements where transaction details span multiple lines.
     */
    private fun joinContinuationLines(text: String): String {
        val datePattern = Regex("""^\s*${ParserPatterns.DATE_CORE}""")
        val lines = text.lines()
        // StringBuilder per logical line — avoids O(n²) re-concatenation when many continuation
        // lines append to the same parent.
        val joined = mutableListOf<StringBuilder>()

        for (line in lines) {
            if (line.isBlank()) continue
            if (datePattern.containsMatchIn(line) || joined.isEmpty()) {
                joined.add(StringBuilder(line))
            } else {
                joined.last().append(' ').append(line.trim())
            }
        }

        return joined.joinToString("\n")
    }
}
