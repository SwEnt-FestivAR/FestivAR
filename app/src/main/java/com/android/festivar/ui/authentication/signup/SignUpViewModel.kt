package com.android.festivar.ui.authentication.signup

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUIState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
)

class SignUpViewModel(
    private val authRepository: AuthRepository // TODO Add default AuthRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState.asStateFlow()

  fun googleSignUp(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return
    viewModelScope.launch {
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
              onSuccess = { AuthUIState(isAuthenticated = true) },
              onFailure = { AuthUIState(errorMsg = it.message ?: "Unable to sign up.") },
          )
    }
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
