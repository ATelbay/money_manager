package com.atelbay.money_manager.core.ui.util

import java.util.Locale

data class SupportedCurrencyDisplay(
    val symbol: String,
    val isAmbiguous: Boolean,
)

object SupportedCurrencyDisplayRegistry {
    private val supportedCurrencyDisplays = mapOf(
        "AUD" to SupportedCurrencyDisplay(symbol = "$", isAmbiguous = true),
        "CAD" to SupportedCurrencyDisplay(symbol = "$", isAmbiguous = true),
        "CHF" to SupportedCurrencyDisplay(symbol = "CHF", isAmbiguous = true),
        "CNY" to SupportedCurrencyDisplay(symbol = "¥", isAmbiguous = true),
        "EUR" to SupportedCurrencyDisplay(symbol = "€", isAmbiguous = false),
        "GBP" to SupportedCurrencyDisplay(symbol = "£", isAmbiguous = true),
        "JPY" to SupportedCurrencyDisplay(symbol = "¥", isAmbiguous = true),
        "KGS" to SupportedCurrencyDisplay(symbol = "сом", isAmbiguous = true),
        "KZT" to SupportedCurrencyDisplay(symbol = "₸", isAmbiguous = false),
        "NOK" to SupportedCurrencyDisplay(symbol = "kr", isAmbiguous = true),
        "RUB" to SupportedCurrencyDisplay(symbol = "₽", isAmbiguous = false),
        "SEK" to SupportedCurrencyDisplay(symbol = "kr", isAmbiguous = true),
        "TRY" to SupportedCurrencyDisplay(symbol = "₺", isAmbiguous = false),
        "UAH" to SupportedCurrencyDisplay(symbol = "₴", isAmbiguous = false),
        "USD" to SupportedCurrencyDisplay(symbol = "$", isAmbiguous = true),
        "UZS" to SupportedCurrencyDisplay(symbol = "so'm", isAmbiguous = true),
    )

    fun lookup(currencyCode: String): SupportedCurrencyDisplay? =
        supportedCurrencyDisplays[currencyCode.normalizeCurrencyCode(fallback = currencyCode)]
}

fun String.normalizeCurrencyCode(fallback: String): String =
    trim().uppercase(Locale.ROOT).ifBlank { fallback }
