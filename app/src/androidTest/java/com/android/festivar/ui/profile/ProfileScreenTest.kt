// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.profile

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.credentials.Credential
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring only: the screen shows the ViewModel's state and calls back. The rules are unit tested in
 * ProfileViewModelTest.
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val yasser = User(uid = ME, name = "Ameur", surname = "Yasser", email = "yasser@epfl.ch")

  private val users = UsersRepositoryLocal(listOf(yasser))

  /** A users repository whose saves fail with `offline`. */
  private fun offlineUsers() =
      object : UsersRepository by users {
        override suspend fun editUser(uId: String, newValue: User) =
            throw IllegalStateException("offline")
      }

  /** A users repository whose fetch never ends, so the profile stays loading. */
  private fun neverLoadingUsers() =
      object : UsersRepository by users {
        override suspend fun getUser(uId: String): User = awaitCancellation()
      }

  /** A users repository whose first fetch fails with `offline`, and whose next fetches work. */
  private fun usersFailingTheFirstLoad() =
      object : UsersRepository by users {
        private var failed = false

        override suspend fun getUser(uId: String): User {
          if (!failed) {
            failed = true
            throw IllegalStateException("offline")
          }
          return users.getUser(uId)
        }
      }

  /** An [AuthRepository] whose sign out never ends, so the profile stays signing out. */
  private fun neverSigningOut() =
      object : AuthRepository by FakeAuthRepository() {
        override suspend fun signOut(): Result<Unit> = awaitCancellation()
      }

  /** An [AuthRepository] whose sign out returns [signOutResult]. */
  private class FakeAuthRepository(var signOutResult: Result<Unit> = Result.success(Unit)) :
      AuthRepository {
    override suspend fun signOut(): Result<Unit> = signOutResult

    override suspend fun forgotPassword(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Not used in this UI test"))

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used in this UI test"))

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used in this UI test"))

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used in this UI test"))
  }

  /**
   * Shows the profile of [ME] on [usersRepository], and waits until it is loaded unless
   * [waitForLoad] is false. Returns the ViewModel, so the tests can read its state.
   */
  private fun setProfile(
      usersRepository: UsersRepository = users,
      authRepository: AuthRepository = FakeAuthRepository(),
      onBack: () -> Unit = {},
      onSignedOut: () -> Unit = {},
      waitForLoad: Boolean = true,
  ): ProfileViewModel {
    val viewModel = ProfileViewModel(usersRepository, authRepository, ME)
    composeTestRule.setContent {
      ProfileScreen(profileViewModel = viewModel, onBack = onBack, onSignedOut = onSignedOut)
    }
    if (waitForLoad) waitForTag(ProfileScreenTestTags.AVATAR)
    return viewModel
  }

  private fun waitForTag(tag: String) =
      composeTestRule.waitUntil(5_000) {
        composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
      }

  private fun saveButton() = composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON)

  private fun string(@StringRes id: Int) =
      InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

  /** Turns red when the title, the back arrow, the avatar, a field or a button disappears. */
  @Test
  fun everyPartOfTheProfileShows() {
    setProfile()

    listOf(
            ProfileScreenTestTags.TITLE,
            ProfileScreenTestTags.BACK,
            ProfileScreenTestTags.AVATAR,
            ProfileScreenTestTags.EMAIL,
            ProfileScreenTestTags.SURNAME_FIELD,
            ProfileScreenTestTags.NAME_FIELD,
            ProfileScreenTestTags.SAVE_BUTTON,
            ProfileScreenTestTags.SIGN_OUT_BUTTON,
        )
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertIsDisplayed() }
  }

  /** Turns red when the fields stop showing the loaded user. */
  @Test
  fun theFieldsShowTheLoadedUser() {
    setProfile()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EMAIL).assertTextContains(yasser.email)
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.SURNAME_FIELD)
        .assertTextContains(yasser.surname)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).assertTextContains(yasser.name)
  }

  /** Turns red when the email becomes a field the user can type into. */
  @Test
  fun theEmailCannotBeEdited() {
    setProfile()

    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EMAIL)
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
  }

  /** Turns red when the spinner stops showing during the load, or the form shows before it ends. */
  @Test
  fun whileLoadingOnlyTheSpinnerShows() {
    setProfile(usersRepository = neverLoadingUsers(), waitForLoad = false)

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).assertDoesNotExist()
    saveButton().assertDoesNotExist()
  }

  /** Turns red when Save stops following canSave: off without changes, on once a name changes. */
  @Test
  fun saveIsEnabledOnlyOnceANameChanges() {
    setProfile()
    saveButton().assertIsNotEnabled()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).performTextReplacement("Sami")
    saveButton().assertIsEnabled()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).performTextClearance()
    saveButton().assertIsNotEnabled()
  }

  /**
   * Turns red when a field with invalid characters stops showing as an error with its message, or
   * Save stays enabled, or the error stays once the field is fixed.
   */
  @Test
  fun invalidCharactersShowAnErrorUnderTheirField() {
    setProfile()
    val nameField = composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD)
    val surnameField = composeTestRule.onNodeWithTag(ProfileScreenTestTags.SURNAME_FIELD)
    val isError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_ERROR).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SURNAME_ERROR).assertDoesNotExist()

    nameField.performTextReplacement("Sami😀")
    nameField.assert(isError)
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.NAME_ERROR)
        .assertIsDisplayed()
        .assertTextContains(string(R.string.profile_invalid_name_chars))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SURNAME_ERROR).assertDoesNotExist()
    saveButton().assertIsNotEnabled()

    nameField.performTextReplacement("Sami")
    surnameField.performTextReplacement("Kabbaj2")
    surnameField.assert(isError)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SURNAME_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_ERROR).assertDoesNotExist()
    saveButton().assertIsNotEnabled()

    surnameField.performTextReplacement("Kabbaj")
    surnameField.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SURNAME_ERROR).assertDoesNotExist()
    saveButton().assertIsEnabled()
  }

  /** Turns red when the fields stop reaching the ViewModel or Save stops saving them. */
  @Test
  fun savingStoresTheTypedNames() {
    val viewModel = setProfile()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).performTextReplacement("Sami")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.SURNAME_FIELD)
        .performTextReplacement("Kabbaj")
    saveButton().performClick()

    composeTestRule.waitUntil(5_000) { viewModel.uiState.value.user?.name == "Sami" }
    assertEquals(
        yasser.copy(name = "Sami", surname = "Kabbaj"),
        runBlocking { users.getUser(ME) },
    )
    saveButton().assertIsNotEnabled()
  }

  /** Turns red when a failed save stops showing its error in the snackbar. */
  @Test
  fun failedSaveShowsTheError() {
    setProfile(usersRepository = offlineUsers())

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).performTextReplacement("Sami")
    saveButton().performClick()

    waitForTag(ProfileScreenTestTags.ERROR_MESSAGE)
    composeTestRule.onNodeWithText(string(R.string.profile_save_failed)).assertIsDisplayed()
  }

  /**
   * Turns red when a failed load stops showing its error in the snackbar, or shows the form instead
   * of Retry.
   */
  @Test
  fun failedLoadShowsTheError() {
    setProfile(usersRepository = UsersRepositoryLocal(), waitForLoad = false)

    waitForTag(ProfileScreenTestTags.ERROR_MESSAGE)
    composeTestRule.onNodeWithText(string(R.string.profile_load_failed)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.RETRY_BUTTON).assertIsDisplayed()
    saveButton().assertDoesNotExist()
  }

  /** Turns red when Retry stops loading the profile again after a failed load. */
  @Test
  fun retryAfterAFailedLoadShowsTheProfile() {
    setProfile(usersRepository = usersFailingTheFirstLoad(), waitForLoad = false)
    waitForTag(ProfileScreenTestTags.RETRY_BUTTON)

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.RETRY_BUTTON).performClick()

    waitForTag(ProfileScreenTestTags.AVATAR)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME_FIELD).assertTextContains(yasser.name)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.RETRY_BUTTON).assertDoesNotExist()
  }

  /** Turns red when Sign out stops reaching the auth repository or stops calling onSignedOut. */
  @Test
  fun signOutSignsOutAndCallsOnSignedOut() {
    var signedOut = false
    val viewModel = setProfile(onSignedOut = { signedOut = true })

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).performClick()

    composeTestRule.waitUntil(5_000) { signedOut }
    assertTrue(viewModel.uiState.value.signedOut)
  }

  /** Turns red when Sign out stays enabled while the sign out is running. */
  @Test
  fun signOutIsDisabledWhileSigningOut() {
    setProfile(authRepository = neverSigningOut())

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).performClick()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).assertIsNotEnabled()
  }

  /**
   * Turns red when a failed sign out stops showing its error in the snackbar, or leaves the screen
   * anyway.
   */
  @Test
  fun failedSignOutShowsTheErrorAndStays() {
    var signedOut = false
    val authRepository =
        FakeAuthRepository(signOutResult = Result.failure(IllegalStateException("offline")))
    setProfile(authRepository = authRepository, onSignedOut = { signedOut = true })

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).performClick()

    waitForTag(ProfileScreenTestTags.ERROR_MESSAGE)
    composeTestRule.onNodeWithText(string(R.string.profile_sign_out_failed)).assertIsDisplayed()
    assertFalse(signedOut)
  }

  /** Turns red when the back arrow stops calling onBack. */
  @Test
  fun backCallsOnBack() {
    var back = false
    setProfile(onBack = { back = true })

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.BACK).performClick()

    assertTrue(back)
  }

  private companion object {
    const val ME = "uid-yasser"
  }
}
