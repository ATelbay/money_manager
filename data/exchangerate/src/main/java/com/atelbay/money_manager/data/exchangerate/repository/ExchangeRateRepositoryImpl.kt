package com.atelbay.money_manager.data.exchangerate.repository

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.data.exchangerate.mapper.toCacheModel
import com.atelbay.money_manager.data.exchangerate.mapper.toDomain
import com.atelbay.money_manager.data.exchangerate.mapper.toStoredSnapshot
import com.atelbay.money_manager.data.exchangerate.remote.NbkExchangeRateRemoteDataSource
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
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

    override fun observeRates(): Flow<ExchangeRateSnapshot?> =
        userPreferences.exchangeRateSnapshot.map { storedRate ->
            storedRate?.toCacheModel()?.toDomain()
        }

    override suspend fun saveRates(snapshot: ExchangeRateSnapshot) {
        userPreferences.setExchangeRateSnapshot(snapshot.toCacheModel().toStoredSnapshot())
    }

    override suspend fun fetchAndStoreRates(): ExchangeRateSnapshot {
        return try {
            val cacheModel = remoteDataSource.fetchRates()
                .toCacheModel(fetchedAt = System.currentTimeMillis())

            userPreferences.setExchangeRateSnapshot(cacheModel.toStoredSnapshot())

            cacheModel.toDomain()
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }

            userPreferences.getExchangeRateSnapshot()
                ?.toCacheModel()
                ?.toDomain()
                ?: throw error
        }
    }
}
