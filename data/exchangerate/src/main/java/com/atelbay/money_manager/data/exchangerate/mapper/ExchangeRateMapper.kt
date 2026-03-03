package com.atelbay.money_manager.data.exchangerate.mapper

import com.atelbay.money_manager.core.datastore.StoredExchangeRate
import com.atelbay.money_manager.data.exchangerate.model.ExchangeRateCacheModel
import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate

internal fun StoredExchangeRate.toCacheModel(): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        usdToKzt = usdToKzt,
        fetchedAt = fetchedAt,
        source = source,
    )

internal fun ExchangeRateCacheModel.toDomain(): ExchangeRate =
    ExchangeRate(
        usdToKzt = usdToKzt,
        fetchedAt = fetchedAt,
    )

internal fun ExchangeRate.toCacheModel(
    source: String? = null,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        usdToKzt = usdToKzt,
        fetchedAt = fetchedAt,
        source = source,
    )

internal fun NbkExchangeRateRemoteModel.toCacheModel(
    fetchedAt: Long,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        usdToKzt = usdToKzt,
        fetchedAt = fetchedAt,
        source = source,
    )
