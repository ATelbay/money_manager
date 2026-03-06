package com.atelbay.money_manager.domain.exchangerate.usecase

import com.atelbay.money_manager.domain.exchangerate.model.CurrencyRate
import com.atelbay.money_manager.domain.exchangerate.model.ExchangeRateSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertAmountUseCaseTest {

    private val useCase = ConvertAmountUseCase()
    private val snapshot = ExchangeRateSnapshot(
        fetchedAt = 1L,
        source = "NBK",
        rates = mapOf(
            "KZT" to CurrencyRate(code = "KZT", kztPerUnit = 1.0),
            "USD" to CurrencyRate(code = "USD", kztPerUnit = 500.0),
            "EUR" to CurrencyRate(code = "EUR", kztPerUnit = 550.0),
        ),
    )

    @Test
    fun `convert KZT to EUR uses snapshot rates`() {
        val converted = useCase(
            amount = 50_000.0,
            sourceCurrency = "KZT",
            targetCurrency = "EUR",
            snapshot = snapshot,
        )

        assertEquals(90.91, converted ?: 0.0, 0.0)
    }

    @Test
    fun `convert EUR to USD uses snapshot rates`() {
        val converted = useCase(
            amount = 100.0,
            sourceCurrency = "EUR",
            targetCurrency = "USD",
            snapshot = snapshot,
        )

        assertEquals(110.0, converted ?: 0.0, 0.0)
    }

    @Test
    fun `same currency returns rounded source amount`() {
        val converted = useCase(
            amount = 2.345,
            sourceCurrency = "USD",
            targetCurrency = "USD",
            snapshot = snapshot,
        )

        assertEquals(2.35, converted ?: 0.0, 0.0)
    }

    @Test
    fun `missing rate returns null`() {
        val converted = useCase(
            amount = 10.0,
            sourceCurrency = "GBP",
            targetCurrency = "USD",
            snapshot = snapshot,
        )

        assertNull(converted)
    }
}
