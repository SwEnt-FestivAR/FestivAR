// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepositoryProvider
import com.android.festivar.ui.authentication.SignInScreenTestTags
import com.android.festivar.ui.home.HomeScreenTestTags
import com.android.festivar.utils.FakeAuthRepository
import com.android.festivar.utils.FakeCredentialManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sign-in flow through the real navigation graph: launch, sign in, land on Home, sign out, back
 * on sign-in. Authentication is faked so the test needs no Google account.
 */
@RunWith(AndroidJUnit4::class)
class FestivARAppTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var auth: FakeAuthRepository

  @Before
  fun installFakes() {
    auth = FakeAuthRepository()
    AuthRepositoryProvider.repository = auth
  }

  @Test
  fun aNewPersonSignsInReachesHomeAndSignsOutAgain() {
    composeTestRule.setContent {
      FestivARApp(credentialManager = FakeCredentialManager.create(), authRepository = auth)
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(HomeScreenTestTags.USER_NAME).assertIsDisplayed()

    composeTestRule.onNodeWithTag(HomeScreenTestTags.SIGN_OUT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun aSignedInPersonStartsOnHome() {
    auth = FakeAuthRepository(FakeAuthRepository.ADA)
    AuthRepositoryProvider.repository = auth

    composeTestRule.setContent {
      FestivARApp(credentialManager = FakeCredentialManager.create(), authRepository = auth)
    }

    composeTestRule.onNodeWithTag(HomeScreenTestTags.TITLE).assertIsDisplayed()
  }
}
