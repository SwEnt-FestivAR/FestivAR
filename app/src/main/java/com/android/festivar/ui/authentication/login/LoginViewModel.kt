// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import androidx.credentials.Credential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.authentication.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUIState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMsg: String? = null,
    val signedOut: Boolean = true,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(LoginUIState())
  val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

  fun updateEmail(email: String) {
    _uiState.update { it.copy(email = email, errorMsg = null) }
  }

  fun updatePassword(password: String) {
    _uiState.update { it.copy(password = password, errorMsg = null) }
  }

  fun signIn() {
    authenticateWithEmail { email, password ->
      authRepository.signInWithEmailAndPassword(email, password)
    }
  }

  fun signInWithGoogle(credential: Credential) {
    authenticate { authRepository.signInWithGoogle(credential) }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  private fun authenticateWithEmail(
      operation: suspend (email: String, password: String) -> Result<*>,
  ) {
    val state = _uiState.value
    if (state.isLoading) return
    if (state.email.isBlank() || state.password.isBlank()) {
      _uiState.update { it.copy(errorMsg = "Enter your email address and password") }
      return
    }

    val email = state.email.trim()
    val password = state.password
    authenticate { operation(email, password) }
  }

  private fun authenticate(operation: suspend () -> Result<*>) {
    if (_uiState.value.isLoading) return

    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    viewModelScope.launch {
      val result = runRepositoryCall(operation)
      _uiState.update { state ->
        result.fold(
            onSuccess = {
              state.copy(
                  isLoading = false,
                  isAuthenticated = true,
                  signedOut = false,
              )
            },
            onFailure = { state.copy(isLoading = false, errorMsg = it.errorMessage()) },
        )
      }
    }
  }

  private suspend fun runRepositoryCall(operation: suspend () -> Result<*>): Result<*> =
      try {
        operation()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (exception: Exception) {
        Result.failure<Nothing>(exception)
      }

  private fun Throwable.errorMessage(): String =
      message ?: "Authentication failed. Please try again."
}
