// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.authentication.login

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val viewModel = LoginViewModel(DataRepositoryLocal(FakeAuthRepository()))

  @Test
  fun displaysLoginFieldsAndMasksPassword() {
    composeTestRule.setContent {
      LoginScreen(
          viewModel = viewModel,
          onSignUpClick = {},
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.CATCHPHRASE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Every table in the right place.").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Create events and tasks, and keep everything in one place.")
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.EMAIL_INPUT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT)
        .assertIsDisplayed()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
    composeTestRule.onNodeWithTag(LoginScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.GOOGLE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.ANONYMOUS_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun textInputUpdatesViewModelAndRejectedSignInShowsError() {
    composeTestRule.setContent {
      LoginScreen(
          viewModel = viewModel,
          onSignUpClick = {},
      )
    }

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.EMAIL_INPUT)
        .performTextInput("user@example.com")
    composeTestRule.onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT).performTextInput("secret")
    composeTestRule.runOnIdle {
      assertEquals("user@example.com", viewModel.uiState.value.email)
      assertEquals("secret", viewModel.uiState.value.password)
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Unable to sign in. Check your details and try again.")
        .assertIsDisplayed()
  }

  @Test
  fun passwordDoneImeActionSignsIn() {
    val loginViewModel = LoginViewModel(DataRepositoryLocal(FakeAuthRepository()))
    composeTestRule.setContent {
      LoginScreen(
          viewModel = loginViewModel,
          onSignUpClick = {},
      )
    }

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.EMAIL_INPUT)
        .performTextInput("user@example.com")
    composeTestRule.onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT).performTextInput("secret")
    composeTestRule.onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT).performImeAction()

    composeTestRule
        .onNodeWithText("Unable to sign in. Check your details and try again.")
        .assertIsDisplayed()
  }

  @Test
  fun createAccountFooterUsesButtonAndNavigatesOnClick() {
    var signUpClicked = false
    val loginViewModel = LoginViewModel(DataRepositoryLocal(FakeAuthRepository()))
    composeTestRule.setContent {
      LoginScreen(
          viewModel = loginViewModel,
          onSignUpClick = { signUpClicked = true },
      )
    }

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .performScrollTo()
        .performClick()

    composeTestRule.runOnIdle { assertTrue(signUpClicked) }
  }

  @Test
  fun forgotPasswordButtonInvokesCallback() {
    var forgotPasswordClicked = false
    val loginViewModel = LoginViewModel(DataRepositoryLocal(FakeAuthRepository()))
    composeTestRule.setContent {
      LoginScreen(
          viewModel = loginViewModel,
          onSignUpClick = {},
          onForgotPasswordClick = { forgotPasswordClicked = true },
      )
    }

    composeTestRule.onNodeWithText("Forgot password?").performClick()

    composeTestRule.runOnIdle { assertTrue(forgotPasswordClicked) }
  }

  @Test
  fun googleSignInButtonIsDisabledWhileAnotherSignInIsLoading() {
    val authRepository = FakeAuthRepository(signInDeferred = CompletableDeferred())
    val loadingViewModel = LoginViewModel(DataRepositoryLocal(authRepository))
    composeTestRule.setContent {
      LoginScreen(
          viewModel = loadingViewModel,
          onSignUpClick = {},
      )
    }

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.EMAIL_INPUT)
        .performTextInput("user@example.com")
    composeTestRule.onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT).performTextInput("secret")
    composeTestRule.onNodeWithTag(LoginScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.GOOGLE_BUTTON).assertIsNotEnabled()
    authRepository.signInDeferred?.complete(
        Result.failure(UnsupportedOperationException("Not used in this UI test"))
    )
  }

  @Test
  fun anonymousSignInButtonSignsIn() {
    val user = mockk<FirebaseUser>()
    val authRepository = FakeAuthRepository(signInAnonymousResult = Result.success(user))
    val loginViewModel = LoginViewModel(DataRepositoryLocal(authRepository))
    var signedIn = false

    composeTestRule.setContent {
      LoginScreen(
          viewModel = loginViewModel,
          onSignUpClick = {},
          onSignedIn = { signedIn = true },
      )
    }

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.ANONYMOUS_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.runOnIdle { assertTrue(!loginViewModel.uiState.value.isSignedIn) }

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.ANONYMOUS_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.onNodeWithText("Continue anyway").performClick()

    composeTestRule.runOnIdle {
      assertTrue(signedIn)
      assertTrue(loginViewModel.uiState.value.isSignedIn)
    }
  }
}
