// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.festivar.ui.components.EmptyStateTestTags
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/**
 * Renders the two previews of the announcement feed, so that a preview that crashes when drawn is
 * caught by the tests and not only in Android Studio.
 */
class AnnouncementFeedPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // Breaks if the preview stops showing the feed of the Figma: a pinned announcement, then today's
  // announcements under their header, with the add button.
  @Test
  fun preview_showsTheFeedOfTheFigma() {
    composeTestRule.setContent { AnnouncementFeedContentPreview() }

    composeTestRule.onNodeWithText("Gates and opening hours").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.dateHeader(LocalDate.of(2026, 10, 17)))
        .assertTextEquals("TODAY · SAT 17 OCT")
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.ADD_ANNOUNCEMENT_FAB)
        .assertIsDisplayed()
  }

  // Breaks if the empty preview stops showing the empty state of the feed.
  @Test
  fun emptyPreview_showsTheEmptyState() {
    composeTestRule.setContent { AnnouncementFeedEmptyPreview() }

    composeTestRule
        .onNodeWithTag(EmptyStateTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("No announcements yet")
  }
}
