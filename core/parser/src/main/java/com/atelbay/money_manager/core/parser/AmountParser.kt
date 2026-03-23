package com.atelbay.money_manager.core.parser

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AmountParser {

    fun parseAmount(amountStr: String, format: String): Double = when (format) {
        "dot" -> amountStr.replace(Regex("[^\\d.\\-]"), "").toDouble()
        "comma_dot" -> amountStr.replace(",", "").toDouble()
        else -> amountStr.replace("\\s".toRegex(), "").replace(",", ".").toDouble()
    }

    fun parseDateString(dateStr: String, dateFormat: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        return LocalDate.parse(dateStr.trim(), formatter)
    }
}
