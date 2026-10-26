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
    assertNull(viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.isRequestSuccessful)
  }

  @Test
  fun blankEmailShowsErrorWithoutCallingRepository() {
    viewModel.updateEmail("   ")

    viewModel.sendPasswordResetEmail()

    assertEquals(ForgotPasswordError.EMPTY_EMAIL, viewModel.uiState.value.error)
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
    assertTrue(viewModel.uiState.value.isRequestSuccessful)
    assertNull(viewModel.uiState.value.error)
    coVerify(exactly = 1) { authRepository.forgotPassword("user@example.com") }
  }

  @Test
  fun failedRequestShowsGenericError() {
    coEvery { authRepository.forgotPassword("user@example.com") } returns
        Result.failure(IllegalArgumentException("Request failed"))
    viewModel.updateEmail("user@example.com")

    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(ForgotPasswordError.REQUEST_FAILED, viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.isRequestSuccessful)
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
        ForgotPasswordError.REQUEST_FAILED,
        viewModel.uiState.value.error,
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
    assertTrue(viewModel.uiState.value.isRequestSuccessful)
    coVerify(exactly = 1) { authRepository.forgotPassword("user@example.com") }
  }

  @Test
  fun successfulRequestCannotBeRepeatedUntilEmailChanges() {
    coEvery { authRepository.forgotPassword("user@example.com") } returns Result.success(Unit)
    viewModel.updateEmail("user@example.com")

    viewModel.sendPasswordResetEmail()
    shadowOf(Looper.getMainLooper()).idle()
    viewModel.sendPasswordResetEmail()

    coVerify(exactly = 1) { authRepository.forgotPassword("user@example.com") }

    viewModel.updateEmail("new@example.com")
    assertFalse(viewModel.uiState.value.isRequestSuccessful)
  }
}
