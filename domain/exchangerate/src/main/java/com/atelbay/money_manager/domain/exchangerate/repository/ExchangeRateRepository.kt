package com.atelbay.money_manager.domain.exchangerate.repository

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Contract for loading and storing multi-currency exchange-rate snapshots.
 *
 * Implementations are expected to use a remote-first strategy with
 * a local cache fallback when the network is unavailable.
 */
interface ExchangeRateRepository {

    /**
     * Observes the locally cached snapshot. Emits null if no cached snapshot exists yet.
     */
    fun observeRates(): Flow<ExchangeRateSnapshot?>

    /**
     * Persists the provided snapshot into the local cache.
     */
    suspend fun saveRates(snapshot: ExchangeRateSnapshot)

    /**
     * Fetches the latest snapshot from the remote source (NBK) and persists it locally.
     *
     * @return The freshly fetched [ExchangeRateSnapshot].
     * @throws Exception if the network request fails and no cached snapshot is available.
     */
    suspend fun fetchAndStoreRates(): ExchangeRateSnapshot
}
