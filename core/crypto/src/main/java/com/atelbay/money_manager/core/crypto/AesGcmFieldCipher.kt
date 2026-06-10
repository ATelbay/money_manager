package com.atelbay.money_manager.core.crypto

import android.util.Base64
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Hkdf

/**
 * AES-256-GCM [FieldCipher] whose key is derived via HKDF-SHA256 from the user's Firebase [uid]
 * plus a static salt/info baked into the app.
 *
 * Because the only per-user input is the uid — which the backend knows and which is not secret —
 * this yields **obfuscation from third parties, not confidentiality from the backend**. See
 * [FieldCipher] for the full security model and the rationale for this deliberate trade-off.
 *
 * The AES-GCM primitive itself is used correctly: [AesGcmJce] generates a fresh random 12-byte
 * nonce per [encrypt] call and prepends it to the ciphertext, so identical plaintexts produce
 * distinct ciphertexts.
 */
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

    /**
     * Invalidates this cipher: zeroes our copy of the derived key and makes further
     * [encrypt]/[decrypt] calls throw.
     *
     * NOTE: this is best-effort, not a guaranteed secure wipe. [AesGcmJce] (Tink) keeps its
     * own internal copy of the key material inside the JCE [javax.crypto.Cipher], which we
     * cannot reach to zero out. The key therefore may still reside in heap memory until GC.
     * The main guarantee here is the [cleared] flag preventing reuse after logout.
     */
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
