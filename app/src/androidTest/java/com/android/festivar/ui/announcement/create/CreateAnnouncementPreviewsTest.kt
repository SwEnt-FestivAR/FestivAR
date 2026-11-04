// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.festivar.model.event.announcement.AnnouncementPriority
import org.junit.Rule
import org.junit.Test

/**
 * Renders the three previews of the Create announcement screen, so that a preview that crashes when
 * drawn is caught by the tests and not only in Android Studio. Only the filled form is compared
 * with the Figma.
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

  // Breaks if the preview stops showing a running save: the spinner, with Publish locked.
  @Test
  fun savingPreview_showsTheIndicatorAndADisabledPublish() {
    composeTestRule.setContent { CreateAnnouncementContentSavingPreview() }

    composeTestRule
        .onNodeWithTag(CreateAnnouncementScreenTestTags.SAVING_INDICATOR, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateAnnouncementScreenTestTags.PUBLISH_BUTTON)
        .assertIsNotEnabled()
  }

  // Breaks if the preview stops showing both field errors, with Publish locked.
  @Test
  fun fieldErrorsPreview_showsBothErrorsAndADisabledPublish() {
    composeTestRule.setContent { CreateAnnouncementContentFieldErrorsPreview() }

    composeTestRule.onNodeWithTag(CreateAnnouncementScreenTestTags.TITLE_ERROR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateAnnouncementScreenTestTags.DESCRIPTION_ERROR)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateAnnouncementScreenTestTags.PUBLISH_BUTTON)
        .assertIsNotEnabled()
  }
}
