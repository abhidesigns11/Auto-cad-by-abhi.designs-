package com.example.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.MembershipTier
import com.example.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val profile: UserProfile = UserProfile(displayName = "Guest Drafter", membershipPlan = MembershipTier.FREE_STARTER),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

/**
 * Wires our own auth server (via [AuthRepository]) to Compose UI state.
 * Restores any existing session on launch so the user isn't logged out
 * every time the app is reopened.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repository.restoreSession()
            if (existing != null) {
                _uiState.value = _uiState.value.copy(isLoggedIn = true, profile = existing)
            }
        }
    }

    fun signIn(email: String, password: String) = runAuthAction {
        repository.signIn(email, password)
    }

    fun signUp(email: String, password: String, name: String, organization: String) = runAuthAction {
        repository.signUp(email, password, name, organization)
    }

    fun signInWithGoogle(context: Context) = runAuthAction {
        repository.signInWithGoogle(context)
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your e-mail first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            when (val result = repository.sendPasswordReset(email)) {
                is AuthOutcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    infoMessage = "If an account exists for $email, a reset link was sent."
                )
                is AuthOutcome.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun upgradePlan(tier: MembershipTier) {
        // Optimistic local update, then reconcile with the server.
        _uiState.value = _uiState.value.copy(profile = _uiState.value.profile.copy(membershipPlan = tier))
        if (_uiState.value.profile.uid.isNotBlank()) {
            viewModelScope.launch {
                when (val result = repository.updateMembership(tier)) {
                    is AuthOutcome.Success -> _uiState.value = _uiState.value.copy(profile = result.profile)
                    is AuthOutcome.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    private fun runAuthAction(action: suspend () -> AuthOutcome) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            when (val result = action()) {
                is AuthOutcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    profile = result.profile
                )
                is AuthOutcome.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }
}
