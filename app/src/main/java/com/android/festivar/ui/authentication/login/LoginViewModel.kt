// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State displayed by the login screen. */
data class LoginUIState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val user: FirebaseUser? = null,
)

/** Handles email and Google authentication for the login screen. */
class LoginViewModel(private val authRepository: AuthRepository = AuthRepositoryFirebase()) :
    ViewModel() {
  private val _uiState = MutableStateFlow(LoginUIState())

  /** Observable state for the login screen. */
  val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

  /** Updates the email and clears any prior error. */
  fun updateEmail(email: String) {
    _uiState.update { it.copy(email = email, errorMsg = null) }
  }

  /** Updates the password and clears any prior error. */
  fun updatePassword(password: String) {
    _uiState.update { it.copy(password = password, errorMsg = null) }
  }

  /** Signs in with the current email and password. */
  fun signIn() {
    authenticateWithEmail { email, password ->
      authRepository.signInWithEmailAndPassword(email, password)
    }
  }

  /** Starts Google sign-in with Credential Manager. */
  fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
    authenticate {
      val signInWithGoogleOption =
          GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id))
              .build()
      val request =
          GetCredentialRequest.Builder().addCredentialOption(signInWithGoogleOption).build()
      val credential = credentialManager.getCredential(context, request).credential
      authRepository.signInWithGoogle(credential)
    }
  }

  /** Clears the current authentication error. */
  fun clearError() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  private fun authenticateWithEmail(
      operation: suspend (email: String, password: String) -> Result<FirebaseUser>,
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

  private fun authenticate(operation: suspend () -> Result<FirebaseUser>) {
    if (_uiState.value.isLoading) return

    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    viewModelScope.launch {
      val result = runRepositoryCall(operation)
      _uiState.update { state ->
        result.fold(
            onSuccess = { user ->
              state.copy(
                  isLoading = false,
                  user = user,
              )
            },
            onFailure = { state.copy(isLoading = false, errorMsg = it.errorMessage()) },
        )
      }
    }
  }

  private suspend fun runRepositoryCall(
      operation: suspend () -> Result<FirebaseUser>
  ): Result<FirebaseUser> =
      try {
        operation()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (cancellation: GetCredentialCancellationException) {
        Result.failure(IllegalStateException("Sign-in cancelled"))
      } catch (exception: GetCredentialException) {
        Result.failure(IllegalStateException("Failed to get credentials"))
      } catch (exception: Exception) {
        Result.failure<Nothing>(exception)
      }

  private fun Throwable.errorMessage(): String =
      message ?: "Authentication failed. Please try again."
}
