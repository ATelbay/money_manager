package com.atelbay.money_manager.data.exchangerate.mapper

import com.atelbay.money_manager.core.datastore.StoredExchangeRate
import com.atelbay.money_manager.data.exchangerate.model.ExchangeRateCacheModel
import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate

/**
 * Bridges legacy [StoredExchangeRate] (single usdToKzt value) to the
 * multi-currency [ExchangeRateCacheModel]. US-002 will migrate storage
 * to persist the full quotes map.
 */
internal fun StoredExchangeRate.toCacheModel(): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        quotes = mapOf("USD" to usdToKzt),
        fetchedAt = fetchedAt,
        source = source,
    )

internal fun ExchangeRateCacheModel.toDomain(): ExchangeRate =
    ExchangeRate(
        quotes = quotes,
        fetchedAt = fetchedAt,
    )

internal fun ExchangeRate.toCacheModel(
    source: String? = null,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        quotes = quotes,
        fetchedAt = fetchedAt,
        source = source,
    )

internal fun NbkExchangeRateRemoteModel.toCacheModel(
    fetchedAt: Long,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        quotes = quotes,
        fetchedAt = fetchedAt,
        source = source,
    )
