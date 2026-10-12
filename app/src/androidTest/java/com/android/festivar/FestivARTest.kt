// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class FestivARTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun createsFestivARWithInitialDestination() {
    composeTestRule.activity.setContent {
      FestivAR(
          credentialManager = mockk(relaxed = true),
      )
    }
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON).performClick()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.CREATE_ACC_TITLE).assertIsDisplayed()
  }
}
