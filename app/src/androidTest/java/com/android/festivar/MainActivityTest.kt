// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import io.mockk.mockk
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun rendersLoginScreen() {
    val authRepository = FakeAuthRepository(currentUser = null)

    composeTestRule.activity.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = authRepository,
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @After
  fun tearDown() {
    Firebase.auth.signOut()
  }

  companion object {
    @BeforeClass
    @JvmStatic
    fun signOut() {
      Firebase.auth.signOut()
    }
  }
}
