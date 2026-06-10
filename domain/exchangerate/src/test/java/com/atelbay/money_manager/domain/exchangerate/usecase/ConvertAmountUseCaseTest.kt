package com.atelbay.money_manager.domain.exchangerate.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertAmountUseCaseTest {

    private val useCase = ConvertAmountUseCase()

    @Test
    fun `convert KZT to USD uses provided rate`() {
        // 50_000.00 KZT / 475 = 105.2631… → 105.26 → 10_526 minor
        val converted = useCase(
            amountMinor = 5_000_000L,
            sourceCurrency = "KZT",
            targetCurrency = "USD",
            quotes = mapOf("KZT" to 1.0, "USD" to 475.0),
        )

        assertEquals(10_526L, converted)
    }

    @Test
    fun `convert USD to KZT uses provided rate`() {
        // 100.00 USD * 475 = 47_500.00 KZT → 4_750_000 minor
        val converted = useCase(
            amountMinor = 10_000L,
            sourceCurrency = "USD",
            targetCurrency = "KZT",
            quotes = mapOf("KZT" to 1.0, "USD" to 475.0),
        )

        assertEquals(4_750_000L, converted)
    }

    @Test
    fun `rounds converted amount with HALF_UP strategy`() {
        // 0.01 USD * 1 / 2 = 0.005 major = 0.5 minor → HALF_UP → 1 minor
        val converted = useCase(
            amountMinor = 1L,
            sourceCurrency = "USD",
            targetCurrency = "EUR",
            quotes = mapOf("USD" to 1.0, "EUR" to 2.0),
        )

        assertEquals(1L, converted)
    }

    @Test
    fun `same currency passthrough returns amount unchanged`() {
        val converted = useCase(
            amountMinor = 12_345L,
            sourceCurrency = "USD",
            targetCurrency = "USD",
            quotes = emptyMap(),
        )

        assertEquals(12_345L, converted)
    }

    @Test
    fun `inverse pair conversion via KZT pivot`() {
        // 100.00 EUR * 520 / 475 = 109.47 USD → 10_947 minor
        val converted = useCase(
            amountMinor = 10_000L,
            sourceCurrency = "EUR",
            targetCurrency = "USD",
            quotes = mapOf("KZT" to 1.0, "EUR" to 520.0, "USD" to 475.0),
        )

        assertEquals(10_947L, converted)
    }

    @Test
    fun `nominal-based currency conversion uses normalized per-unit rate`() {
        // 10_000.00 JPY * 0.3357 / 475 = 7.0673… → 7.07 USD → 707 minor
        val converted = useCase(
            amountMinor = 1_000_000L,
            sourceCurrency = "JPY",
            targetCurrency = "USD",
            quotes = mapOf("KZT" to 1.0, "JPY" to 0.3357, "USD" to 475.0),
        )

        assertEquals(707L, converted)
    }

    @Test
    fun `KZT as source uses implicit rate of 1`() {
        // 52_000.00 KZT * 1 / 520 = 100.00 EUR → 10_000 minor
        val converted = useCase(
            amountMinor = 5_200_000L,
            sourceCurrency = "KZT",
            targetCurrency = "EUR",
            quotes = mapOf("EUR" to 520.0),
        )

        assertEquals(10_000L, converted)
    }

    @Test
    fun `KZT as target uses implicit rate of 1`() {
        // 100.00 EUR * 520 / 1 = 52_000.00 KZT → 5_200_000 minor
        val converted = useCase(
            amountMinor = 10_000L,
            sourceCurrency = "EUR",
            targetCurrency = "KZT",
            quotes = mapOf("EUR" to 520.0),
        )

        assertEquals(5_200_000L, converted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing source quote throws IllegalArgumentException`() {
        useCase(
            amountMinor = 10_000L,
            sourceCurrency = "GBP",
            targetCurrency = "USD",
            quotes = mapOf("USD" to 475.0),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing target quote throws IllegalArgumentException`() {
        useCase(
            amountMinor = 10_000L,
            sourceCurrency = "USD",
            targetCurrency = "GBP",
            quotes = mapOf("USD" to 475.0),
        )
    }
}
