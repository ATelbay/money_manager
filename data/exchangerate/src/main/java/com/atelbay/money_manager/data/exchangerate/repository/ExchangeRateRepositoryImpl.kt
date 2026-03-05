package com.atelbay.money_manager.data.exchangerate.repository

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.data.exchangerate.mapper.toCacheModel
import com.atelbay.money_manager.data.exchangerate.mapper.toDomain
import com.atelbay.money_manager.data.exchangerate.remote.NbkExchangeRateRemoteDataSource
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepositoryImpl @Inject constructor(
    private val remoteDataSource: NbkExchangeRateRemoteDataSource,
    private val userPreferences: UserPreferences,
) : ExchangeRateRepository {

    override fun observeQuotes(): Flow<ExchangeRate?> =
        userPreferences.exchangeRate.map { storedRate ->
            storedRate?.toCacheModel()?.toDomain()
        }

    override suspend fun saveQuotes(rate: ExchangeRate) {
        val cacheModel = rate.toCacheModel(source = SOURCE_NBK)
        // Legacy storage: extract USD quote for the single-value preference key.
        // US-002 will migrate storage to persist the full quotes map.
        val usdRate = cacheModel.quotes[USD] ?: return
        userPreferences.setExchangeRate(
            usdToKzt = usdRate,
            fetchedAt = cacheModel.fetchedAt,
            source = cacheModel.source,
        )
    }

    override suspend fun fetchAndStoreQuotes(): ExchangeRate {
        return try {
            val cacheModel = remoteDataSource.fetchQuotes()
                .toCacheModel(fetchedAt = System.currentTimeMillis())

            val usdRate = cacheModel.quotes[USD]
            if (usdRate != null) {
                userPreferences.setExchangeRate(
                    usdToKzt = usdRate,
                    fetchedAt = cacheModel.fetchedAt,
                    source = cacheModel.source,
                )
            }

            cacheModel.toDomain()
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }

            userPreferences.getExchangeRate()
                ?.toCacheModel()
                ?.toDomain()
                ?: throw error
        }
    }

    private companion object {
        const val SOURCE_NBK = "NBK"
        const val USD = "USD"
    }
}
