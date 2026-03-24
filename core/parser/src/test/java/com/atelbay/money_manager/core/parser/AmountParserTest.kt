package com.atelbay.money_manager.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountParserTest {

    // ==================== space_dot ====================

    @Test
    fun `space_dot normal decimal parses correctly`() {
        val result = AmountParser.parseAmount("107 061.00", "space_dot")
        assertEquals(107061.0, result, 0.001)
    }

    @Test
    fun `space_dot integer only parses correctly`() {
        val result = AmountParser.parseAmount("5 000", "space_dot")
        assertEquals(5000.0, result, 0.001)
    }

    @Test
    fun `space_dot negative decimal parses correctly`() {
        val result = AmountParser.parseAmount("-107 061.00", "space_dot")
        assertEquals(-107061.0, result, 0.001)
    }

    @Test
    fun `space_dot non-breaking space is handled`() {
        val result = AmountParser.parseAmount("107\u00A0061.00", "space_dot")
        assertEquals(107061.0, result, 0.001)
    }

    @Test
    fun `space_dot merged cell returns first amount`() {
        val result = AmountParser.parseAmount("107 061.00  0.00  0.00 KZT", "space_dot")
        assertEquals(107061.0, result, 0.001)
    }

    @Test
    fun `space_dot single digit decimal parses correctly`() {
        val result = AmountParser.parseAmount("5.00", "space_dot")
        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun `space_dot merged cell with leading zero returns largest amount`() {
        // "Balance: 0.00 -107 061.00 KZT" — must return -107061.0, not 0.0
        val result = AmountParser.parseAmount("Balance: 0.00 -107 061.00 KZT", "space_dot")
        assertEquals(-107061.0, result, 0.001)
    }

    @Test
    fun `space_dot plain integer without spaces parses correctly`() {
        val result = AmountParser.parseAmount("5000", "space_dot")
        assertEquals(5000.0, result, 0.001)
    }

    @Test
    fun `space_dot non-breaking space integer parses correctly`() {
        val result = AmountParser.parseAmount("5\u00A0000", "space_dot")
        assertEquals(5000.0, result, 0.001)
    }

    @Test
    fun `space_dot negative integer with spaces parses correctly`() {
        val result = AmountParser.parseAmount("-5 000", "space_dot")
        assertEquals(-5000.0, result, 0.001)
    }

    // ==================== regression: other formats ====================

    @Test
    fun `space_comma with 1 234 comma 56 parses correctly`() {
        val result = AmountParser.parseAmount("1 234,56", "space_comma")
        assertEquals(1234.56, result, 0.001)
    }

    @Test
    fun `comma_dot with 1 comma 234 dot 56 parses correctly`() {
        val result = AmountParser.parseAmount("1,234.56", "comma_dot")
        assertEquals(1234.56, result, 0.001)
    }

    @Test
    fun `dot with 1234 dot 56 parses correctly`() {
        val result = AmountParser.parseAmount("1234.56", "dot")
        assertEquals(1234.56, result, 0.001)
    }
}
