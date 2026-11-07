// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.authentication.signup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpViewModelTest {
  private val authRepository = FakeAuthRepository()

  @Test
  fun signUp_setsLoadingThenUserOnSuccess() {
    val user = mockk<FirebaseUser>()
    val repositoryStarted = CompletableDeferred<Unit>()
    val response = CompletableDeferred<Result<FirebaseUser>>()
    authRepository.signUpBehavior = { _, _ ->
      repositoryStarted.complete(Unit)
      response.await()
    }
    val viewModel = SignUpViewModel(DataRepositoryLocal(authRepository))

    viewModel.signUp("user@example.com", "password")
    runBlocking { withTimeout(5_000.milliseconds) { repositoryStarted.await() } }
    Assert.assertTrue(viewModel.uiState.value.isLoading)

    response.complete(Result.success(user))
    val state = awaitState(viewModel) { it.isAuthenticated || it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertTrue(state.isAuthenticated)
    Assert.assertNull(state.errorMsg)
  }

  @Test
  fun signUp_setsErrorWhenRepositoryFails() {
    val failure = IllegalStateException("Account already exists")
    authRepository.signUpBehavior = { _, _ -> Result.failure<FirebaseUser>(failure) }
    val viewModel = SignUpViewModel(DataRepositoryLocal(authRepository))

    viewModel.signUp("user@example.com", "password")
    val state = awaitState(viewModel) { it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Account already exists", state.errorMsg)
    Assert.assertFalse(state.isAuthenticated)
  }

  @Test
  fun clearError_removesCurrentError() {
    authRepository.signUpBehavior = { _, _ ->
      Result.failure(IllegalStateException("Account already exists"))
    }
    val viewModel = SignUpViewModel(DataRepositoryLocal(authRepository))

    viewModel.signUp("user@example.com", "password")
    awaitState(viewModel) { it.errorMsg != null }

    viewModel.clearError()

    Assert.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signUp_ignoresDuplicateRequestWhileLoading() {
    val repositoryStarted = CompletableDeferred<Unit>()
    val response = CompletableDeferred<Result<FirebaseUser>>()
    authRepository.signUpBehavior = { _, _ ->
      repositoryStarted.complete(Unit)
      response.await()
    }
    val viewModel = SignUpViewModel(DataRepositoryLocal(authRepository))

    viewModel.signUp("user@example.com", "password")
    runBlocking { withTimeout(5_000.milliseconds) { repositoryStarted.await() } }
    viewModel.signUp("other@example.com", "another-password")

    Assert.assertTrue(viewModel.uiState.value.isLoading)
    Assert.assertEquals(1, authRepository.signUpCallCount)
    Assert.assertEquals("user@example.com" to "password", authRepository.lastSignUpCredentials)

    response.complete(Result.failure(IllegalStateException("test complete")))
    awaitState(viewModel) { it.errorMsg != null }
  }

  private fun awaitState(
      viewModel: SignUpViewModel,
      predicate: (SignUpUIState) -> Boolean,
  ): SignUpUIState = runBlocking {
    withTimeout(5_000.milliseconds) { viewModel.uiState.first(predicate) }
  }
}
