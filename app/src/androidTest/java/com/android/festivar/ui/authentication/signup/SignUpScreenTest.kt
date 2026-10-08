// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: SignUpViewModel
  private lateinit var authRepository: FakeAuthRepository

  @Before
  fun setUp() {
    authRepository = FakeAuthRepository()
    viewModel = SignUpViewModel(authRepository)
  }

  @Test
  fun signUpScreen_displaysTaggedTitleAndFields() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          viewModel = viewModel,
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
          viewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGNUP_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun signUpButton_callsViewModelWithEnteredEmailAndPassword() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          viewModel = viewModel,
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
    composeTestRule.waitForIdle()

    assertEquals(
        "user@example.com" to "secure-password",
        authRepository.lastSignUpCredentials,
    )
  }

  @Test
  fun confirmPassword_mismatchShowsErrorMessage() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          viewModel = viewModel,
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
  fun signUpButton_isDisabledWhenPasswordsDoNotMatch() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          viewModel = viewModel,
      )
    }

    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD)
        .performTextInput("user@example.com")
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASS_FIELD).performTextInput("password")
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.PASS_CONFIRM_FIELD)
        .performTextInput("different")

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.SIGNUP_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun passwordVisibilityButton_togglesShowAndHideLabels() {
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          viewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithContentDescription("Show password").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithContentDescription("Hide password").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithContentDescription("Show password").assertIsDisplayed()
  }

  @Test
  fun googleSignUpButton_callsViewModel() {
    val credentialManager = mockk<CredentialManager>(relaxed = true)
    val credential = mockk<Credential>()
    val response = mockk<GetCredentialResponse>()
    coEvery { credentialManager.getCredential(any(), any<GetCredentialRequest>()) } returns response
    every { response.credential } returns credential
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = credentialManager,
          viewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.GOOGLE_SIGNUP_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertSame(credential, authRepository.lastGoogleCredential)
  }

  @Test
  fun backButton_invokesBackCallback() {
    var callbackInvoked = false
    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          onBackClick = { callbackInvoked = true },
          viewModel = viewModel,
      )
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assertTrue(callbackInvoked)
  }

  @Test
  fun loginNowButton_invokesSignInCallback() {
    var callbackInvoked = false

    composeTestRule.setContent {
      SignUpScreen(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          onSignInClick = { callbackInvoked = true },
          viewModel = viewModel,
      )
    }

    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    assertTrue(callbackInvoked)
  }

  private class FakeAuthRepository : AuthRepository {
    var lastSignUpCredentials: Pair<String, String>? = null
      private set

    var lastGoogleCredential: Credential? = null
      private set

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> {
      lastSignUpCredentials = email to password
      return Result.failure(UnsupportedOperationException("Not configured"))
    }

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
      lastGoogleCredential = credential
      return Result.failure(UnsupportedOperationException("Not configured"))
    }

    override suspend fun signOut(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Not configured"))

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not configured"))
  }
}
