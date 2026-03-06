package com.atelbay.money_manager.domain.exchangerate.repository

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/**
 * Contract for loading and storing multi-currency exchange-rate quotes.
 *
 * Implementations are expected to use remote-first strategy with
 * a local cache fallback when the network is unavailable.
 */
interface ExchangeRateRepository {

    /**
     * Observes the locally cached quote set. Emits null if no quotes exist yet.
     */
    fun observeQuotes(): Flow<ExchangeRate?>

    /**
     * Persists the provided quote set into the local cache.
     */
    suspend fun saveQuotes(rate: ExchangeRate)

    /**
     * Fetches the latest quotes from the remote source (NBK) and persists them locally.
     *
     * @return The freshly fetched [ExchangeRate].
     * @throws Exception if the network request fails and no cached quotes are available.
     */
    suspend fun fetchAndStoreQuotes(): ExchangeRate
}
