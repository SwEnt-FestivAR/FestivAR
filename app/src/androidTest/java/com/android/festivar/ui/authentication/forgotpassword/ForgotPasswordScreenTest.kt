// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.forgotpassword

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.Credential
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForgotPasswordScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var authRepository: FakeAuthRepository
  private lateinit var viewModel: ForgotPasswordViewModel

  @Before
  fun setUp() {
    authRepository = FakeAuthRepository()
    viewModel = ForgotPasswordViewModel(authRepository)
  }

  @Test
  fun screenDisplaysPromptInputAndReturnButton() {
    composeTestRule.setContent { ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = {}) }

    composeTestRule
        .onNodeWithText("Enter your email address to retrieve your password")
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.EMAIL_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.RETURN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun enteringEmailEnablesButtonAndShowsSuccessAfterRequest() {
    composeTestRule.setContent { ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = {}) }

    composeTestRule
        .onNodeWithTag(ForgotPasswordScreenTestTags.EMAIL_INPUT)
        .performTextInput("  user@example.com  ")
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals("user@example.com", authRepository.lastResetEmail)
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SUCCESS_MESSAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(
            "If an account exists for this email, you’ll receive a password reset email."
        )
        .assertIsDisplayed()
  }

  @Test
  fun failedRequestDisplaysError() {
    authRepository.resetResult = Result.failure(IllegalArgumentException("Request failed"))
    composeTestRule.setContent { ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = {}) }
    composeTestRule
        .onNodeWithTag(ForgotPasswordScreenTestTags.EMAIL_INPUT)
        .performTextInput("user@example.com")

    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Request failed").assertIsDisplayed()
  }

  @Test
  fun returnButtonInvokesCallback() {
    var returnedToLogin = false
    composeTestRule.setContent {
      ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = { returnedToLogin = true })
    }

    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.RETURN_BUTTON).performClick()

    assertTrue(returnedToLogin)
  }

  private class FakeAuthRepository : AuthRepository {
    var lastResetEmail: String? = null
      private set

    var resetResult: Result<Unit> = Result.success(Unit)

    override suspend fun forgotPassword(email: String): Result<Unit> {
      lastResetEmail = email
      return resetResult
    }

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used in this test"))

    override suspend fun signOut(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Not used in this test"))

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used in this test"))

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used in this test"))
  }
}
