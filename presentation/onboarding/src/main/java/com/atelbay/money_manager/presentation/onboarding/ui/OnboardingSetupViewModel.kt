package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.auth.SignInCancelledException
import com.atelbay.money_manager.core.datastore.UserPreferences
import com.atelbay.money_manager.core.model.SyncStatus
import com.atelbay.money_manager.core.ui.theme.AppStrings
import com.atelbay.money_manager.domain.auth.usecase.SignInWithGoogleUseCase
import com.atelbay.money_manager.domain.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class OnboardingSetupViewModel @Inject constructor(
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val syncRepository: SyncRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingSetupState())
    val state: StateFlow<OnboardingSetupState> = _state.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    fun signIn(strings: AppStrings) {
        if (_state.value.isSigningIn || _state.value.isSyncing) return
        viewModelScope.launch {
            _state.update { it.copy(isSigningIn = true, signInError = null) }
            try {
                signInWithGoogle()
                // Sign-in successful — wait for sync
                _state.update { it.copy(isSigningIn = false, isSyncing = true) }
                awaitSync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SignInCancelledException) {
                _state.update { it.copy(isSigningIn = false) }
            } catch (e: Exception) {
                val message = if (e.cause is IOException || e is IOException) {
                    strings.errorNoInternet
                } else {
                    strings.errorSignInFailed
                }
                _state.update { it.copy(isSigningIn = false, signInError = message) }
            }
        }
    }

    fun retrySync() {
        if (_state.value.isSyncing) return
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, signInError = null) }
            syncRepository.retrySync()
            awaitSync()
        }
    }

    fun skip() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted()
            _navigateToHome.emit(Unit)
        }
    }

    fun clearError() {
        _state.update { it.copy(signInError = null) }
    }

    private suspend fun awaitSync() {
        val result = syncRepository.syncStatus.first { it is SyncStatus.Synced || it is SyncStatus.Failed }
        when (result) {
            is SyncStatus.Synced -> {
                _state.update { it.copy(isSyncing = false) }
                userPreferences.setOnboardingCompleted()
                _navigateToHome.emit(Unit)
            }
            is SyncStatus.Failed -> {
                _state.update { it.copy(isSyncing = false, signInError = null) }
                // Sync failed but user is signed in — complete onboarding anyway
                userPreferences.setOnboardingCompleted()
                _navigateToHome.emit(Unit)
            }
            else -> { /* not reachable */ }
        }
    }
}
