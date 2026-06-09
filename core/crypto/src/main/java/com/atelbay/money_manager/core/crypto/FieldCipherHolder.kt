package com.atelbay.money_manager.core.crypto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide holder of the active [FieldCipher], (re)initialized on login and cleared on logout.
 * See [FieldCipher] for the security model — the cipher is obfuscation-grade, not zero-knowledge.
 */
@Singleton
class FieldCipherHolder @Inject constructor() {

    @Volatile
    var cipher: FieldCipher? = null
        private set

    private var currentUid: String? = null

    @Synchronized
    fun init(uid: String) {
        if (cipher != null && currentUid == uid) return
        clear()
        cipher = AesGcmFieldCipher(uid)
        currentUid = uid
    }

    @Synchronized
    fun clear() {
        (cipher as? AesGcmFieldCipher)?.clearKey()
        cipher = null
        currentUid = null
    }
}
