// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.home

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the home screen.
 *
 * @property userName what to greet the signed-in person with.
 * @property signedOut true once sign-out completed, so the screen can navigate away.
 * @property errorMsg a failure to show, or null.
 */
data class HomeUiState(
    val userName: String = "",
    val signedOut: Boolean = false,
    val errorMsg: String? = null,
)

/** Greets the signed-in person and signs them out on request. */
class HomeViewModel(private val auth: AuthRepository = AuthRepositoryProvider.repository) :
    ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState(userName = displayName()))
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  /**
   * Clears the Credential Manager state, so the next sign-in shows the account picker again, then
   * ends the session. The manager must be the instance that signed the person in.
   */
  fun signOut(credentialManager: CredentialManager) {
    viewModelScope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // Nothing was stored for this app, or Play services balked; signing out is still right.
      }
      auth
          .signOut()
          .onSuccess { _uiState.update { it.copy(signedOut = true) } }
          .onFailure { e ->
            _uiState.update { it.copy(errorMsg = "Sign-out failed: ${e.message}") }
          }
    }
  }

  private fun displayName(): String {
    val user = auth.currentUser() ?: return ""
    return user.displayName?.takeIf { it.isNotBlank() } ?: user.uid
  }
}
