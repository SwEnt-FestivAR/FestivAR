// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun showsText() {
    composeTestRule.setContent { AppTheme { Avatar(text = "SK") } }
    composeTestRule.onNodeWithText("SK").assertIsDisplayed()
  }
}
