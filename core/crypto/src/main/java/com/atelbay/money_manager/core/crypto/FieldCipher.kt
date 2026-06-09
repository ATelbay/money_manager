package com.atelbay.money_manager.core.crypto

/**
 * Field-level cipher for values synced to Firestore.
 *
 * ## Security model — READ BEFORE RELYING ON THIS
 * This performs **real AES-256-GCM encryption**, but the key is derived deterministically from the
 * user's Firebase **uid** alone (see [AesGcmFieldCipher]). The uid is known to the Firebase backend
 * and is **not a secret**. Consequently:
 *
 * - ✅ Protects synced field values from a third party who reads the raw Firestore documents
 *   **without** knowing the uid (leaked export, another user, casual inspection of the database).
 * - ❌ Does **NOT** provide confidentiality against the Firebase operator (us) or anyone who knows
 *   the uid — they can re-derive the key and decrypt everything. This is **obfuscation-grade**, not
 *   zero-knowledge / end-to-end encryption.
 *
 * This trade-off is **intentional** (design decision "variant A"): it keeps multi-device sync
 * seamless — the same Google account yields the same uid on any device, hence the same key, so data
 * is readable right after re-login with no key-exchange step.
 *
 * If true confidentiality from the backend ever becomes a requirement, do NOT patch this in place:
 * switch to a passphrase-derived, wrapped-DEK scheme (a random data key encrypted by a key derived
 * from a user passphrase the server never sees). Until then, never treat synced data as hidden from
 * the server.
 */
interface FieldCipher {
    companion object {
        const val CURRENT_ENCRYPTION_VERSION = 1
    }

    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
    fun encryptDouble(value: Double): String
    fun decryptDouble(ciphertext: String): Double
    fun encryptLong(value: Long): String
    fun decryptLong(ciphertext: String): Long
}
