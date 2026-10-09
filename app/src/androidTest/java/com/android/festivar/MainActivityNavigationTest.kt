// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun loginNavigatesToSignUpAndBackToLogin() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          initialDestination = AppScreens.Login.name,
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON).performClick()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.CREATE_ACC_TITLE).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  // TODO when screen is actually in navigation, delete this test and use the real screen instead of
  // the placeholder
  @Test
  fun eventOverviewLogoutSignsOutAndReturnsToLogin() {
    val authRepository = SuccessfulSignOutRepository()

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = authRepository,
          initialDestination = AppScreens.EventOverview.name,
      )
    }

    composeTestRule
        .onNodeWithText("Hello event overview, click to logout!")
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  private class SuccessfulSignOutRepository : AuthRepository {
    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException())

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException())

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException())
  }
}
