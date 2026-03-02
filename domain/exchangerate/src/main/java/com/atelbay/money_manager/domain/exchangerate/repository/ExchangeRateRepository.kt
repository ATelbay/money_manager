package com.atelbay.money_manager.domain.exchangerate.repository

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/**
 * Contract for loading and storing the USD-KZT exchange rate.
 *
 * Implementations are expected to use remote-first strategy with
 * a local cache fallback when the network is unavailable.
 */
interface ExchangeRateRepository {

    /**
     * Observes the locally cached rate. Emits null if no cached rate exists yet.
     */
    fun observeRate(): Flow<ExchangeRate?>

    /**
     * Persists the provided rate into the local cache.
     */
    suspend fun saveRate(rate: ExchangeRate)

    /**
     * Fetches the latest rate from the remote source (NBK) and persists it locally.
     *
     * @return The freshly fetched [ExchangeRate].
     * @throws Exception if the network request fails and no cached rate is available.
     */
    suspend fun fetchAndStoreRate(): ExchangeRate
}
