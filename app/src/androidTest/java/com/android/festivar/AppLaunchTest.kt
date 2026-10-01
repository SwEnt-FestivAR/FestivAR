// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepositoryProvider
import com.android.festivar.ui.authentication.SignInScreenTestTags
import com.android.festivar.utils.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Launches the real Activity, theme and navigation graph. Nobody is signed in, so the sign-in
 * screen must be the first thing on screen. The fake repository is installed before the rule
 * launches the Activity, which is why it sits in an init block and not in a @Before method.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
  init {
    AuthRepositoryProvider.repository = FakeAuthRepository()
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun aFreshStartOpensOnTheSignInScreen() {
    composeTestRule.onNodeWithTag(SignInScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).assertIsDisplayed()
  }
}
