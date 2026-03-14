package com.atelbay.money_manager.core.crypto

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldCipherHolder @Inject constructor() {

    @Volatile
    var cipher: FieldCipher? = null
        private set

    private var currentUid: String? = null

    fun init(uid: String) {
        if (cipher != null && currentUid == uid) return
        clear()
        cipher = AesGcmFieldCipher(uid)
        currentUid = uid
    }

    fun clear() {
        (cipher as? AesGcmFieldCipher)?.clearKey()
        cipher = null
        currentUid = null
    }
}
