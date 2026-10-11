package com.android.festivar.ui.authentication.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUIState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
    val password: String = "",
    val confirmPassword: String = "",
    val email: String = "",
)

class SignUpViewModel(private val authRepository: AuthRepository = AuthRepositoryFirebase()) :
    ViewModel() {
  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState.asStateFlow()

  fun updateEmail(email: String) {
    _uiState.update { it.copy(email = email) }
  }

  fun updatePassword(password: String) {
    _uiState.update { it.copy(password = password) }
  }

  fun updateConfirmPassword(confirmPassword: String) {
    _uiState.update { it.copy(confirmPassword = confirmPassword) }
  }

  /** Clears the current authentication error. */
  fun clearError() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  fun signUp(
      email: String,
      password: String,
  ) {
    if (_uiState.value.isLoading) return
    viewModelScope.launch {
      _uiState.value = AuthUIState(isLoading = true)
      val result =
          authRepository.signUpWithEmailAndPassword(
              email = email,
              password = password,
          )
      _uiState.value =
          result.fold(
              onSuccess = { AuthUIState(isAuthenticated = true) },
              onFailure = { AuthUIState(errorMsg = it.message ?: "Unable to sign up.") },
          )
    }
  }

  fun signOut() {
    viewModelScope.launch {
      val result = authRepository.signOut()
      _uiState.value =
          result.fold(
              onSuccess = { AuthUIState(signedOut = true) },
              onFailure = { AuthUIState(errorMsg = it.message ?: "Unable to sign out.") },
          )
    }
  }
}
