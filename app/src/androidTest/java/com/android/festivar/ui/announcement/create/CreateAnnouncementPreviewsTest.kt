// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.festivar.model.event.announcement.AnnouncementPriority
import org.junit.Rule
import org.junit.Test

/**
 * Renders the preview of the Create announcement screen, the one compared with the Figma, so that a
 * preview that crashes when drawn is caught by the tests and not only in Android Studio.
 */
class CreateAnnouncementPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // Breaks if the preview stops showing the filled form of the Figma, with Publish ready to tap.
  @Test
  fun preview_showsTheFilledFormOfTheFigma() {
    composeTestRule.setContent { CreateAnnouncementContentPreview() }

    composeTestRule.onNodeWithText("Stage B is closed").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateAnnouncementScreenTestTags.priorityChip(AnnouncementPriority.URGENT))
        .assertIsSelected()
    composeTestRule.onNodeWithTag(CreateAnnouncementScreenTestTags.PUBLISH_BUTTON).assertIsEnabled()
  }
}
