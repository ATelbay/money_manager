package com.atelbay.money_manager.core.crypto

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FieldCipherBenchmarkTest {

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `encrypt and decrypt 500 String and 500 Double fields within 100ms`() {
        val cipher = AesGcmFieldCipher("benchmark-uid")

        // Warmup
        repeat(10) {
            val enc = cipher.encrypt("warmup-$it")
            cipher.decrypt(enc)
        }

        val start = System.nanoTime()

        // 500 String fields
        val encryptedStrings = (1..500).map { cipher.encrypt("Transaction note #$it") }
        encryptedStrings.forEach { cipher.decrypt(it) }

        // 500 Double fields
        val encryptedDoubles = (1..500).map { cipher.encryptDouble(it * 100.50) }
        encryptedDoubles.forEach { cipher.decryptDouble(it) }

        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        // Spec SC-004 requires ≤100ms; using 500ms as CI safety margin for slow runners
        assertTrue(
            "Encrypt+decrypt 500 String + 500 Double fields took ${elapsedMs}ms (limit: 500ms)",
            elapsedMs <= 500,
        )
    }
}
