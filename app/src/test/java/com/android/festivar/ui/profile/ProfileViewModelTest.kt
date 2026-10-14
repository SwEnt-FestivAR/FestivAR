// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.profile

import androidx.credentials.Credential
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  /**
   * A [UsersRepositoryLocal] serving [users]. It throws [getError] on every fetch and [editError]
   * on every edit while they are set, and records every [User] it is asked to save. While
   * [editGate] is set, an edit waits until the test completes it, so the tests can act during a
   * save.
   */
  private class RecordingUsersRepository(
      users: List<User> = listOf(),
      var getError: Exception? = null,
      var editError: Exception? = null,
      var editGate: CompletableDeferred<Unit>? = null,
      private val local: UsersRepository = UsersRepositoryLocal(users),
  ) : UsersRepository by local {
    val editedUsers = mutableListOf<User>()

    override suspend fun getUser(uId: String): User {
      getError?.let { throw it }
      return local.getUser(uId)
    }

    override suspend fun editUser(uId: String, newValue: User) {
      editedUsers.add(newValue)
      editGate?.await()
      editError?.let { throw it }
      local.editUser(uId, newValue)
    }
  }

  /**
   * An [AuthRepository] whose sign out returns [signOutResult] and counts its calls. While
   * [signOutGate] is set, a sign out waits until the test completes it, so the tests can act during
   * a sign out.
   */
  private class FakeAuthRepository(
      var signOutResult: Result<Unit> = Result.success(Unit),
      var signOutGate: CompletableDeferred<Unit>? = null,
  ) : AuthRepository {
    var signOutCount = 0
      private set

    override suspend fun signOut(): Result<Unit> {
      signOutCount += 1
      signOutGate?.await()
      return signOutResult
    }

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used"))

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used"))

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used"))
  }

  private val testDispatcher = StandardTestDispatcher()

  private val sara =
      User(
          uid = "u1",
          name = "Sara",
          surname = "Keller",
          email = "sara.keller@epfl.ch",
          joinedEvents = listOf("e1", "e2"),
          tasksAssigned = listOf("t1"),
      )

  private val authRepository = FakeAuthRepository()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(
      repository: UsersRepository = RecordingUsersRepository(listOf(sara)),
      uId: String = sara.uid,
  ): ProfileViewModel = ProfileViewModel(repository, authRepository, uId)

  /** Test that the UI state is loading until the user is fetched, then fills the fields. */
  @Test
  fun isLoadingUntilTheUserIsFetched() = runTest {
    val viewModel = createViewModel()
    assertTrue(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.user)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(sara, state.user)
    assertEquals(sara.name, state.name)
    assertEquals(sara.surname, state.surname)
  }

  /**
   * Test that any repository failure, not only a missing user, stops the loading, shows an error
   * message and leaves nothing to save.
   */
  @Test
  fun repositoryFailureShowsErrorMsg() = runTest {
    val repository =
        RecordingUsersRepository(listOf(sara), getError = RuntimeException("Network down"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertEquals("Failed to load profile: Network down", state.errorMsg)
    assertFalse(state.canSave)
  }

  /** Test that a failure whose exception has no message still shows an error message. */
  @Test
  fun failureWithoutMessageShowsUnknownError() = runTest {
    val loadViewModel =
        createViewModel(RecordingUsersRepository(listOf(sara), getError = RuntimeException()))
    advanceUntilIdle()
    assertEquals("Failed to load profile: unknown error", loadViewModel.uiState.value.errorMsg)

    val saveViewModel =
        createViewModel(RecordingUsersRepository(listOf(sara), editError = RuntimeException()))
    advanceUntilIdle()
    saveViewModel.updateName("Sarah")
    saveViewModel.saveChanges()
    advanceUntilIdle()
    assertEquals("Failed to update profile: unknown error", saveViewModel.uiState.value.errorMsg)

    authRepository.signOutResult = Result.failure(RuntimeException())
    val signOutViewModel = createViewModel()
    advanceUntilIdle()
    signOutViewModel.signOut()
    advanceUntilIdle()
    assertEquals("Failed to sign out: unknown error", signOutViewModel.uiState.value.errorMsg)
  }

  /** Test that typing in the fields changes them, but not the saved user. */
  @Test
  fun updateNameAndSurnameDoNotChangeTheSavedUser() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.updateName("Sarah")
    viewModel.updateSurname("Keller-Muller")

    val state = viewModel.uiState.value
    assertEquals("Sarah", state.name)
    assertEquals("Keller-Muller", state.surname)
    assertEquals(sara, state.user)
  }

  /** Test that nothing can be saved before the user is loaded. */
  @Test
  fun cannotSaveWithoutAUser() {
    assertFalse(ProfileUIState(name = "Sara", surname = "Keller").canSave)
  }

  /** Test that nothing can be saved while the fields are those of the saved user. */
  @Test
  fun cannotSaveWithoutChanges() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.canSave)

    // Changing a field and changing it back leaves nothing to save
    viewModel.updateName("Sarah")
    viewModel.updateName("Sara")
    assertFalse(viewModel.uiState.value.canSave)
  }

  /** Test that the name or the surname changed alone is enough to save. */
  @Test
  fun canSaveOnceANameChanges() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.updateName("Sarah")
    assertTrue(viewModel.uiState.value.canSave)

    viewModel.updateName(sara.name)
    viewModel.updateSurname("Muller")
    assertTrue(viewModel.uiState.value.canSave)
  }

  /** Test that spaces around a name are not a change. */
  @Test
  fun surroundingSpacesAreNotAChange() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.updateName("  Sara ")
    viewModel.updateSurname(" Keller  ")

    assertFalse(viewModel.uiState.value.canSave)
  }

  /** Test that a blank name or surname cannot be saved. */
  @Test
  fun cannotSaveABlankField() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.updateName("   ")
    assertFalse(viewModel.uiState.value.canSave)

    viewModel.updateName("Sarah")
    viewModel.updateSurname("")
    assertFalse(viewModel.uiState.value.canSave)
  }

  /** Test that nothing can be saved while saving or signing out, nor once signed out. */
  @Test
  fun cannotSaveWhileSavingOrSigningOut() {
    val state = ProfileUIState(user = sara, name = "Sarah", surname = sara.surname)
    assertTrue(state.canSave)
    assertFalse(state.copy(isSaving = true).canSave)
    assertFalse(state.copy(isSigningOut = true).canSave)
    assertFalse(state.copy(signedOut = true).canSave)
  }

  /**
   * Test that saving stores the trimmed names, keeps the email, joined events and assigned tasks,
   * and makes the saved user the new original.
   */
  @Test
  fun saveChangesSavesTheTrimmedNamesAndKeepsTheRest() = runTest {
    val repository = RecordingUsersRepository(listOf(sara))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.updateName("  Sarah ")
    viewModel.updateSurname(" Muller")
    viewModel.saveChanges()
    advanceUntilIdle()

    val expected = sara.copy(name = "Sarah", surname = "Muller")
    assertEquals(listOf(expected), repository.editedUsers)
    assertEquals(expected, repository.getUser(sara.uid))
    val state = viewModel.uiState.value
    assertEquals(expected, state.user)
    assertFalse(state.isSaving)
    assertNull(state.errorMsg)
    assertFalse(state.canSave)
  }

  /** Test that saving does nothing when there is nothing to save. */
  @Test
  fun saveChangesDoesNothingWithoutChanges() = runTest {
    val repository = RecordingUsersRepository(listOf(sara))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.saveChanges()
    advanceUntilIdle()

    assertTrue(repository.editedUsers.isEmpty())
    assertFalse(viewModel.uiState.value.isSaving)
  }

  /** Test that the UI state is saving until the repository answers, and a second tap is ignored. */
  @Test
  fun isSavingUntilTheRepositoryAnswersAndASecondTapIsIgnored() = runTest {
    val gate = CompletableDeferred<Unit>()
    val repository = RecordingUsersRepository(listOf(sara), editGate = gate)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.updateName("Sarah")
    viewModel.saveChanges()
    assertTrue(viewModel.uiState.value.isSaving)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isSaving)

    viewModel.saveChanges()
    advanceUntilIdle()
    assertEquals(1, repository.editedUsers.size)

    gate.complete(Unit)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isSaving)
    assertEquals(sara.copy(name = "Sarah"), viewModel.uiState.value.user)
  }

  /**
   * Test that typing during a save does not change what is saved, and that the new typing can be
   * saved afterwards.
   */
  @Test
  fun typingDuringASaveDoesNotChangeWhatIsSaved() = runTest {
    val gate = CompletableDeferred<Unit>()
    val repository = RecordingUsersRepository(listOf(sara), editGate = gate)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.updateName("Sarah")
    viewModel.saveChanges()
    advanceUntilIdle()
    viewModel.updateName("Sarita")
    gate.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf(sara.copy(name = "Sarah")), repository.editedUsers)
    val state = viewModel.uiState.value
    assertEquals(sara.copy(name = "Sarah"), state.user)
    assertEquals("Sarita", state.name)
    assertTrue(state.canSave)
  }

  /**
   * Test that a failing save shows an error message and keeps the typed values, and that retrying
   * once the repository recovers saves them and removes the error message.
   */
  @Test
  fun saveFailureShowsErrorMsgAndCanBeRetried() = runTest {
    val repository =
        RecordingUsersRepository(listOf(sara), editError = RuntimeException("Network down"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    viewModel.updateName("Sarah")
    viewModel.saveChanges()
    advanceUntilIdle()

    val failed = viewModel.uiState.value
    assertFalse(failed.isSaving)
    assertEquals("Failed to update profile: Network down", failed.errorMsg)
    assertEquals(sara, failed.user)
    assertEquals("Sarah", failed.name)
    assertTrue(failed.canSave)

    repository.editError = null
    viewModel.saveChanges()
    assertNull(viewModel.uiState.value.errorMsg)
    advanceUntilIdle()

    val saved = viewModel.uiState.value
    assertNull(saved.errorMsg)
    assertEquals(sara.copy(name = "Sarah"), saved.user)
    assertEquals(sara.copy(name = "Sarah"), repository.getUser(sara.uid))
  }

  /**
   * Test that a successful sign out sets signedOut, without any error, and that signing out again
   * then does nothing.
   */
  @Test
  fun signOutSuccessSetsSignedOut() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.signOut()
    advanceUntilIdle()

    assertEquals(1, authRepository.signOutCount)
    assertTrue(viewModel.uiState.value.signedOut)
    assertNull(viewModel.uiState.value.errorMsg)

    viewModel.signOut()
    advanceUntilIdle()
    assertEquals(1, authRepository.signOutCount)
  }

  /**
   * Test that a failing sign out shows an error message and keeps the user signed in, and that it
   * ends the signing out, so the user can try again.
   */
  @Test
  fun signOutFailureShowsErrorMsgAndCanBeRetried() = runTest {
    authRepository.signOutResult = Result.failure(RuntimeException("Network down"))
    val viewModel = createViewModel()
    advanceUntilIdle()
    viewModel.signOut()
    advanceUntilIdle()

    val failed = viewModel.uiState.value
    assertFalse(failed.signedOut)
    assertFalse(failed.isSigningOut)
    assertEquals("Failed to sign out: Network down", failed.errorMsg)

    authRepository.signOutResult = Result.success(Unit)
    viewModel.signOut()
    advanceUntilIdle()

    assertEquals(2, authRepository.signOutCount)
    assertTrue(viewModel.uiState.value.signedOut)
  }

  /**
   * Test that the UI state is signing out until the repository answers, without being signed out
   * yet, and that a second tap meanwhile is ignored.
   */
  @Test
  fun isSigningOutUntilTheRepositoryAnswersAndASecondTapIsIgnored() = runTest {
    val gate = CompletableDeferred<Unit>()
    authRepository.signOutGate = gate
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.signOut()
    assertTrue(viewModel.uiState.value.isSigningOut)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isSigningOut)
    assertFalse(viewModel.uiState.value.signedOut)

    viewModel.signOut()
    advanceUntilIdle()
    assertEquals(1, authRepository.signOutCount)

    gate.complete(Unit)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.signedOut)
    assertEquals(1, authRepository.signOutCount)
  }

  /** Test that typing and saving do nothing while signing out, nor once signed out. */
  @Test
  fun editingIsIgnoredWhileSigningOutAndOnceSignedOut() = runTest {
    val gate = CompletableDeferred<Unit>()
    authRepository.signOutGate = gate
    val repository = RecordingUsersRepository(listOf(sara))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    viewModel.updateName("Sarah")
    viewModel.signOut()
    advanceUntilIdle()

    // Tries to type and save, then checks that nothing changed
    fun assertEditingIsIgnored() {
      viewModel.updateName("Sarita")
      viewModel.updateSurname("Muller")
      viewModel.saveChanges()
      advanceUntilIdle()
      val state = viewModel.uiState.value
      assertEquals("Sarah", state.name)
      assertEquals(sara.surname, state.surname)
      assertFalse(state.isSaving)
      assertTrue(repository.editedUsers.isEmpty())
    }

    assertEditingIsIgnored()

    gate.complete(Unit)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.signedOut)
    assertEditingIsIgnored()
  }

  /** Test that the error message is removed once cleared. */
  @Test
  fun clearErrorMsgRemovesTheErrorMsg() = runTest {
    val viewModel = createViewModel(uId = "unknown")
    advanceUntilIdle()
    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }
}
