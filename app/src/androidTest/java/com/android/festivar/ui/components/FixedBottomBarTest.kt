// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FixedBottomBarTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the bar no longer shows every line placed in it. */
  @Test
  fun showsItsContent() {
    composeTestRule.setContent {
      AppTheme {
        FixedBottomBar {
          Text("first line")
          Text("second line")
        }
      }
    }

    composeTestRule.onNodeWithText("first line").assertIsDisplayed()
    composeTestRule.onNodeWithText("second line").assertIsDisplayed()
  }

  /**
   * Breaks if the preview no longer compiles or draws, which Android Studio alone would not catch.
   */
  @Test
  fun preview_showsTheBar() {
    composeTestRule.setContent { FixedBottomBarPreview() }

    composeTestRule.onNodeWithText("First line of the bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second line of the bar").assertIsDisplayed()
  }
}
