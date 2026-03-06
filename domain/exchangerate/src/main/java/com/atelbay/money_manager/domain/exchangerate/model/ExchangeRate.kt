package com.atelbay.money_manager.domain.exchangerate.model

/**
 * Represents a set of cached exchange-rate quotes from National Bank of Kazakhstan.
 *
 * Each entry in [quotes] maps a currency code (e.g. "USD", "EUR") to the amount
 * of KZT per 1 unit of that currency. KZT itself is the implicit pivot and is
 * not stored in the map (its rate is always 1.0).
 *
 * @param quotes Currency code → KZT per 1 unit. Example: {"USD": 475.0, "EUR": 520.0}.
 * @param fetchedAt Epoch millis when the quotes were fetched from NBK.
 * @param source Source provider label for the cached quote snapshot.
 */
data class ExchangeRate(
    val quotes: Map<String, Double>,
    val fetchedAt: Long,
    val source: String? = null,
)
