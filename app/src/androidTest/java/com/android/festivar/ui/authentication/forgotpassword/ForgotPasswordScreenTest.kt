// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.authentication.forgotpassword

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.ui.components.CREDENTIAL_MAX_LENGTH
import kotlinx.coroutines.CompletableDeferred
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
    viewModel = ForgotPasswordViewModel(DataRepositoryLocal(authRepository))
  }

  @Test
  fun screenDisplaysPromptInputAndReturnButton() {
    composeTestRule.setContent { ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = {}) }

    composeTestRule
        .onNodeWithText("Enter your email address to reset your password")
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
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).assertIsNotEnabled()
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
    composeTestRule
        .onNodeWithText("Unable to send password reset email. Please try again.")
        .assertIsDisplayed()
  }

  @Test
  fun pendingRequestDisablesButtonUntilRequestCompletes() {
    val result = CompletableDeferred<Result<Unit>>()
    authRepository.pendingResetResult = result
    composeTestRule.setContent { ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = {}) }
    composeTestRule
        .onNodeWithTag(ForgotPasswordScreenTestTags.EMAIL_INPUT)
        .performTextInput("user@example.com")

    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).performClick()
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SEND_BUTTON).assertIsNotEnabled()

    result.complete(Result.success(Unit))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ForgotPasswordScreenTestTags.SUCCESS_MESSAGE).assertIsDisplayed()
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

  // An email address can be up to 320 characters, far over the 80 default. Breaks if the field
  // keeps the default or loses its limit.
  @Test
  fun emailTakesUpToTheCredentialLimit() {
    composeTestRule.setContent { ForgotPasswordScreen(viewModel = viewModel, onReturnToLogin = {}) }

    composeTestRule
        .onNodeWithTag(ForgotPasswordScreenTestTags.EMAIL_INPUT)
        .performTextInput("a".repeat(CREDENTIAL_MAX_LENGTH + 1))

    composeTestRule.runOnIdle {
      assertEquals(CREDENTIAL_MAX_LENGTH, viewModel.uiState.value.email.length)
    }
  }
}
