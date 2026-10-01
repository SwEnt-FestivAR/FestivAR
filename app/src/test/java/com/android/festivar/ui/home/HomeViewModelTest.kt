// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.home

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialUnknownException
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

/** Plain JVM test: the repository is a fake and the Credential Manager a mock. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  /** Remembers who is signed in and whether sign-out should fail. */
  private class FakeAuthRepository(
      private var user: AuthUser?,
      private val failSignOut: Boolean = false,
  ) : AuthRepository {
    override fun currentUser(): AuthUser? = user

    override suspend fun signInWithGoogle(credential: Credential): Result<AuthUser> =
        Result.failure(UnsupportedOperationException("not under test"))

    override fun signOut(): Result<Unit> {
      if (failSignOut) return Result.failure(IllegalStateException("offline"))
      user = null
      return Result.success(Unit)
    }
  }

  private fun managerThatClears(): CredentialManager {
    val manager = mockk<CredentialManager>()
    coEvery { manager.clearCredentialState(any<ClearCredentialStateRequest>()) } returns Unit
    return manager
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun greetsTheSignedInPersonByDisplayName() {
    val viewModel = HomeViewModel(FakeAuthRepository(AuthUser("uid-1", "Ada")))

    assertEquals("Ada", viewModel.uiState.value.userName)
  }

  @Test
  fun fallsBackToTheUidWhenThereIsNoDisplayName() {
    val viewModel = HomeViewModel(FakeAuthRepository(AuthUser("uid-1", " ")))

    assertEquals("uid-1", viewModel.uiState.value.userName)
  }

  @Test
  fun signOutClearsTheCredentialStateAndEndsTheSession() {
    val repository = FakeAuthRepository(AuthUser("uid-1", "Ada"))
    val manager = managerThatClears()
    val viewModel = HomeViewModel(repository)

    viewModel.signOut(manager)

    assertTrue(viewModel.uiState.value.signedOut)
    assertNull(repository.currentUser())
    coVerify { manager.clearCredentialState(any<ClearCredentialStateRequest>()) }
  }

  @Test
  fun signOutStillHappensWhenThereWasNoCredentialStateToClear() {
    val manager = mockk<CredentialManager>()
    coEvery { manager.clearCredentialState(any<ClearCredentialStateRequest>()) } throws
        ClearCredentialUnknownException("nothing stored")
    val viewModel = HomeViewModel(FakeAuthRepository(AuthUser("uid-1", "Ada")))

    viewModel.signOut(manager)

    assertTrue(viewModel.uiState.value.signedOut)
  }

  @Test
  fun signOutSurvivesAnUnexpectedCredentialManagerFailure() {
    val manager = mockk<CredentialManager>()
    coEvery { manager.clearCredentialState(any<ClearCredentialStateRequest>()) } throws
        SecurityException("binder died")
    val viewModel = HomeViewModel(FakeAuthRepository(AuthUser("uid-1", "Ada")))

    viewModel.signOut(manager)

    assertTrue(viewModel.uiState.value.signedOut)
  }

  @Test
  fun aFailedSignOutIsReportedAndKeepsThePersonSignedIn() {
    val viewModel = HomeViewModel(FakeAuthRepository(AuthUser("uid-1", "Ada"), failSignOut = true))

    viewModel.signOut(managerThatClears())

    val state = viewModel.uiState.value
    assertFalse(state.signedOut)
    assertTrue(state.errorMsg.orEmpty().contains("offline"))
  }
}
