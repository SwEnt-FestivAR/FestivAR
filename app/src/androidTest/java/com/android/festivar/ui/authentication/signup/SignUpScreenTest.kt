// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: SignUpViewModel

  @Before
  fun setUp() {
    viewModel = mockk(relaxed = true)
    every { viewModel.uiState } returns MutableStateFlow(AuthUIState())
  }

  @Test
  fun signUpScreen_displaysTaggedTitleAndFields() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          signUpViewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.CREATE_ACC_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASS_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASS_CONFIRM_FIELD).assertIsDisplayed()
  }

  @Test
  fun signUpButton_isDisabledWhenRequiredFieldsAreBlank() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          signUpViewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGNUP_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun signUpButton_callsViewModelWithEnteredEmailAndPassword() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          signUpViewModel = viewModel,
      )
    }

    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD)
        .performTextInput("user@example.com")
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.PASS_FIELD)
        .performTextInput("secure-password")
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.PASS_CONFIRM_FIELD)
        .performTextInput("secure-password")
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGNUP_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGNUP_BUTTON).performClick()

    verify { viewModel.signUp("user@example.com", "secure-password") }
  }

  @Test
  fun confirmPassword_mismatchShowsErrorMessage() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          signUpViewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASS_FIELD).performTextInput("password")
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.PASS_CONFIRM_FIELD)
        .performTextInput("different")

    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.PASSWORD_MISMATCH_ERROR)
        .assertIsDisplayed()
        .assertTextEquals("Passwords do not match")
  }

  @Test
  fun loginNowButton_invokesCreateAccountCallback() {
    var callbackInvoked = false
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          onCreateAccountClick = { callbackInvoked = true },
          signUpViewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON).performClick()

    assertTrue(callbackInvoked)
  }
}
