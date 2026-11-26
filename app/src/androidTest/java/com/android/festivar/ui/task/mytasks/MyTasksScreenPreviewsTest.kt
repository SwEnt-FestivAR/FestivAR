// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/**
 * Renders the previews of My tasks, so that a preview that crashes when drawn is caught by the
 * tests and not only in Android Studio.
 */
class MyTasksScreenPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the list preview stops showing the tasks of today and those without a start. */
  @Test
  fun listPreview_showsTheTasksGroupedByDay() {
    composeTestRule.setContent { MyTasksListPreview() }

    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MyTasksScreenTestTags.section(LocalDate.now()))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.row("t2")).assertIsDisplayed()
  }

  /** Breaks if the empty preview stops showing the "No tasks yet" state. */
  @Test
  fun emptyPreview_showsTheEmptyState() {
    composeTestRule.setContent { MyTasksEmptyPreview() }

    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.EMPTY).assertIsDisplayed()
  }

  /** Breaks if the no match preview stops offering "Clear filters" with the search open. */
  @Test
  fun noMatchPreview_offersToClearTheFilters() {
    composeTestRule.setContent { MyTasksNoMatchPreview() }

    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.SEARCH_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.NO_MATCH).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.CLEAR_FILTERS).assertIsDisplayed()
  }

  /** Breaks if the refresh error preview hides the list or loses the Retry snackbar. */
  @Test
  fun refreshErrorPreview_keepsTheListUnderTheRetrySnackbar() {
    composeTestRule.setContent { MyTasksRefreshErrorPreview() }

    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.RETRY).assertIsDisplayed()
  }
}
