// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryProvider
import com.android.festivar.model.authentication.AuthUser
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the sign-in screen.
 *
 * @property isLoading true while the Google flow is in progress.
 * @property user the signed-in user once the flow succeeded, or null.
 * @property errorMsg a failure to show, or null.
 */
data class SignInUiState(
    val isLoading: Boolean = false,
    val user: AuthUser? = null,
    val errorMsg: String? = null,
)

/** Runs the Google sign-in flow through the Credential Manager and reports the outcome. */
class SignInViewModel(private val auth: AuthRepository = AuthRepositoryProvider.repository) :
    ViewModel() {
  private val _uiState = MutableStateFlow(SignInUiState())
  val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

  /**
   * Asks [credentialManager] for a Google credential and signs in with it. The manager is injected
   * so tests hand in a fake. Dismissing the account picker is not an error.
   */
  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return
    val option =
        GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    viewModelScope.launch {
      val result =
          try {
            val response = credentialManager.getCredential(context, request)
            auth.signInWithGoogle(response.credential)
          } catch (e: GetCredentialCancellationException) {
            _uiState.update { it.copy(isLoading = false) }
            return@launch
          } catch (e: CancellationException) {
            throw e
          } catch (e: NoCredentialException) {
            Result.failure(IllegalStateException("No Google account is available on this device"))
          } catch (e: Exception) {
            Result.failure(e)
          }
      result
          .onSuccess { user -> _uiState.update { it.copy(isLoading = false, user = user) } }
          .onFailure { e ->
            _uiState.update {
              it.copy(isLoading = false, errorMsg = "Sign-in failed: ${e.message}")
            }
          }
    }
  }
}
