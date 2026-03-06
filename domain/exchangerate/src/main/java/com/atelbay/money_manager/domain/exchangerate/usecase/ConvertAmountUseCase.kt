package com.atelbay.money_manager.domain.exchangerate.usecase

import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Converts a monetary amount between any two supported currencies using
 * KZT-per-unit rates from a snapshot.
 */
class ConvertAmountUseCase @Inject constructor() {

    operator fun invoke(
        amount: Double,
        sourceCurrency: String,
        targetCurrency: String,
        snapshot: ExchangeRateSnapshot,
    ): Double? {
        val normalizedSourceCurrency = sourceCurrency.trim().uppercase()
        val normalizedTargetCurrency = targetCurrency.trim().uppercase()

        if (normalizedSourceCurrency.isBlank() || normalizedTargetCurrency.isBlank()) {
            return null
        }

        if (normalizedSourceCurrency == normalizedTargetCurrency) {
            return round(amount)
        }

        val sourceRate = snapshot.rateFor(normalizedSourceCurrency)?.kztPerUnit
        val targetRate = snapshot.rateFor(normalizedTargetCurrency)?.kztPerUnit
        if (sourceRate == null || targetRate == null || sourceRate <= 0.0 || targetRate <= 0.0) {
            return null
        }

        val amountInKzt = BigDecimal(amount.toString())
            .multiply(BigDecimal(sourceRate.toString()))
        return amountInKzt
            .divide(BigDecimal(targetRate.toString()), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    private fun round(amount: Double): Double =
        BigDecimal(amount.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
}
