package com.atelbay.money_manager.core.parser

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AmountParser {

    fun parseAmount(amountStr: String, format: String): Double = when (format) {
        "dot" -> amountStr.replace(Regex("[^\\d.\\-]"), "").toDouble()
        "comma_dot" -> amountStr.replace(Regex("[^\\d,.\\-]"), "").replace(",", "").toDouble()
        "space_comma" -> amountStr.replace(Regex("[^\\d\\s.,\\-]"), "").replace("\\s".toRegex(), "").replace(",", ".").toDouble()
        else -> throw IllegalArgumentException("Unknown amount format: $format")
    }

    fun parseDateString(dateStr: String, dateFormat: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        return LocalDate.parse(dateStr.trim(), formatter)
    }
}
