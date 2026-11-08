// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyTasksPlaceholderScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  // Breaks if the preview stops rendering the placeholder or its My tasks title.
  @Test
  fun thePreviewShowsTheMyTasksTitle() {
    composeTestRule.setContent { MyTasksPlaceholderScreenPreview() }

    composeTestRule.onNodeWithTag(MyTasksPlaceholderTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithText("My tasks").assertIsDisplayed()
  }
}
