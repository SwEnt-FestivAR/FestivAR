// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.authentication

import android.content.Context
import android.os.Bundle
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthUser
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs under Robolectric because the Google sign-in option carries an Android Bundle. The
 * Credential Manager and the repository are fakes, so no Google account is involved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignInViewModelTest {
  private val ada = AuthUser("uid-1", "Ada")

  /** Accepts every Google credential, or rejects every one of them. */
  private class FakeAuthRepository(private val user: AuthUser, private val fail: Boolean = false) :
      AuthRepository {
    override fun currentUser(): AuthUser? = null

    override suspend fun signInWithGoogle(credential: Credential): Result<AuthUser> =
        if (fail) Result.failure(IllegalStateException("rejected")) else Result.success(user)

    override fun signOut(): Result<Unit> = Result.success(Unit)
  }

  private fun managerReturning(credential: Credential): CredentialManager {
    val manager = mockk<CredentialManager>()
    coEvery { manager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
        GetCredentialResponse(credential)
    return manager
  }

  private fun managerThrowing(e: Exception): CredentialManager {
    val manager = mockk<CredentialManager>()
    coEvery { manager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws e
    return manager
  }

  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  private val googleCredential = CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun signInStoresTheUserOnSuccess() {
    val viewModel = SignInViewModel(FakeAuthRepository(ada))

    viewModel.signIn(context, managerReturning(googleCredential))

    val state = viewModel.uiState.value
    assertEquals(ada, state.user)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  @Test
  fun signInReportsARepositoryFailure() {
    val viewModel = SignInViewModel(FakeAuthRepository(ada, fail = true))

    viewModel.signIn(context, managerReturning(googleCredential))

    val state = viewModel.uiState.value
    assertNull(state.user)
    assertFalse(state.isLoading)
    assertTrue(state.errorMsg.orEmpty().contains("rejected"))
  }

  @Test
  fun dismissingTheAccountPickerIsNotAnError() {
    val viewModel = SignInViewModel(FakeAuthRepository(ada))

    viewModel.signIn(context, managerThrowing(GetCredentialCancellationException("cancelled")))

    val state = viewModel.uiState.value
    assertNull(state.user)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  @Test
  fun signInExplainsWhenNoGoogleAccountIsAvailable() {
    val viewModel = SignInViewModel(FakeAuthRepository(ada))

    viewModel.signIn(context, managerThrowing(NoCredentialException("none")))

    val state = viewModel.uiState.value
    assertNull(state.user)
    assertFalse(state.isLoading)
    assertTrue(state.errorMsg.orEmpty().contains("No Google account"))
  }

  @Test
  fun aSecondTapWhileTheFlowIsRunningStartsNothing() {
    val gate = CompletableDeferred<GetCredentialResponse>()
    val manager = mockk<CredentialManager>()
    coEvery { manager.getCredential(any<Context>(), any<GetCredentialRequest>()) } coAnswers
        {
          gate.await()
        }
    val viewModel = SignInViewModel(FakeAuthRepository(ada))

    viewModel.signIn(context, manager)
    viewModel.signIn(context, manager)
    gate.complete(GetCredentialResponse(googleCredential))

    coVerify(exactly = 1) { manager.getCredential(any<Context>(), any<GetCredentialRequest>()) }
    assertEquals(ada, viewModel.uiState.value.user)
  }

  @Test
  fun anyOtherCredentialManagerFailureIsShown() {
    val viewModel = SignInViewModel(FakeAuthRepository(ada))

    viewModel.signIn(context, managerThrowing(IllegalStateException("boom")))

    assertTrue(viewModel.uiState.value.errorMsg.orEmpty().contains("boom"))
  }
}
