// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.ui.authentication.checkPassword
import com.android.festivar.ui.authentication.isEmailValid
import com.android.festivar.ui.authentication.isPasswordValid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUIState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMsg: String? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val email: String = "",
) {
  val canSubmit: Boolean
    get() =
        isPasswordValid(password) &&
            isPasswordValid(confirmPassword) &&
            isEmailValid(email) &&
            password == confirmPassword &&
            !isLoading
}

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(SignUpUIState())
  val uiState: StateFlow<SignUpUIState> = _uiState.asStateFlow()

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
    val passwdChk = checkPassword(password)
    if (passwdChk != null) {
      _uiState.update { it.copy(errorMsg = passwdChk) }
      return
    }
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    viewModelScope.launch {
      val result =
          authRepository.signUpWithEmailAndPassword(
              email = email.trim(),
              password = password,
          )
      _uiState.update { state ->
        result.fold(
            onSuccess = { state.copy(isLoading = false, isAuthenticated = true) },
            onFailure = {
              state.copy(isLoading = false, errorMsg = it.message ?: "Unable to sign up.")
            },
        )
      }
    }
  }
}
