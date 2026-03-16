package com.atelbay.money_manager.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyDisplayFormatterTest {

    @Test
    fun `safe supported symbol uses symbol first mode`() {
        val context = MoneyDisplayFormatter.resolve("KZT")

        assertEquals(CurrencyDisplayContextMode.RESOLVED_SINGLE, context.mode)
        assertEquals("KZT", context.displayCurrencyCode)
        assertEquals("₸", context.displayCurrencySymbol)
        assertEquals(MoneyDisplayMode.SYMBOL_FIRST, context.fallbackLabelMode)

        val presentation = MoneyDisplayFormatter.format(context)

        assertEquals("₸", presentation.primaryLabel)
        assertNull(presentation.secondaryLabel)
        assertEquals(MoneyDisplayMode.SYMBOL_FIRST, presentation.displayMode)
    }

    @Test
    fun `ambiguous supported symbol falls back to symbol plus code`() {
        val context = MoneyDisplayFormatter.resolve("usd")

        assertEquals(CurrencyDisplayContextMode.RESOLVED_SINGLE_AMBIGUOUS, context.mode)
        assertEquals("USD", context.displayCurrencyCode)
        assertEquals("$", context.displayCurrencySymbol)
        assertEquals(MoneyDisplayMode.SYMBOL_PLUS_CODE, context.fallbackLabelMode)

        val presentation = MoneyDisplayFormatter.format(context)

        assertEquals("$", presentation.primaryLabel)
        assertEquals("USD", presentation.secondaryLabel)
        assertEquals(MoneyDisplayMode.SYMBOL_PLUS_CODE, presentation.displayMode)
    }

    @Test
    fun `unsupported symbol mapping falls back to code only`() {
        val context = MoneyDisplayFormatter.resolve("aed")

        assertEquals(CurrencyDisplayContextMode.RESOLVED_SINGLE_AMBIGUOUS, context.mode)
        assertEquals("AED", context.displayCurrencyCode)
        assertNull(context.displayCurrencySymbol)
        assertEquals(MoneyDisplayMode.CODE_ONLY, context.fallbackLabelMode)

        val presentation = MoneyDisplayFormatter.format(context)

        assertEquals("AED", presentation.primaryLabel)
        assertNull(presentation.secondaryLabel)
        assertEquals(MoneyDisplayMode.CODE_ONLY, presentation.displayMode)
    }

    @Test
    fun `unavailable display mode returns dash with helper text`() {
        val context = MoneyDisplayFormatter.unavailable(reason = "Mixed currencies")
        val presentation = MoneyDisplayFormatter.format(context)

        assertEquals(CurrencyDisplayContextMode.UNAVAILABLE, context.mode)
        assertEquals(MoneyDisplayMode.UNAVAILABLE, context.fallbackLabelMode)
        assertEquals("-", presentation.primaryLabel)
        assertEquals("Mixed currencies", presentation.secondaryLabel)
        assertEquals(MoneyDisplayMode.UNAVAILABLE, presentation.displayMode)
    }

    @Test
    fun `resolve and format provides direct code only fallback for unsupported currency`() {
        val presentation = MoneyDisplayFormatter.resolveAndFormat("BHD")

        assertEquals("BHD", presentation.primaryLabel)
        assertNull(presentation.secondaryLabel)
        assertEquals(MoneyDisplayMode.CODE_ONLY, presentation.displayMode)
    }

    @Test
    fun `formatted amount uses shared display mode rules`() {
        val ambiguous = MoneyDisplayFormatter.resolveAndFormat("USD")
        val unsupported = MoneyDisplayFormatter.resolveAndFormat("BHD")

        assertEquals("+$ USD 10.50", ambiguous.formatAmount(10.5, sign = "+"))
        assertEquals("BHD 99.00", unsupported.formatAmount(99.0))
    }
}
