package com.atelbay.money_manager.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AggregateCurrencyDisplayResolverTest {

    @Test
    fun `empty scope falls back to normalized base currency`() {
        val resolution = AggregateCurrencyDisplayResolver.resolve(
            baseCurrency = " usd ",
            scopedCurrencies = emptySet(),
            canDisplayInBaseCurrency = true,
        )

        assertEquals("USD", resolution.displayCurrency)
        assertEquals(
            AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
            resolution.displayMode,
        )
    }

    @Test
    fun `resolvable multi currency scope uses normalized base currency`() {
        val resolution = AggregateCurrencyDisplayResolver.resolve(
            baseCurrency = " eur ",
            scopedCurrencies = setOf("kzt", "usd"),
            canDisplayInBaseCurrency = true,
        )

        assertEquals("EUR", resolution.displayCurrency)
        assertEquals(AggregateCurrencyDisplayMode.CONVERTED, resolution.displayMode)
    }

    @Test
    fun `unresolvable single currency scope keeps original currency`() {
        val resolution = AggregateCurrencyDisplayResolver.resolve(
            baseCurrency = "USD",
            scopedCurrencies = setOf(" kzt "),
            canDisplayInBaseCurrency = false,
        )

        assertEquals("KZT", resolution.displayCurrency)
        assertEquals(
            AggregateCurrencyDisplayMode.ORIGINAL_SINGLE_CURRENCY,
            resolution.displayMode,
        )
    }

    @Test
    fun `unresolvable mixed scope reports unavailable display`() {
        val resolution = AggregateCurrencyDisplayResolver.resolve(
            baseCurrency = "USD",
            scopedCurrencies = setOf("KZT", "GBP"),
            canDisplayInBaseCurrency = false,
        )

        assertNull(resolution.displayCurrency)
        assertEquals(AggregateCurrencyDisplayMode.UNAVAILABLE, resolution.displayMode)
    }
}
