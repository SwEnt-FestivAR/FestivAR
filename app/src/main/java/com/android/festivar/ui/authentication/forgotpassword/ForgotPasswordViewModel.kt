// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State displayed by the forgot-password screen. */
data class ForgotPasswordUIState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isRequestSuccessful: Boolean = false,
    val error: ForgotPasswordError? = null,
)

enum class ForgotPasswordError {
  EMPTY_EMAIL,
  REQUEST_FAILED,
}

/** Handles password reset requests. */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository = AuthRepositoryFirebase()
) : ViewModel() {
  private val _uiState = MutableStateFlow(ForgotPasswordUIState())

  /** Observable state for the forgot-password screen. */
  val uiState: StateFlow<ForgotPasswordUIState> = _uiState.asStateFlow()

  /** Updates the email and clears previous feedback. */
  fun updateEmail(email: String) {
    _uiState.update { it.copy(email = email, isRequestSuccessful = false, error = null) }
  }

  /** Sends a password reset email to the entered address. */
  fun sendPasswordResetEmail() {
    val state = _uiState.value
    if (state.isLoading || state.isRequestSuccessful) return
    if (state.email.isBlank()) {
      _uiState.update { it.copy(error = ForgotPasswordError.EMPTY_EMAIL) }
      return
    }

    _uiState.update { it.copy(isLoading = true, isRequestSuccessful = false, error = null) }
    viewModelScope.launch {
      val result =
          try {
            authRepository.forgotPassword(state.email.trim())
          } catch (cancellation: CancellationException) {
            throw cancellation
          } catch (exception: Exception) {
            Result.failure(exception)
          }
      _uiState.update { currentState ->
        result.fold(
            onSuccess = { currentState.copy(isLoading = false, isRequestSuccessful = true) },
            onFailure = {
              currentState.copy(isLoading = false, error = ForgotPasswordError.REQUEST_FAILED)
            },
        )
      }
    }
  }
}
