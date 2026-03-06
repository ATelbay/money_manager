package com.atelbay.money_manager.core.auth

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface AuthManager {
    /** Emits the currently signed-in user, or null when signed out. Survives process death. */
    val currentUser: StateFlow<AuthUser?>

    /**
     * Launches the Google Sign-In credential picker.
     * @param context Must be an Activity context (required by CredentialManager).
     * @throws SignInCancelledException if the user dismissed the picker.
     * @throws SignInFailedException wrapping the underlying cause.
     */
    suspend fun signInWithGoogle(context: Context): AuthUser

    /** Signs out from Firebase and clears the credential session. */
    suspend fun signOut()
}
