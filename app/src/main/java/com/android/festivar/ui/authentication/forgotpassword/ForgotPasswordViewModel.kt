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
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

/** Handles password reset requests. */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository = AuthRepositoryFirebase()
) : ViewModel() {
  private val _uiState = MutableStateFlow(ForgotPasswordUIState())

  /** Observable state for the forgot-password screen. */
  val uiState: StateFlow<ForgotPasswordUIState> = _uiState.asStateFlow()

  /** Updates the email and clears previous feedback. */
  fun updateEmail(email: String) {
    _uiState.update { it.copy(email = email, successMessage = null, errorMessage = null) }
  }

  /** Sends a password reset email to the entered address. */
  fun sendPasswordResetEmail() {
    val state = _uiState.value
    if (state.isLoading) return
    if (state.email.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Enter your email address") }
      return
    }

    _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
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
            onSuccess = {
              currentState.copy(
                  isLoading = false,
                  successMessage =
                      "If an account exists for this email, you’ll receive a password reset email.",
              )
            },
            onFailure = {
              currentState.copy(
                  isLoading = false,
                  errorMessage =
                      it.message ?: "Unable to send password reset email. Please try again.",
              )
            },
        )
      }
    }
  }
}
