// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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

/** Wiring only: the greeting shows the name and the button signs out. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greetsTheSignedInPerson() {
    val viewModel = HomeViewModel(FakeAuthRepository(FakeAuthRepository.ADA))
    composeTestRule.setContent {
      HomeScreen(credentialManager = FakeCredentialManager.create(), viewModel = viewModel)
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HomeScreenTestTags.USER_NAME)
        .assertTextContains("Ada", substring = true)
  }

  @Test
  fun tappingSignOutCallsBack() {
    var signedOut = false
    val viewModel = HomeViewModel(FakeAuthRepository(FakeAuthRepository.ADA))
    composeTestRule.setContent {
      HomeScreen(
          credentialManager = FakeCredentialManager.create(),
          viewModel = viewModel,
          onSignedOut = { signedOut = true },
      )
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.SIGN_OUT_BUTTON).performClick()

    composeTestRule.waitUntil { signedOut }
    assertTrue(signedOut)
  }
}
