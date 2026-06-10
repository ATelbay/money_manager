package com.atelbay.money_manager.domain.exchangerate.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Converts a monetary amount between any two currencies using KZT as the pivot.
 *
 * [quotes] maps each currency code to its KZT-per-1-unit rate (e.g. "USD" → 475.0).
 * Snapshots normally include KZT with a rate of 1.0, but the use case still treats it as
 * an implicit pivot so legacy inputs without an explicit KZT entry remain valid.
 *
 * Conversion path: source → KZT → target.
 * Same-currency passthrough returns [amountMinor] unchanged (no rounding applied).
 *
 * All amounts are [Long] minor units (hundredths). Rounding strategy: HALF_UP at the minor-unit
 * boundary; intermediate division uses a generous scale to avoid premature precision loss.
 *
 * @throws IllegalArgumentException if a required quote is missing or non-positive.
 */
class ConvertAmountUseCase @Inject constructor() {

    /**
     * @param amountMinor    The source amount to convert, in minor units (hundredths).
     * @param sourceCurrency ISO currency code of the source amount.
     * @param targetCurrency ISO currency code of the desired result.
     * @param quotes         Currency code → KZT per 1 unit.
     * @return Converted amount in minor units, rounded HALF_UP.
     */
    operator fun invoke(
        amountMinor: Long,
        sourceCurrency: String,
        targetCurrency: String,
        quotes: Map<String, Double>,
    ): Long {
        if (sourceCurrency == targetCurrency) return amountMinor

        val sourceToKzt = kztRate(sourceCurrency, quotes)
        val targetToKzt = kztRate(targetCurrency, quotes)

        // amountMinor → major (exact) → ×sourceRate ÷ targetRate → back to minor.
        val major = BigDecimal(amountMinor).movePointLeft(SCALE)
        val sourceRate = BigDecimal(sourceToKzt.toString())
        val targetRate = BigDecimal(targetToKzt.toString())

        val resultMajor = major.multiply(sourceRate)
            .divide(targetRate, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)

        return resultMajor.movePointRight(SCALE).setScale(0, RoundingMode.HALF_UP).toLong()
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
        const val INTERMEDIATE_SCALE = 10
    }
}
