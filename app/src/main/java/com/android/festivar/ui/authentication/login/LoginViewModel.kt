// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
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

data class LoginUIState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val user: FirebaseUser? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryFirebase(),
    private val _uiState: MutableStateFlow<LoginUIState> = MutableStateFlow(LoginUIState()),
    val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow(),
) : ViewModel() {

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
      } catch (exception: Exception) {
        Result.failure<Nothing>(exception)
      }

  private fun Throwable.errorMessage(): String =
      message ?: "Authentication failed. Please try again."
}
