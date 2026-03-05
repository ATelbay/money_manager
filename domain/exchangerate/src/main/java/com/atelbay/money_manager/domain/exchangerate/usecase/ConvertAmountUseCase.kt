package com.atelbay.money_manager.domain.exchangerate.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Converts a monetary amount between any two currencies using KZT as the pivot.
 *
 * [quotes] maps each currency code to its KZT-per-1-unit rate (e.g. "USD" → 475.0).
 * KZT is the implicit pivot with a rate of 1.0 and does not need to appear in the map.
 *
 * Conversion path: source → KZT → target.
 * Same-currency passthrough returns [amount] unchanged (no rounding applied).
 *
 * Rounding strategy: HALF_UP, scaled to 2 decimal places (deterministic).
 *
 * @throws IllegalArgumentException if a required quote is missing or non-positive.
 */
class ConvertAmountUseCase @Inject constructor() {

    /**
     * @param amount         The source amount to convert.
     * @param sourceCurrency ISO currency code of the source amount.
     * @param targetCurrency ISO currency code of the desired result.
     * @param quotes         Currency code → KZT per 1 unit.
     * @return Converted amount rounded to 2 decimal places using HALF_UP.
     */
    operator fun invoke(
        amount: Double,
        sourceCurrency: String,
        targetCurrency: String,
        quotes: Map<String, Double>,
    ): Double {
        if (sourceCurrency == targetCurrency) return amount

        val sourceToKzt = kztRate(sourceCurrency, quotes)
        val targetToKzt = kztRate(targetCurrency, quotes)

        val bd = BigDecimal(amount.toString())
        val sourceRate = BigDecimal(sourceToKzt.toString())
        val targetRate = BigDecimal(targetToKzt.toString())

        return bd.multiply(sourceRate)
            .divide(targetRate, SCALE, RoundingMode.HALF_UP)
            .toDouble()
    }

    private fun kztRate(currency: String, quotes: Map<String, Double>): Double {
        if (currency == KZT) return 1.0
        val rate = quotes[currency]
            ?: throw IllegalArgumentException("No quote available for $currency")
        require(rate > 0.0) { "Quote for $currency must be positive, got $rate" }
        return rate
    }

    private companion object {
        const val KZT = "KZT"
        const val SCALE = 2
    }
}
