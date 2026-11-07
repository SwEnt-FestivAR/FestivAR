// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyStateTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Test that the title, the subtitle and the given actions are shown, and the actions work. */
  @Test
  fun showsTheTextsAndTheActions() {
    var clicks = 0
    composeTestRule.setContent {
      AppTheme {
        EmptyState(icon = R.drawable.ic_event, title = "No items yet", subtitle = "Add one.") {
          Button(onClick = { clicks++ }) { Text(text = "Add") }
        }
      }
    }

    composeTestRule
        .onNodeWithTag(EmptyStateTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("No items yet")
    composeTestRule.onNodeWithText("Add one.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Add").assertIsDisplayed().performClick()
    assertEquals(1, clicks)
  }

  /** Test that, without actions, only the title and the subtitle are shown, with no button. */
  @Test
  fun withoutActionsShowsOnlyTheTexts() {
    composeTestRule.setContent {
      AppTheme {
        EmptyState(icon = R.drawable.ic_event, title = "No items yet", subtitle = "Add one.")
      }
    }

    composeTestRule.onNodeWithTag(EmptyStateTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Add one.").assertIsDisplayed()
    composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
  }
}
