package com.atelbay.money_manager.domain.exchangerate.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertAmountUseCaseTest {

    private val useCase = ConvertAmountUseCase()

    @Test
    fun `convert KZT to USD uses provided rate`() {
        val converted = useCase(
            amount = 50_000.0,
            rate = 475.0,
            direction = ConversionDirection.KZT_TO_USD,
        )

        assertEquals(105.26, converted, 0.0)
    }

    @Test
    fun `convert USD to KZT uses provided rate`() {
        val converted = useCase(
            amount = 100.0,
            rate = 475.0,
            direction = ConversionDirection.USD_TO_KZT,
        )

        assertEquals(47_500.0, converted, 0.0)
    }

    @Test
    fun `rounds converted amount with HALF_UP strategy`() {
        val converted = useCase(
            amount = 2.345,
            rate = 1.0,
            direction = ConversionDirection.USD_TO_KZT,
        )

        assertEquals(2.35, converted, 0.0)
    }
}
