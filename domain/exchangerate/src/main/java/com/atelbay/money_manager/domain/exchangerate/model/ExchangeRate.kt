package com.atelbay.money_manager.domain.exchangerate.model

/**
 * Represents a cached USD→KZT exchange rate from National Bank of Kazakhstan.
 *
 * @param usdToKzt Amount of KZT per 1 USD.
 * @param fetchedAt Epoch millis when the rate was fetched from NBK.
 */
data class ExchangeRate(
    val usdToKzt: Double,
    val fetchedAt: Long,
)
