package com.atelbay.money_manager.core.model.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class MoneyConversionsTest {

    @Test
    fun majorToMinor_roundTrips_commonValues() {
        assertEquals(10L, majorToMinor(0.1))
        assertEquals(20L, majorToMinor(0.2))
        assertEquals(1234567L, majorToMinor(12345.67))
        assertEquals(10000L, majorToMinor(100.0))
        assertEquals(0L, majorToMinor(0.0))
    }

    @Test
    fun majorToMinor_roundsHalfUp() {
        assertEquals(235L, majorToMinor(2.345))
        assertEquals(13L, majorToMinor(0.125))
        assertEquals(-235L, majorToMinor(-2.345))
    }

    @Test
    fun minorToMajor_roundTripIsLossless() {
        val values = longArrayOf(0L, 10L, 20L, 10000L, 1234567L, -500L)
        for (minor in values) {
            assertEquals(minor, majorToMinor(minor.toMajor()))
        }
    }

    @Test
    fun toMajorDouble_dividesByHundred() {
        assertEquals(123.45, 12345L.toMajorDouble(), 0.0)
        assertEquals(0.1, 10L.toMajorDouble(), 0.0)
    }

    @Test
    fun toMajorPlainString_stripsTrailingZeros() {
        assertEquals("100", 10000L.toMajorPlainString())
        assertEquals("123.45", 12345L.toMajorPlainString())
        assertEquals("0.1", 10L.toMajorPlainString())
        assertEquals("0", 0L.toMajorPlainString())
    }

    @Test
    fun parseToMinorUnitsOrNull_parsesValid() {
        assertEquals(12345L, "123.45".parseToMinorUnitsOrNull())
        assertEquals(10000L, "100".parseToMinorUnitsOrNull())
        assertEquals(10L, "0.1".parseToMinorUnitsOrNull())
        assertEquals(150000L, "1 500".parseToMinorUnitsOrNull())
    }

    @Test
    fun parseToMinorUnitsOrNull_handlesLocaleComma() {
        assertEquals(12345L, "123,45".parseToMinorUnitsOrNull())
    }

    @Test
    fun parseToMinorUnitsOrNull_returnsNullOnInvalid() {
        assertNull("".parseToMinorUnitsOrNull())
        assertNull("   ".parseToMinorUnitsOrNull())
        assertNull("abc".parseToMinorUnitsOrNull())
        assertNull(".".parseToMinorUnitsOrNull())
    }

    @Test
    fun toMinorUnits_throwsOnOverflow() {
        assertThrows(ArithmeticException::class.java) {
            majorToMinor(1e17)
        }
        assertThrows(ArithmeticException::class.java) {
            BigDecimal("100000000000000000").toMinorUnits()
        }
    }
}
