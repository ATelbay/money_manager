package com.atelbay.money_manager.domain.exchangerate.usecase

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import com.atelbay.money_manager.domain.exchangerate.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExchangeRateSnapshotUseCase @Inject constructor(
    private val repository: ExchangeRateRepository,
) {
    operator fun invoke(): Flow<ExchangeRateSnapshot?> = repository.observeRates()
}
