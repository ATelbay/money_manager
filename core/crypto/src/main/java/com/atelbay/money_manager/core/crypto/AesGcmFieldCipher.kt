package com.atelbay.money_manager.core.crypto

import android.util.Base64
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Hkdf

class AesGcmFieldCipher(uid: String) : FieldCipher {

    private val keyBytes: ByteArray = Hkdf.computeHkdf(
        "HmacSha256",
        uid.toByteArray(Charsets.UTF_8),
        SALT,
        INFO,
        KEY_SIZE_BYTES,
    )

    private val aesGcm = AesGcmJce(keyBytes)

    private var cleared = false

    override fun encrypt(plaintext: String): String {
        if (cleared) throw IllegalStateException("Cipher has been cleared")
        val ciphertext = aesGcm.encrypt(plaintext.toByteArray(Charsets.UTF_8), EMPTY_AAD)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String {
        if (cleared) throw IllegalStateException("Cipher has been cleared")
        val decoded = Base64.decode(ciphertext, Base64.NO_WRAP)
        val plaintext = aesGcm.decrypt(decoded, EMPTY_AAD)
        return String(plaintext, Charsets.UTF_8)
    }

    override fun encryptDouble(value: Double): String = encrypt(value.toString())

    override fun decryptDouble(ciphertext: String): Double = decrypt(ciphertext).toDouble()

    override fun encryptLong(value: Long): String = encrypt(value.toString())

    override fun decryptLong(ciphertext: String): Long = decrypt(ciphertext).toLong()

    fun clearKey() {
        keyBytes.fill(0)
        cleared = true
    }

    companion object {
        private const val KEY_SIZE_BYTES = 32
        private val SALT = "money-manager-field-cipher-salt!".toByteArray(Charsets.UTF_8)
        private val INFO = "money-manager-firestore-v1".toByteArray(Charsets.UTF_8)
        private val EMPTY_AAD = ByteArray(0)
    }
}
