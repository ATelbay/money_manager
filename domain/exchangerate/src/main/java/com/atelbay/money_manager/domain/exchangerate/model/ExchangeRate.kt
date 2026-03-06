package com.atelbay.money_manager.domain.exchangerate.model

/**
 * Represents a full exchange-rate snapshot normalized to KZT-per-unit values.
 *
 * @param fetchedAt Epoch millis when the snapshot was fetched from NBK.
 * @param source Source label for the snapshot.
 * @param rates Map keyed by ISO code.
 */
data class ExchangeRateSnapshot(
    val fetchedAt: Long,
    val source: String?,
    val rates: Map<String, CurrencyRate>,
) {
    fun rateFor(code: String): CurrencyRate? = rates[code.trim().uppercase()]
}

data class CurrencyRate(
    val code: String,
    val kztPerUnit: Double,
)
