// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.forgotpassword

import android.os.Looper
import com.android.festivar.model.authentication.AuthRepository
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
class ForgotPasswordViewModelTest {
  private val authRepository = mockk<AuthRepository>()
  private val viewModel = ForgotPasswordViewModel(authRepository)

  @Test
  fun initiallyHasEmptyState() {
    assertEquals(ForgotPasswordUIState(), viewModel.uiState.value)
  }

  @Test
  fun updateEmailClearsPreviousFeedback() {
    coEvery { authRepository.forgotPassword(any()) } returns
        Result.failure(IllegalArgumentException("Request failed"))
    viewModel.updateEmail("user@example.com")
    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()

    viewModel.updateEmail("new@example.com")

    assertEquals("new@example.com", viewModel.uiState.value.email)
    assertNull(viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.successMessage)
  }

  @Test
  fun blankEmailShowsErrorWithoutCallingRepository() {
    viewModel.updateEmail("   ")

    viewModel.sendPasswordResetEmail()

    assertEquals("Enter your email address", viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isLoading)
    coVerify(exactly = 0) { authRepository.forgotPassword(any()) }
  }

  @Test
  fun successfulRequestTrimsEmailAndShowsSuccess() {
    coEvery { authRepository.forgotPassword("user@example.com") } returns Result.success(Unit)
    viewModel.updateEmail("  user@example.com  ")

    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        "If an account exists for this email, you’ll receive a password reset email.",
        viewModel.uiState.value.successMessage,
    )
    assertNull(viewModel.uiState.value.errorMessage)
    coVerify(exactly = 1) { authRepository.forgotPassword("user@example.com") }
  }

  @Test
  fun failedRequestShowsRepositoryMessage() {
    coEvery { authRepository.forgotPassword("user@example.com") } returns
        Result.failure(IllegalArgumentException("Request failed"))
    viewModel.updateEmail("user@example.com")

    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals("Request failed", viewModel.uiState.value.errorMessage)
    assertNull(viewModel.uiState.value.successMessage)
  }

  @Test
  fun requestExceptionWithoutMessageShowsFallback() {
    coEvery { authRepository.forgotPassword("user@example.com") } throws
        IllegalStateException(null as String?)
    viewModel.updateEmail("user@example.com")

    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        "Unable to send password reset email. Please try again.",
        viewModel.uiState.value.errorMessage,
    )
  }

  @Test
  fun concurrentRequestsCallRepositoryOnlyOnce() {
    val result = CompletableDeferred<Result<Unit>>()
    coEvery { authRepository.forgotPassword("user@example.com") } coAnswers { result.await() }
    viewModel.updateEmail("user@example.com")

    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(viewModel.uiState.value.isLoading)
    viewModel.sendPasswordResetEmail()
    result.complete(Result.success(Unit))
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        "If an account exists for this email, you’ll receive a password reset email.",
        viewModel.uiState.value.successMessage,
    )
    coVerify(exactly = 1) { authRepository.forgotPassword("user@example.com") }
  }
}
