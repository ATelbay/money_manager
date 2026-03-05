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

    @Test
    fun `inverse pair conversion via KZT pivot`() {
        // EUR → USD: amount * (EUR/KZT) / (USD/KZT)
        // 100 EUR * 520 / 475 = 109.47 USD
        val converted = useCase(
            amount = 100.0,
            sourceCurrency = "EUR",
            targetCurrency = "USD",
            quotes = mapOf("EUR" to 520.0, "USD" to 475.0),
        )

        assertEquals(109.47, converted, 0.0)
    }

    @Test
    fun `nominal-based currency conversion uses normalized per-unit rate`() {
        // JPY → USD: 10000 JPY * 0.3357 / 475 = 7.07 USD
        // (0.3357 KZT per 1 JPY is already normalized from 100 JPY = 33.57 KZT)
        val converted = useCase(
            amount = 10_000.0,
            sourceCurrency = "JPY",
            targetCurrency = "USD",
            quotes = mapOf("JPY" to 0.3357, "USD" to 475.0),
        )

        assertEquals(7.07, converted, 0.0)
    }

    @Test
    fun `KZT as source uses implicit rate of 1`() {
        // KZT → EUR: 52_000 KZT * 1.0 / 520 = 100 EUR
        val converted = useCase(
            amount = 52_000.0,
            sourceCurrency = "KZT",
            targetCurrency = "EUR",
            quotes = mapOf("EUR" to 520.0),
        )

        assertEquals(100.0, converted, 0.0)
    }

    @Test
    fun `KZT as target uses implicit rate of 1`() {
        // EUR → KZT: 100 EUR * 520 / 1.0 = 52_000 KZT
        val converted = useCase(
            amount = 100.0,
            sourceCurrency = "EUR",
            targetCurrency = "KZT",
            quotes = mapOf("EUR" to 520.0),
        )

        assertEquals(52_000.0, converted, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing source quote throws IllegalArgumentException`() {
        useCase(
            amount = 100.0,
            sourceCurrency = "GBP",
            targetCurrency = "USD",
            quotes = mapOf("USD" to 475.0),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing target quote throws IllegalArgumentException`() {
        useCase(
            amount = 100.0,
            sourceCurrency = "USD",
            targetCurrency = "GBP",
            quotes = mapOf("USD" to 475.0),
        )
    }
}
