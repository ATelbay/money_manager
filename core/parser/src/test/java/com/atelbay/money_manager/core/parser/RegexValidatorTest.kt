package com.atelbay.money_manager.core.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexValidatorTest {

    private val validator = RegexValidator()

    // --- Unsafe patterns ---

    @Test
    fun `nested quantifier - (a+)+ is unsafe`() {
        assertFalse(validator.isReDoSSafe("(a+)+"))
    }

    @Test
    fun `nested quantifier - (a star) star is unsafe`() {
        assertFalse(validator.isReDoSSafe("(a*)*"))
    }

    @Test
    fun `overlapping alternation - (a or a) star is unsafe`() {
        assertFalse(validator.isReDoSSafe("(a|a)*"))
    }

    @Test
    fun `adjacent overlapping - digit+digit+ is unsafe`() {
        assertFalse(validator.isReDoSSafe("\\d+\\d+"))
    }

    // --- Safe patterns: existing bank regexes ---

    @Test
    fun `kaspi bank regex is safe`() {
        val kaspi = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s*₸\\s+(Покупка|Перевод|Пополнение)\\s+(.+?)\\s*\$"
        assertTrue(validator.isReDoSSafe(kaspi))
    }

    @Test
    fun `freedom bank regex is safe`() {
        val freedom = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+([+-])\\s+([\\d\\s]+,\\d{2})\\s+(Покупка|Перевод|Пополнение|Выпуск карты)\\s+(.*?)\\s*\$"
        assertTrue(validator.isReDoSSafe(freedom))
    }

    @Test
    fun `forte bank regex is safe`() {
        val forte = "^\\s*(\\d{2}\\.\\d{2}\\.\\d{4})\\s+([+-]?)([\\d\\s]+,\\d{2})\\s+KZT\\s+(.+?)\\s{2,}(.+?)\\s*\$"
        assertTrue(validator.isReDoSSafe(forte))
    }

    @Test
    fun `bereke bank regex is safe`() {
        val bereke = "(?<date>\\d{2}/\\d{2}/\\d{4})\\s+(?<sign>[+-]?)(?<amount>[\\d,]+\\.\\d{2})\\s+KZT\\s+(?<operation>.+?)\\s{2,}(?<details>.+)"
        assertTrue(validator.isReDoSSafe(bereke))
    }

    @Test
    fun `eurasian bank regex is safe`() {
        val eurasian = "(?<date>\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(?<sign>[+-])\\s*(?<amount>[\\d\\s]+,\\d{2})\\s+KZT\\s+(?<operation>.+?)\\s{2,}(?<details>.+)"
        assertTrue(validator.isReDoSSafe(eurasian))
    }

    // --- Safe: simple valid pattern ---

    @Test
    fun `simple date pattern is safe`() {
        assertTrue(validator.isReDoSSafe("\\d{4}-\\d{2}-\\d{2}"))
    }
}
