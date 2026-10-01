// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.authentication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.utils.FakeAuthRepository
import com.android.festivar.utils.FakeCredentialManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Wiring only: the state reaches the screen and a tap starts the flow. Logic is unit tested. */
@RunWith(AndroidJUnit4::class)
class SignInScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun showsTheTitleAndAnEnabledSignInButton() {
    val viewModel = SignInViewModel(FakeAuthRepository())
    composeTestRule.setContent { SignInScreen(FakeCredentialManager.create(), viewModel) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).assertIsEnabled()
  }

  @Test
  fun tappingTheButtonSignsInAndCallsBack() {
    var signedIn = false
    val viewModel = SignInViewModel(FakeAuthRepository())
    composeTestRule.setContent {
      SignInScreen(
          credentialManager = FakeCredentialManager.create(),
          viewModel = viewModel,
          onSignedIn = { signedIn = true },
      )
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).performClick()

    composeTestRule.waitUntil { signedIn }
    assertTrue(signedIn)
  }

  @Test
  fun aRejectedSignInShowsTheError() {
    val viewModel = SignInViewModel(FakeAuthRepository(fail = true))
    composeTestRule.setContent {
      SignInScreen(credentialManager = FakeCredentialManager.create(), viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).performClick()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }
}
