package com.atelbay.money_manager.data.exchangerate.mapper

import com.atelbay.money_manager.core.datastore.StoredCurrencyRate
import com.atelbay.money_manager.core.datastore.StoredExchangeRateSnapshot
import com.atelbay.money_manager.data.exchangerate.model.ExchangeRateCacheModel
import com.atelbay.money_manager.data.exchangerate.model.NbkExchangeRateRemoteModel
import com.atelbay.money_manager.domain.exchangerate.model.CurrencyRate
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot

internal fun StoredExchangeRateSnapshot.toCacheModel(): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        fetchedAt = fetchedAt,
        source = source,
        rates = rates.mapValues { (_, value) -> value.kztPerUnit },
    )

internal fun ExchangeRateCacheModel.toDomain(): ExchangeRateSnapshot =
    ExchangeRateSnapshot(
        fetchedAt = fetchedAt,
        source = source,
        rates = rates.mapValues { (code, kztPerUnit) ->
            CurrencyRate(
                code = code,
                kztPerUnit = kztPerUnit,
            )
        },
    )

internal fun ExchangeRateSnapshot.toCacheModel(): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        fetchedAt = fetchedAt,
        source = source,
        rates = rates.mapValues { (_, value) -> value.kztPerUnit },
    )

internal fun NbkExchangeRateRemoteModel.toCacheModel(
    fetchedAt: Long,
): ExchangeRateCacheModel =
    ExchangeRateCacheModel(
        fetchedAt = fetchedAt,
        source = source,
        rates = rates,
    )

internal fun ExchangeRateCacheModel.toStoredSnapshot(): StoredExchangeRateSnapshot =
    StoredExchangeRateSnapshot(
        fetchedAt = fetchedAt,
        source = source,
        rates = rates.mapValues { (code, kztPerUnit) ->
            StoredCurrencyRate(
                code = code,
                kztPerUnit = kztPerUnit,
            )
        },
    )
