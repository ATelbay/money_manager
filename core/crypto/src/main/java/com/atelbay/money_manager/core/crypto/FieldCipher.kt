package com.atelbay.money_manager.core.crypto

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
