package com.atelbay.money_manager.core.parser

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SPACE_DOT_PATTERN = Regex("-?\\d{1,3}( \\d{3})*\\.\\d+")
private val SPACE_DOT_INTEGER_PATTERN = Regex("-?\\d{1,3}( \\d{3})+")
private val SPACE_DOT_PLAIN_INTEGER_PATTERN = Regex("-?\\d+")

object AmountParser {

    fun parseAmount(amountStr: String, format: String): Double = when (format) {
        "dot" -> amountStr.replace(Regex("[^\\d.\\-]"), "").toDouble()
        "comma_dot" -> amountStr.replace(Regex("[^\\d,.\\-]"), "").replace(",", "").toDouble()
        "space_comma" -> amountStr.replace(Regex("[^\\d\\s.,\\-]"), "").replace("\\s".toRegex(), "").replace(",", ".").toDouble()
        "space_dot" -> {
            // Normalize non-breaking spaces before matching
            val normalized = amountStr.replace('\u00A0', ' ')
            // Precise grouped-thousands decimal regex — avoids matching partial unrelated numbers.
            // When multiple matches exist (merged cells), pick the largest absolute value.
            val decimalMatches = SPACE_DOT_PATTERN.findAll(normalized).toList()
            if (decimalMatches.isNotEmpty()) {
                val best = decimalMatches.maxByOrNull { kotlin.math.abs(it.value.replace(" ", "").toDouble()) }!!
                return best.value.replace(" ", "").toDouble()
            }
            // Integer-only: "5 000" or plain "5000"
            val intMatch = SPACE_DOT_INTEGER_PATTERN.find(normalized)
                ?: SPACE_DOT_PLAIN_INTEGER_PATTERN.find(normalized)
            if (intMatch != null) {
                return intMatch.value.replace(" ", "").toDouble()
            }
            normalized.replace(" ", "").toDouble()
        }
        else -> throw IllegalArgumentException("Unknown amount format: $format")
    }

    fun parseDateString(dateStr: String, dateFormat: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        return LocalDate.parse(dateStr.trim(), formatter)
    }
}
