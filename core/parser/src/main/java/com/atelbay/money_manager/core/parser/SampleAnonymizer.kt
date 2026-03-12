package com.atelbay.money_manager.core.parser

import javax.inject.Inject

class SampleAnonymizer @Inject constructor() {

    fun anonymize(sampleRows: String): String {
        if (sampleRows.isBlank()) return sampleRows

        val merchantMap = mutableMapOf<String, String>()
        var merchantCounter = 0

        return sampleRows.lines().joinToString("\n") { line ->
            anonymizeLine(line, merchantMap, { ++merchantCounter })
        }
    }

    private fun anonymizeLine(
        line: String,
        merchantMap: MutableMap<String, String>,
        nextIndex: () -> Int,
    ): String {
        if (line.isBlank()) return line

        // Step 1: identify protected spans (dates, amounts, currency, operation words, symbols)
        val protectedRanges = mutableListOf<IntRange>()

        // Dates: dd.MM.yyyy, dd/MM/yyyy, dd.MM.yy, dd/MM/yy
        DATE_PATTERN.findAll(line).forEach { protectedRanges += it.range }

        // Amounts: digit sequences with optional space/dot/comma separators, including currency symbols
        AMOUNT_PATTERN.findAll(line).forEach { protectedRanges += it.range }

        // Currency codes (3 uppercase letters from known set) and currency symbols
        CURRENCY_PATTERN.findAll(line).forEach { protectedRanges += it.range }
        CURRENCY_SYMBOL_PATTERN.findAll(line).forEach { protectedRanges += it.range }

        // Sign indicators (+/-)
        SIGN_PATTERN.findAll(line).forEach { protectedRanges += it.range }

        // Operation type words (common banking terms in Russian and English)
        OPERATION_PATTERN.findAll(line).forEach { protectedRanges += it.range }

        // Step 2: remove account/card number patterns
        var processed = CARD_MASK_PATTERN.replace(line, "")
        processed = IBAN_PATTERN.replace(processed, "")
        // Remove long digit sequences only if they don't overlap with date/amount ranges
        val preProtected = mutableListOf<IntRange>()
        DATE_PATTERN.findAll(processed).forEach { preProtected += it.range }
        AMOUNT_PATTERN.findAll(processed).forEach { preProtected += it.range }
        processed = replaceUnprotected(processed, LONG_DIGIT_PATTERN, preProtected)

        // Recalculate protected ranges on the processed line
        val newProtected = mutableListOf<IntRange>()
        DATE_PATTERN.findAll(processed).forEach { newProtected += it.range }
        AMOUNT_PATTERN.findAll(processed).forEach { newProtected += it.range }
        CURRENCY_PATTERN.findAll(processed).forEach { newProtected += it.range }
        CURRENCY_SYMBOL_PATTERN.findAll(processed).forEach { newProtected += it.range }
        SIGN_PATTERN.findAll(processed).forEach { newProtected += it.range }
        OPERATION_PATTERN.findAll(processed).forEach { newProtected += it.range }

        // Step 3: find unprotected word tokens that look like merchant names
        val merchantTokenRanges = mutableListOf<Pair<IntRange, String>>()
        // Find consecutive word sequences (including quoted strings) not in protected ranges
        MERCHANT_CANDIDATE_PATTERN.findAll(processed).forEach { match ->
            val range = match.range
            if (!isProtected(range, newProtected)) {
                val value = match.value.trim()
                if (value.isNotBlank() && !isNumericOnly(value)) {
                    merchantTokenRanges += range to value
                }
            }
        }

        // Group adjacent merchant tokens into full merchant names
        val merchantGroups = groupAdjacentMerchants(merchantTokenRanges, processed)

        // Step 4: replace merchant groups with placeholders
        val result = StringBuilder(processed)
        // Process in reverse order to maintain correct indices
        for ((range, merchantName) in merchantGroups.sortedByDescending { it.first.first }) {
            val placeholder = merchantMap.getOrPut(merchantName) {
                "MERCHANT_${nextIndex()}"
            }
            result.replace(range.first, range.last + 1, placeholder)
        }

        return result.toString()
    }

    private fun replaceUnprotected(
        text: String,
        pattern: Regex,
        protectedRanges: List<IntRange>,
    ): String {
        val result = StringBuilder(text)
        var offset = 0
        for (match in pattern.findAll(text)) {
            val overlaps = protectedRanges.any { pRange ->
                match.range.first <= pRange.last && match.range.last >= pRange.first
            }
            if (!overlaps) {
                val start = match.range.first + offset
                val end = match.range.last + 1 + offset
                result.replace(start, end, "")
                offset -= match.value.length
            }
        }
        return result.toString()
    }

    private fun isProtected(range: IntRange, protectedRanges: List<IntRange>): Boolean {
        return protectedRanges.any { protected ->
            range.first >= protected.first && range.last <= protected.last
        }
    }

    private fun isNumericOnly(value: String): Boolean {
        return value.all { it.isDigit() || it == ' ' || it == ',' || it == '.' }
    }

    private fun groupAdjacentMerchants(
        tokens: List<Pair<IntRange, String>>,
        line: String,
    ): List<Pair<IntRange, String>> {
        if (tokens.isEmpty()) return emptyList()

        val groups = mutableListOf<Pair<IntRange, String>>()
        var currentStart = tokens[0].first.first
        var currentEnd = tokens[0].first.last
        val nameBuilder = StringBuilder(tokens[0].second)

        for (i in 1 until tokens.size) {
            val (range, value) = tokens[i]
            // Check bounds: tokens may be adjacent or overlapping after processing
            if (currentEnd + 1 < range.first) {
                val gap = line.substring(currentEnd + 1, range.first)
                // Adjacent if only whitespace or punctuation like quotes between them
                if (gap.all { it.isWhitespace() || it == '"' || it == '«' || it == '»' || it == '\'' } &&
                    gap.length <= 3
                ) {
                    currentEnd = range.last
                    nameBuilder.append(" ").append(value)
                } else {
                    groups += IntRange(currentStart, currentEnd) to nameBuilder.toString()
                    currentStart = range.first
                    currentEnd = range.last
                    nameBuilder.clear().append(value)
                }
            } else {
                // Overlapping or immediately adjacent — merge into the current group
                currentEnd = maxOf(currentEnd, range.last)
                nameBuilder.append(" ").append(value)
            }
        }
        groups += IntRange(currentStart, currentEnd) to nameBuilder.toString()

        return groups
    }

    companion object {
        // Dates: dd.MM.yyyy, dd/MM/yyyy, dd.MM.yy, dd/MM/yy
        private val DATE_PATTERN =
            Regex("""\d{1,2}[./]\d{1,2}[./]\d{2,4}""")

        // Amounts: digits with optional thousand separators and decimal part
        private val AMOUNT_PATTERN =
            Regex("""\d{1,3}(?:[ \u00A0]\d{3})*[,.]?\d*""")

        // ISO currency codes
        private val CURRENCY_PATTERN =
            Regex("""\b[A-Z]{3}\b""")

        // Currency symbols
        private val CURRENCY_SYMBOL_PATTERN =
            Regex("""[₸$€£¥₽]""")

        // Sign indicators before amounts
        private val SIGN_PATTERN =
            Regex("""(?<=\s)[+\-](?=\s)""")

        // Common operation type words (Russian banking terms)
        private val OPERATION_PATTERN = Regex(
            """(?iu)\b(?:Покупка|Пополнение|Перевод|Снятие|Зачисление|Списание|Возврат|Оплата|Purchase|Transfer|Withdrawal|Deposit|Refund|Payment)\b""",
        )

        // Card masks: ****1234
        private val CARD_MASK_PATTERN =
            Regex("""\*{2,}\d{2,6}""")

        // IBAN-like: KZ followed by digits
        private val IBAN_PATTERN =
            Regex("""\bKZ\d{5,}\b""")

        // Long digit sequences (4+ digits) that aren't dates or amounts
        private val LONG_DIGIT_PATTERN =
            Regex("""\b\d{4,}\b""")

        // Merchant candidate: word tokens (Cyrillic, Latin, quotes, dots)
        private val MERCHANT_CANDIDATE_PATTERN =
            Regex("""[A-Za-zА-ЯЁа-яёІіҢңҮүҰұҚқӘәӨө"«»'.][A-Za-zА-ЯЁа-яёІіҢңҮүҰұҚқӘәӨө"«»'.\-]*""")
    }
}
