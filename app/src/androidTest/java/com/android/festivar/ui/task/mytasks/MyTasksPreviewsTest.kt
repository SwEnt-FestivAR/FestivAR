// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the preview of My tasks, so that a preview that no longer compiles or crashes when drawn
 * is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class MyTasksPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // The list preview shows its three rows under today and tomorrow.
  @Test
  fun listPreview_showsItsRows() {
    composeTestRule.setContent { MyTasksListPreview() }

    listOf("desk", "power", "wrist").forEach {
      composeTestRule.onNodeWithTag(MyTasksScreenTestTags.row(it)).assertIsDisplayed()
    }
  }
}
