package com.android.festivar.ui.authentication.signup

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
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
  fun googleSignUp_setsLoadingThenUserOnSuccess() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credential = mockk<Credential>()
    val user = mockk<FirebaseUser>()
    val response = mockk<GetCredentialResponse>()
    every { context.getString(com.android.festivar.R.string.default_web_client_id) } returns
        "web-client-id"
    every { response.credential } returns credential
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } returns
        response
    val credentialManagerStarted = CompletableDeferred<Unit>()
    val repositoryResponse = CompletableDeferred<Result<FirebaseUser>>()
    authRepository.googleSignInBehavior = {
      credentialManagerStarted.complete(Unit)
      repositoryResponse.await()
    }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.googleSignUp(context, credentialManager)
    runBlocking { withTimeout(5_000.milliseconds) { credentialManagerStarted.await() } }
    Assert.assertTrue(viewModel.uiState.value.isLoading)

    repositoryResponse.complete(Result.success(user))
    val state = awaitState(viewModel) { it.isAuthenticated || it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertTrue(state.isAuthenticated)
    Assert.assertEquals(null, state.errorMsg)
    Assert.assertSame(credential, authRepository.lastGoogleCredential)
  }

  @Test
  fun googleSignUp_setsErrorWhenRepositoryFails() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credential = mockk<Credential>()
    val response = mockk<GetCredentialResponse>()
    val failure = IllegalStateException("Google account already exists")
    every { context.getString(com.android.festivar.R.string.default_web_client_id) } returns
        "web-client-id"
    every { response.credential } returns credential
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } returns
        response
    authRepository.googleSignInBehavior = { Result.failure<FirebaseUser>(failure) }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.googleSignUp(context, credentialManager)
    val state = awaitState(viewModel) { it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertFalse(state.isAuthenticated)
    Assert.assertEquals("Google account already exists", state.errorMsg)
  }

  @Test
  fun googleSignUp_setsErrorWhenCredentialManagerFails() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val failure = IllegalStateException("Unable to retrieve Google credentials")
    every { context.getString(com.android.festivar.R.string.default_web_client_id) } returns
        "web-client-id"
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } throws failure
    val viewModel = SignUpViewModel(authRepository)

    viewModel.googleSignUp(context, credentialManager)
    val state = awaitState(viewModel) { it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertFalse(state.isAuthenticated)
    Assert.assertEquals("Unable to retrieve Google credentials", state.errorMsg)
    Assert.assertEquals(0, authRepository.googleSignInCallCount)
  }

  @Test
  fun googleSignUp_ignoresDuplicateRequestWhileLoading() {
    val context = mockk<Context>()
    val credentialManager = mockk<CredentialManager>()
    val credential = mockk<Credential>()
    val response = mockk<GetCredentialResponse>()
    val repositoryStarted = CompletableDeferred<Unit>()
    val repositoryResponse = CompletableDeferred<Result<FirebaseUser>>()
    every { context.getString(com.android.festivar.R.string.default_web_client_id) } returns
        "web-client-id"
    every { response.credential } returns credential
    coEvery { credentialManager.getCredential(context, any<GetCredentialRequest>()) } returns
        response
    authRepository.googleSignInBehavior = {
      repositoryStarted.complete(Unit)
      repositoryResponse.await()
    }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.googleSignUp(context, credentialManager)
    runBlocking { withTimeout(5_000.milliseconds) { repositoryStarted.await() } }
    viewModel.googleSignUp(context, credentialManager)

    Assert.assertTrue(viewModel.uiState.value.isLoading)
    Assert.assertEquals(1, authRepository.googleSignInCallCount)

    repositoryResponse.complete(Result.failure(IllegalStateException("test complete")))
    awaitState(viewModel) { it.errorMsg != null }
  }

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
    Assert.assertTrue(viewModel.uiState.value.isLoading)

    response.complete(Result.success(user))
    val state = awaitState(viewModel) { it.isAuthenticated || it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertTrue(state.isAuthenticated)
    Assert.assertEquals(null, state.errorMsg)
  }

  @Test
  fun signUp_setsErrorWhenRepositoryFails() {
    val failure = IllegalStateException("Account already exists")
    authRepository.signUpBehavior = { _, _ -> Result.failure<FirebaseUser>(failure) }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.signUp("user@example.com", "password")
    val state = awaitState(viewModel) { it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Account already exists", state.errorMsg)
    Assert.assertFalse(state.isAuthenticated)
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

    Assert.assertTrue(viewModel.uiState.value.isLoading)
    Assert.assertEquals(1, authRepository.signUpCallCount)
    Assert.assertEquals("user@example.com" to "password", authRepository.lastSignUpCredentials)

    response.complete(Result.failure(IllegalStateException("test complete")))
    awaitState(viewModel) { it.errorMsg != null }
  }

  @Test
  fun signOut_setsSignedOutOnSuccess() {
    authRepository.signOutBehavior = { Result.success(Unit) }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.signOut()
    val state = awaitState(viewModel) { it.signedOut || it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertTrue(state.signedOut)
    Assert.assertEquals(null, state.errorMsg)
  }

  @Test
  fun signOut_setsErrorWhenRepositoryFails() {
    val failure = IllegalStateException("Unable to sign out")
    authRepository.signOutBehavior = { Result.failure(failure) }
    val viewModel = SignUpViewModel(authRepository)

    viewModel.signOut()
    val state = awaitState(viewModel) { it.errorMsg != null }

    Assert.assertFalse(state.isLoading)
    Assert.assertFalse(state.signedOut)
    Assert.assertEquals("Unable to sign out", state.errorMsg)
  }

  private fun awaitState(
      viewModel: SignUpViewModel,
      predicate: (AuthUIState) -> Boolean,
  ): AuthUIState = runBlocking {
    withTimeout(5_000.milliseconds) { viewModel.uiState.first(predicate) }
  }

  private class FakeAuthRepository : AuthRepository {
    var googleSignInBehavior: suspend () -> Result<FirebaseUser> = {
      Result.failure(UnsupportedOperationException("Not configured"))
    }
    var googleSignInCallCount = 0
      private set

    var lastGoogleCredential: Credential? = null
      private set

    var signUpBehavior: suspend (String, String) -> Result<FirebaseUser> = { _, _ ->
      Result.failure(UnsupportedOperationException("Not configured"))
    }
    var signUpCallCount = 0
      private set

    var lastSignUpCredentials: Pair<String, String>? = null
      private set

    var signOutBehavior: suspend () -> Result<Unit> = {
      Result.failure(UnsupportedOperationException("Not configured"))
    }

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> {
      signUpCallCount += 1
      lastSignUpCredentials = email to password
      return signUpBehavior(email, password)
    }

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
      googleSignInCallCount += 1
      lastGoogleCredential = credential
      return googleSignInBehavior()
    }

    override suspend fun signOut(): Result<Unit> = signOutBehavior()

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not configured"))
  }
}
