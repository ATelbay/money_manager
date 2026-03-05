package com.atelbay.money_manager.domain.exchangerate.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertAmountUseCaseTest {

    private val useCase = ConvertAmountUseCase()

    @Test
    fun `convert KZT to USD uses provided rate`() {
        val converted = useCase(
            amount = 50_000.0,
            sourceCurrency = "KZT",
            targetCurrency = "USD",
            quotes = mapOf("USD" to 475.0),
        )

        assertEquals(105.26, converted, 0.0)
    }

    @Test
    fun `convert USD to KZT uses provided rate`() {
        val converted = useCase(
            amount = 100.0,
            sourceCurrency = "USD",
            targetCurrency = "KZT",
            quotes = mapOf("USD" to 475.0),
        )

        assertEquals(47_500.0, converted, 0.0)
    }

    @Test
    fun `rounds converted amount with HALF_UP strategy`() {
        val converted = useCase(
            amount = 2.345,
            sourceCurrency = "USD",
            targetCurrency = "KZT",
            quotes = mapOf("USD" to 1.0),
        )

        assertEquals(2.35, converted, 0.0)
    }

    @Test
    fun `same currency passthrough returns amount unchanged`() {
        val converted = useCase(
            amount = 123.456,
            sourceCurrency = "USD",
            targetCurrency = "USD",
            quotes = emptyMap(),
        )

        assertEquals(123.456, converted, 0.0)
    }
}
