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
        quotes = quotes ?: mapOf("USD" to usdToKzt),
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
