package com.atelbay.money_manager.core.common

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class TransactionHashGeneratorTest {

    /** Replicates the pre-migration Double-based digest, to prove cross-version stability (R1). */
    private fun legacyHash(date: LocalDate, amount: Double, type: String, details: String): String {
        val input = "$date|$amount|$type|${details.take(30)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `same inputs produce same hash`() {
        val date = LocalDate(2026, 2, 13)
        val hash1 = generateTransactionHash(date, 50000L, "expense", "TOO KASPI MAGAZIN")
        val hash2 = generateTransactionHash(date, 50000L, "expense", "TOO KASPI MAGAZIN")

        assertEquals(hash1, hash2)
    }

    @Test
    fun `different amount produces different hash`() {
        val date = LocalDate(2026, 2, 13)
        val hash1 = generateTransactionHash(date, 50000L, "expense", "TOO KASPI MAGAZIN")
        val hash2 = generateTransactionHash(date, 50100L, "expense", "TOO KASPI MAGAZIN")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `different date produces different hash`() {
        val hash1 = generateTransactionHash(LocalDate(2026, 2, 13), 50000L, "expense", "Shop")
        val hash2 = generateTransactionHash(LocalDate(2026, 2, 14), 50000L, "expense", "Shop")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `different type produces different hash`() {
        val date = LocalDate(2026, 2, 13)
        val hash1 = generateTransactionHash(date, 50000L, "expense", "Transfer")
        val hash2 = generateTransactionHash(date, 50000L, "income", "Transfer")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hash is valid SHA-256 hex string`() {
        val hash = generateTransactionHash(LocalDate(2026, 1, 1), 10000L, "expense", "Test")

        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `details truncated to 30 chars for hash`() {
        val date = LocalDate(2026, 2, 13)
        val longDetails = "A".repeat(100)
        val longerDetails = "A".repeat(100) + "B".repeat(50)

        val hash1 = generateTransactionHash(date, 50000L, "expense", longDetails)
        val hash2 = generateTransactionHash(date, 50000L, "expense", longerDetails)

        assertEquals(hash1, hash2)
    }

    @Test
    fun `minor-unit digest matches legacy Double digest across versions`() {
        val date = LocalDate(2026, 2, 13)
        val cases = listOf(
            10000L to 100.0,
            1234567L to 12345.67,
            10L to 0.1,
            30L to 0.3,
            50000L to 500.0,
        )
        for ((minor, major) in cases) {
            assertEquals(
                "minor=$minor must reproduce the legacy Double=$major digest",
                legacyHash(date, major, "expense", "Shop"),
                generateTransactionHash(date, minor, "expense", "Shop"),
            )
        }
    }
}
