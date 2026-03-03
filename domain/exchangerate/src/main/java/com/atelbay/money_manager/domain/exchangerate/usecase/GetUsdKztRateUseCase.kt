package com.atelbay.money_manager.domain.exchangerate.usecase

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRate
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns an observable stream of the cached USD-KZT exchange rate.
 * Emits null if no rate has been fetched yet.
 */
class GetUsdKztRateUseCase @Inject constructor(
    private val repository: ExchangeRateRepository,
) {
    operator fun invoke(): Flow<ExchangeRate?> = repository.observeRate()
}
