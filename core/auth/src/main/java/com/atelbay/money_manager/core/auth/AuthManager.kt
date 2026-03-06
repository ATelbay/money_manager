package com.atelbay.money_manager.core.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthManager {
    /** Emits the currently signed-in user, or null when signed out. Survives process death. */
    val currentUser: StateFlow<AuthUser?>

    /**
     * Launches the Google Sign-In credential picker.
     * Uses [ActivityProvider] internally to obtain the required Activity context.
     * @throws SignInCancelledException if the user dismissed the picker.
     * @throws SignInFailedException wrapping the underlying cause.
     */
    suspend fun signInWithGoogle(): AuthUser

    /** Signs out from Firebase and clears the credential session. */
    suspend fun signOut()
}
