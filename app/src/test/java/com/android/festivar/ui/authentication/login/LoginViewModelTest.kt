// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import android.content.Context
import android.os.Looper
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {
  private val authRepository = mockk<AuthRepository>()
  private val viewModel = LoginViewModel(authRepository)

  @Test
  fun initiallyHasNoUser() {
    assertNull(viewModel.uiState.value.user)
  }

  @Test
  fun signInWithEmailUpdatesStateOnSuccess() {
    val user = mockk<FirebaseUser>()
    coEvery { authRepository.signInWithEmailAndPassword("user@example.com", "secret") } returns
        Result.success(user)
    viewModel.updateEmail(" user@example.com ")
    viewModel.updatePassword("secret")

    viewModel.signIn()
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(user, viewModel.uiState.value.user)
    assertNull(viewModel.uiState.value.errorMsg)
    coVerify(exactly = 1) {
      authRepository.signInWithEmailAndPassword("user@example.com", "secret")
    }
  }

  @Test
  fun signInWithEmptyCredentialsShowsErrorWithoutCallingRepository() {
    viewModel.signIn()

    assertEquals("Enter your email address and password", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 0) { authRepository.signInWithEmailAndPassword(any(), any()) }
  }

  @Test
  fun clearErrorRemovesCurrentError() {
    viewModel.signIn()

    viewModel.clearError()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signInWithEmailButNoPasswordShowsError() {
    viewModel.updateEmail("user@example.com")

    viewModel.signIn()

    assertEquals("Enter your email address and password", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 0) { authRepository.signInWithEmailAndPassword(any(), any()) }
  }

  @Test
  fun signInFailureIsShownInState() {
    coEvery { authRepository.signInWithEmailAndPassword("user@example.com", "wrong") } returns
        Result.failure(IllegalArgumentException("Invalid credentials"))
    viewModel.updateEmail("user@example.com")
    viewModel.updatePassword("wrong")

    viewModel.signIn()
    shadowOf(Looper.getMainLooper()).idle()

    assertNull(viewModel.uiState.value.user)
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals("Invalid credentials", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signInWithConcurrentRequestDoesNotCallRepositoryTwice() {
    val user = mockk<FirebaseUser>()
    val result = CompletableDeferred<Result<FirebaseUser>>()
    coEvery { authRepository.signInWithEmailAndPassword("user@example.com", "secret") } coAnswers
        {
          result.await()
        }
    viewModel.updateEmail("user@example.com")
    viewModel.updatePassword("secret")

    viewModel.signIn()
    viewModel.signIn()
    result.complete(Result.success(user))
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(user, viewModel.uiState.value.user)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 1) {
      authRepository.signInWithEmailAndPassword("user@example.com", "secret")
    }
  }

  @Test
  fun repositoryExceptionWithoutMessageUsesFallbackError() {
    coEvery { authRepository.signInWithEmailAndPassword("user@example.com", "secret") } coAnswers
        {
          throw IllegalStateException(null as String?)
        }
    viewModel.updateEmail("user@example.com")
    viewModel.updatePassword("secret")

    viewModel.signIn()
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals("Authentication failed. Please try again.", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun googleSignInUsesRepositoryAndUpdatesAuthenticationState() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credential = mockk<Credential>()
    val credentialResponse = mockk<GetCredentialResponse>()
    val user = mockk<FirebaseUser>()
    every { context.getString(R.string.default_web_client_id) } returns "client-id"
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } returns
        credentialResponse
    every { credentialResponse.credential } returns credential
    coEvery { authRepository.signInWithGoogle(credential) } returns Result.success(user)

    viewModel.signInWithGoogle(context, credentialManager)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(user, viewModel.uiState.value.user)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 1) { authRepository.signInWithGoogle(credential) }
  }

  @Test
  fun googleSignInRepositoryFailureShowsError() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credential = mockk<Credential>()
    val credentialResponse = mockk<GetCredentialResponse>()
    every { context.getString(R.string.default_web_client_id) } returns "client-id"
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } returns
        credentialResponse
    every { credentialResponse.credential } returns credential
    coEvery { authRepository.signInWithGoogle(credential) } returns
        Result.failure(IllegalArgumentException("Google sign-in rejected"))

    viewModel.signInWithGoogle(context, credentialManager)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals("Google sign-in rejected", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun googleSignInPickerCancellationShowsCancellationMessage() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val cancellation = mockk<GetCredentialCancellationException>()
    every { context.getString(R.string.default_web_client_id) } returns "client-id"
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } throws
        cancellation

    viewModel.signInWithGoogle(context, credentialManager)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals("Sign-in cancelled", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 0) { authRepository.signInWithGoogle(any()) }
  }

  @Test
  fun googleSignInCredentialErrorShowsClearMessage() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credentialException = mockk<GetCredentialException>()
    every { context.getString(R.string.default_web_client_id) } returns "client-id"
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } throws
        credentialException

    viewModel.signInWithGoogle(context, credentialManager)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals("Failed to get credentials", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 0) { authRepository.signInWithGoogle(any()) }
  }

  @Test
  fun googleSignInWithConcurrentRequestDoesNotCallRepositoryTwice() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credential = mockk<Credential>()
    val credentialResponse = mockk<GetCredentialResponse>()
    val user = mockk<FirebaseUser>()
    val result = CompletableDeferred<Result<FirebaseUser>>()
    every { context.getString(R.string.default_web_client_id) } returns "client-id"
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } returns
        credentialResponse
    every { credentialResponse.credential } returns credential
    coEvery { authRepository.signInWithGoogle(credential) } coAnswers { result.await() }

    viewModel.signInWithGoogle(context, credentialManager)
    viewModel.signInWithGoogle(context, credentialManager)
    result.complete(Result.success(user))
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(user, viewModel.uiState.value.user)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 1) { authRepository.signInWithGoogle(credential) }
  }
}
