package com.android.festivar.ui.authentication.signup

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
  companion object {
    private const val TAG = "SignUpViewModel"
  }

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

  fun googleSignUp(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return
    viewModelScope.launch {
      Log.d(TAG, "Starting Google sign-up")
      _uiState.value = AuthUIState(isLoading = true)
      val result = runCatching {
        val option =
            GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id))
                .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        credentialManager.getCredential(context, request).credential
      }
          .fold(
              onSuccess = { authRepository.signInWithGoogle(it) },
              onFailure = { Result.failure(it) },
          )
      _uiState.value =
          result.fold(
              onSuccess = {
                Log.d(TAG, "Google sign-up succeeded")
                AuthUIState(isAuthenticated = true)
              },
              onFailure = {
                Log.e(TAG, "Google sign-up failed", it)
                AuthUIState(errorMsg = it.message ?: "Unable to sign up.")
              },
          )
    }
  }

  fun signUp(
      email: String,
      password: String,
  ) {
    if (_uiState.value.isLoading) return
    viewModelScope.launch {
      Log.d(TAG, "Starting email sign-up for ${email.maskForLog()}")
      _uiState.value = AuthUIState(isLoading = true)
      val result =
          authRepository.signUpWithEmailAndPassword(
              email = email,
              password = password,
          )
      _uiState.value =
          result.fold(
              onSuccess = {
                Log.d(TAG, "Email sign-up succeeded for ${email.maskForLog()}")
                AuthUIState(isAuthenticated = true)
              },
              onFailure = {
                Log.e(TAG, "Email sign-up failed for ${email.maskForLog()}", it)
                AuthUIState(errorMsg = it.message ?: "Unable to sign up.")
              },
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

  private fun String.maskForLog(): String {
    val atIndex = indexOf('@')
    return if (atIndex > 1) "${take(1)}***${substring(atIndex)}" else "***"
  }
}
