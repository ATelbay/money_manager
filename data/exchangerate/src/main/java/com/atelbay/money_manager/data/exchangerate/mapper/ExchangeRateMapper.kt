package com.atelbay.money_manager.data.exchangerate.mapper

import com.atelbay.money_manager.core.datastore.StoredExchangeRate
import com.atelbay.money_manager.data.exchangerate.model.ExchangeRateCacheModel
import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate

/**
 * Bridges [StoredExchangeRate] to [ExchangeRateCacheModel].
 * Uses full quotes map when available; falls back to legacy single USD value.
 */
internal fun StoredExchangeRate.toCacheModel(): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        quotes = quotes?.withKzt() ?: mapOf("KZT" to 1.0, "USD" to usdToKzt),
        fetchedAt = fetchedAt,
        source = source,
    )

internal fun ExchangeRateCacheModel.toDomain(): ExchangeRate =
    ExchangeRate(
        quotes = quotes.withKzt(),
        fetchedAt = fetchedAt,
        source = source,
    )

internal fun ExchangeRate.toCacheModel(
    source: String? = null,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        quotes = quotes.withKzt(),
        fetchedAt = fetchedAt,
        source = source ?: this.source,
    )

internal fun NbkExchangeRateRemoteModel.toCacheModel(
    fetchedAt: Long,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        quotes = quotes.withKzt(),
        fetchedAt = fetchedAt,
        source = source,
    )

private fun Map<String, Double>.withKzt(): Map<String, Double> =
    if (containsKey("KZT")) this else this + ("KZT" to 1.0)
