// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import android.os.Looper
import androidx.credentials.Credential
import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {
  private val authRepository = mockk<AuthRepository>()
  private val viewModel = LoginViewModel(authRepository)

  @Test
  fun initiallySignedOut() {
    assertTrue(viewModel.uiState.value.signedOut)
    assertFalse(viewModel.uiState.value.isAuthenticated)
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

    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.signedOut)
    assertFalse(viewModel.uiState.value.isLoading)
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

    assertFalse(viewModel.uiState.value.isAuthenticated)
    assertTrue(viewModel.uiState.value.signedOut)
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

    assertTrue(viewModel.uiState.value.isAuthenticated)
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
    val credential = mockk<Credential>()
    val user = mockk<FirebaseUser>()
    coEvery { authRepository.signInWithGoogle(credential) } returns Result.success(user)

    viewModel.signInWithGoogle(credential)
    shadowOf(Looper.getMainLooper()).idle()

    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.signedOut)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 1) { authRepository.signInWithGoogle(credential) }
  }

  @Test
  fun googleSignInWithConcurrentRequestDoesNotCallRepositoryTwice() {
    val credential = mockk<Credential>()
    val user = mockk<FirebaseUser>()
    val result = CompletableDeferred<Result<FirebaseUser>>()
    coEvery { authRepository.signInWithGoogle(credential) } coAnswers { result.await() }

    viewModel.signInWithGoogle(credential)
    viewModel.signInWithGoogle(credential)
    result.complete(Result.success(user))
    shadowOf(Looper.getMainLooper()).idle()

    assertTrue(viewModel.uiState.value.isAuthenticated)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 1) { authRepository.signInWithGoogle(credential) }
  }
}
