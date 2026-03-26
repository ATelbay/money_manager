package com.atelbay.money_manager.domain.importstatement.usecase

import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.domain.auth.repository.AuthRepository
import com.atelbay.money_manager.domain.importstatement.BuildConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class UserIdHasher @Inject constructor(
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
) {
    suspend fun computeHash(userId: String?): String {
        authRepository.signInAnonymouslyIfNeeded()
        val effectiveId = userId ?: userPreferences.getOrCreateAnonymousDeviceId()
        return hmacHash(effectiveId)
    }

    private fun hmacHash(input: String): String {
        val key = BuildConfig.HMAC_KEY
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
