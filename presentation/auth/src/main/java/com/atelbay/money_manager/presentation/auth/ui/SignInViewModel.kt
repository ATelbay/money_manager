package com.atelbay.money_manager.presentation.auth.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.auth.SignInCancelledException
import com.atelbay.money_manager.domain.auth.usecase.ObserveAuthUserUseCase
import com.atelbay.money_manager.domain.auth.usecase.SignInWithGoogleUseCase
import com.atelbay.money_manager.domain.auth.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val observeAuthUser: ObserveAuthUserUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    init {
        observeAuthUser()
            .onEach { user -> _state.update { it.copy(user = user) } }
            .launchIn(viewModelScope)
    }

    fun signIn(activityContext: Context) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                signInWithGoogle(activityContext)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SignInCancelledException) {
                // no-op: user intentionally cancelled
            } catch (e: Exception) {
                val message = if (e.cause is IOException || e is IOException) {
                    "Нет подключения к интернету"
                } else {
                    "Не удалось войти. Попробуйте снова"
                }
                _state.update { it.copy(errorMessage = message) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                signOut.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // sign-out failures are non-critical
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
