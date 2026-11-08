// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
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

  /** Breaks if the start screen preview no longer shows the three tabs with Events selected. */
  @Test
  fun defaultPreview_showsTheThreeTabsWithEventsSelected() {
    composeTestRule.setContent { BottomNavigationBarPreview() }

    NavigationTab.entries.forEach { composeTestRule.tab(it).assertIsDisplayed() }
    composeTestRule.tab(NavigationTab.EVENTS).assertIsSelected()
    composeTestRule.tab(NavigationTab.MY_TASKS).assertIsNotSelected()
  }

  /** Breaks if the task board preview no longer nests the Events tab under its disc. */
  @Test
  fun eventTasksPreview_showsTheDiscOnTheEventsTab() {
    composeTestRule.setContent { BottomNavigationBarEventTasksPreview() }

    composeTestRule.subScreen(NavigationSubScreen.EVENT_TASKS).assertIsDisplayed()
    composeTestRule.tab(NavigationTab.EVENTS).assertIsSelected()
  }

  /** Breaks if the task details preview no longer nests the My tasks tab under its disc. */
  @Test
  fun myTaskDetailsPreview_showsTheDiscOnTheMyTasksTab() {
    composeTestRule.setContent { BottomNavigationBarMyTaskDetailsPreview() }

    composeTestRule.subScreen(NavigationSubScreen.TASK_DETAILS).assertIsDisplayed()
    composeTestRule.tab(NavigationTab.MY_TASKS).assertIsSelected()
  }
}
