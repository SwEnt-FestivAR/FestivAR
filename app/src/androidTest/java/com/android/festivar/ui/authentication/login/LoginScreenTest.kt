// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.Credential
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val viewModel = LoginViewModel(UnusedAuthRepository)

  @Test
  fun displaysLoginFieldsAndMasksPassword() {
    composeTestRule.setContent {
      LoginScreen(
          viewModel = viewModel,
          onSignUpClick = {},
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.EMAIL_INPUT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT)
        .assertIsDisplayed()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Password, Unit))
    composeTestRule.onNodeWithTag(LoginScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.GOOGLE_BUTTON).assertIsDisplayed()
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
    composeTestRule.onNodeWithText("Not used in this UI test").assertIsDisplayed()
  }

  @Test
  fun googleSignInButtonIsDisabledWhileAnotherSignInIsLoading() {
    val authRepository = DeferredAuthRepository()
    val loadingViewModel = LoginViewModel(authRepository)
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
    authRepository.signInResult.complete(
        Result.failure(UnsupportedOperationException("Not used in this UI test"))
    )
  }
}

private class DeferredAuthRepository : AuthRepository {
  val signInResult = CompletableDeferred<Result<FirebaseUser>>()

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      Result.failure(UnsupportedOperationException("Not used in this UI test"))

  override suspend fun signOut(): Result<Unit> =
      Result.failure(UnsupportedOperationException("Not used in this UI test"))

  override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> = signInResult.await()

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> =
      Result.failure(UnsupportedOperationException("Not used in this UI test"))
}

private object UnusedAuthRepository : AuthRepository {
  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      Result.failure(UnsupportedOperationException("Not used in this UI test"))

  override suspend fun signOut(): Result<Unit> =
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
