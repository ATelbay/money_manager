package com.atelbay.money_manager.domain.exchangerate.usecase

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns an observable stream of the cached exchange-rate quotes.
 * Emits null if no quotes have been fetched yet.
 */
class ObserveExchangeRateUseCase @Inject constructor(
    private val repository: ExchangeRateRepository,
) {
    operator fun invoke(): Flow<ExchangeRate?> = repository.observeQuotes()
}
