// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the previews of the bottom bar, so that a preview that no longer compiles or crashes when
 * drawn is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class BottomNavigationBarPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the default preview no longer shows the three tabs with Events selected. */
  @Test
  fun defaultPreview_showsTheThreeTabsWithEventsSelected() {
    composeTestRule.setContent { BottomNavigationBarPreview() }

    NavigationTab.entries.forEach { tab ->
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(tab))
          .assertIsDisplayed()
    }
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(NavigationTab.EVENTS))
        .assertIsSelected()
  }

  /** Breaks if the event tasks preview no longer shows the tasks badge with no tab selected. */
  @Test
  fun eventTasksPreview_showsTheBadgeWithNoTabSelected() {
    composeTestRule.setContent { BottomNavigationBarEventTasksPreview() }

    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.TASKS_BADGE, useUnmergedTree = true)
        .assertIsDisplayed()
    NavigationTab.entries.forEach { tab ->
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(tab))
          .assertIsNotSelected()
    }
  }
}
