package com.atelbay.money_manager.domain.exchangerate.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Converts a monetary amount between KZT and USD using a provided USD-KZT rate.
 *
 * Rounding strategy: HALF_UP, scaled to 2 decimal places (deterministic).
 *
 * Usage:
 * ```
 * val usd = convertAmountUseCase(50_000.0, rate = 475.0, ConversionDirection.KZT_TO_USD) // ≈ 105.26
 * val kzt = convertAmountUseCase(100.0, rate = 475.0, ConversionDirection.USD_TO_KZT)     // 47 500.00
 * ```
 */
class ConvertAmountUseCase @Inject constructor() {

    /**
     * @param amount   The source amount to convert.
     * @param rate     USD-KZT rate (how many KZT per 1 USD).
     * @param direction The direction of conversion.
     * @return Converted amount rounded to 2 decimal places using HALF_UP.
     */
    operator fun invoke(
        amount: Double,
        rate: Double,
        direction: ConversionDirection,
    ): Double {
        require(rate > 0.0) { "USD-KZT rate must be greater than zero." }

        val bd = BigDecimal(amount.toString())
        val rateBd = BigDecimal(rate.toString())
        return when (direction) {
            ConversionDirection.KZT_TO_USD ->
                bd.divide(rateBd, 2, RoundingMode.HALF_UP).toDouble()

            ConversionDirection.USD_TO_KZT ->
                bd.multiply(rateBd).setScale(2, RoundingMode.HALF_UP).toDouble()
        }
    }
}

enum class ConversionDirection {
    KZT_TO_USD,
    USD_TO_KZT,
}
