// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

  /**
   * Test that the title, the subtitle and the given actions are shown and work, and that without
   * actions no button is left.
   */
  @Test
  fun showsTheTextsAndTheActionsOnlyWhenGiven() {
    var clicks = 0
    var withActions by mutableStateOf(true)
    composeTestRule.setContent {
      AppTheme {
        EmptyState(
            icon = R.drawable.ic_event,
            title = "No items yet",
            subtitle = "Add one.",
            actions =
                if (withActions) {
                  { Button(onClick = { clicks++ }) { Text(text = "Add") } }
                } else null,
        )
      }
    }

    composeTestRule
        .onNodeWithTag(EmptyStateTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("No items yet")
    composeTestRule.onNodeWithText("Add one.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Add").assertIsDisplayed().performClick()
    assertEquals(1, clicks)

    withActions = false
    composeTestRule.onNodeWithTag(EmptyStateTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
  }
}
