// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.signup

import androidx.credentials.Credential
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

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
    val viewModel = SignUpViewModel(authRepository)

    viewModel.signUp("user@example.com", "password")
    runBlocking { withTimeout(5_000.milliseconds) { repositoryStarted.await() } }
    assertTrue(viewModel.uiState.value.isLoading)

    response.complete(Result.success(user))
    val state = awaitState(viewModel) { it.isAuthenticated || it.errorMsg != null }

    assertFalse(state.isLoading)
    assertTrue(state.isAuthenticated)
    assertEquals(null, state.errorMsg)
  }

  @Test
  fun signUp_setsErrorWhenRepositoryFails() {
    val failure = IllegalStateException("Account already exists")
    authRepository.signUpBehavior = { _, _ -> Result.failure(failure) }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.signUp("user@example.com", "password")
    val state = awaitState(viewModel) { it.errorMsg != null }

    assertFalse(state.isLoading)
    assertEquals("Account already exists", state.errorMsg)
    assertFalse(state.isAuthenticated)
  }

  @Test
  fun signUp_ignoresDuplicateRequestWhileLoading() {
    val repositoryStarted = CompletableDeferred<Unit>()
    val response = CompletableDeferred<Result<FirebaseUser>>()
    authRepository.signUpBehavior = { _, _ ->
      repositoryStarted.complete(Unit)
      response.await()
    }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.signUp("user@example.com", "password")
    runBlocking { withTimeout(5_000.milliseconds) { repositoryStarted.await() } }
    viewModel.signUp("other@example.com", "another-password")

    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(1, authRepository.signUpCallCount)
    assertEquals("user@example.com" to "password", authRepository.lastSignUpCredentials)

    response.complete(Result.failure(IllegalStateException("test complete")))
    awaitState(viewModel) { it.errorMsg != null }
  }

  private fun awaitState(
    viewModel: SignUpViewModel,
    predicate: (AuthUIState) -> Boolean,
  ): AuthUIState =
    runBlocking { withTimeout(5_000.milliseconds) { viewModel.uiState.first(predicate) } }

  private class FakeAuthRepository : AuthRepository {
    var signUpBehavior: suspend (String, String) -> Result<FirebaseUser> = { _, _ ->
      Result.failure(UnsupportedOperationException("Not configured"))
    }
    var signUpCallCount = 0
      private set

    var lastSignUpCredentials: Pair<String, String>? = null
      private set

    override suspend fun signUpWIthEmailAndPassword(
      email: String,
      password: String,
    ): Result<FirebaseUser> {
      signUpCallCount += 1
      lastSignUpCredentials = email to password
      return signUpBehavior(email, password)
    }

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun signOut(): Result<Unit> =
      Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not configured"))
  }
}
