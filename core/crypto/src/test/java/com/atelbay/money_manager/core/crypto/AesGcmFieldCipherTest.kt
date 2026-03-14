package com.atelbay.money_manager.core.crypto

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

class AesGcmFieldCipherTest {

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
    fun `encrypt and decrypt String round-trip`() {
        val cipher = AesGcmFieldCipher("test-uid-123")
        val plaintext = "Hello, World!"
        val encrypted = cipher.encrypt(plaintext)
        val decrypted = cipher.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt Double round-trip`() {
        val cipher = AesGcmFieldCipher("test-uid-123")
        val value = 50000.75
        val encrypted = cipher.encryptDouble(value)
        val decrypted = cipher.decryptDouble(encrypted)
        assertEquals(value, decrypted, 0.0)
    }

    @Test
    fun `encrypt and decrypt Long round-trip`() {
        val cipher = AesGcmFieldCipher("test-uid-123")
        val value = 0xFF4CAF50L
        val encrypted = cipher.encryptLong(value)
        val decrypted = cipher.decryptLong(encrypted)
        assertEquals(value, decrypted)
    }

    @Test
    fun `same UID produces same key deterministically`() {
        val cipher1 = AesGcmFieldCipher("same-uid")
        val cipher2 = AesGcmFieldCipher("same-uid")
        val plaintext = "determinism check"
        val encrypted = cipher1.encrypt(plaintext)
        val decrypted = cipher2.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test(expected = GeneralSecurityException::class)
    fun `different UIDs produce different keys`() {
        val cipher1 = AesGcmFieldCipher("uid-alice")
        val cipher2 = AesGcmFieldCipher("uid-bob")
        val encrypted = cipher1.encrypt("secret data")
        cipher2.decrypt(encrypted)
    }

    @Test(expected = GeneralSecurityException::class)
    fun `decrypt with wrong key throws exception`() {
        val cipher1 = AesGcmFieldCipher("uid-alice")
        val cipher2 = AesGcmFieldCipher("uid-bob")
        val encrypted = cipher1.encrypt("secret data")
        cipher2.decrypt(encrypted)
    }

    @Test
    fun `encrypted output is valid Base64`() {
        val cipher = AesGcmFieldCipher("test-uid-123")
        val encrypted = cipher.encrypt("test")
        val decoded = java.util.Base64.getDecoder().decode(encrypted)
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun `same plaintext produces different ciphertext each time`() {
        val cipher = AesGcmFieldCipher("test-uid-123")
        val encrypted1 = cipher.encrypt("same text")
        val encrypted2 = cipher.encrypt("same text")
        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `clearKey invalidates cipher`() {
        val cipher = AesGcmFieldCipher("test-uid-123")
        cipher.clearKey()
        assertThrows(IllegalStateException::class.java) { cipher.encrypt("test") }
        assertThrows(IllegalStateException::class.java) { cipher.decrypt("dGVzdA==") }
    }
}
