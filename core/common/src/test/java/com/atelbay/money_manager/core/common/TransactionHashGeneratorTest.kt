package com.atelbay.money_manager.core.common

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHashGeneratorTest {

    @Test
    fun `same inputs produce same hash`() {
        val date = LocalDate(2026, 2, 13)
        val hash1 = generateTransactionHash(date, 500.0, "expense", "TOO KASPI MAGAZIN")
        val hash2 = generateTransactionHash(date, 500.0, "expense", "TOO KASPI MAGAZIN")

        assertEquals(hash1, hash2)
    }

    @Test
    fun `different amount produces different hash`() {
        val date = LocalDate(2026, 2, 13)
        val hash1 = generateTransactionHash(date, 500.0, "expense", "TOO KASPI MAGAZIN")
        val hash2 = generateTransactionHash(date, 501.0, "expense", "TOO KASPI MAGAZIN")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `different date produces different hash`() {
        val hash1 = generateTransactionHash(LocalDate(2026, 2, 13), 500.0, "expense", "Shop")
        val hash2 = generateTransactionHash(LocalDate(2026, 2, 14), 500.0, "expense", "Shop")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `different type produces different hash`() {
        val date = LocalDate(2026, 2, 13)
        val hash1 = generateTransactionHash(date, 500.0, "expense", "Transfer")
        val hash2 = generateTransactionHash(date, 500.0, "income", "Transfer")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hash is valid SHA-256 hex string`() {
        val hash = generateTransactionHash(LocalDate(2026, 1, 1), 100.0, "expense", "Test")

        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `details truncated to 30 chars for hash`() {
        val date = LocalDate(2026, 2, 13)
        val longDetails = "A".repeat(100)
        val longerDetails = "A".repeat(100) + "B".repeat(50)

        val hash1 = generateTransactionHash(date, 500.0, "expense", longDetails)
        val hash2 = generateTransactionHash(date, 500.0, "expense", longerDetails)

        assertEquals(hash1, hash2)
    }
}
