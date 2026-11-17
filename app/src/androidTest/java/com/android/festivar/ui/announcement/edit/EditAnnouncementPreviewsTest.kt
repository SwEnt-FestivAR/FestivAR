// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.announcement.edit.EditAnnouncementScreenTestTags as Tags
import org.junit.Rule
import org.junit.Test

/**
 * Renders the four previews of the Edit announcement screen, so that a preview that crashes when
 * drawn is caught by the tests and not only in Android Studio. Only the filled form is compared
 * with the Figma.
 */
class EditAnnouncementPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // Breaks if the preview stops showing the filled form of the Figma, with an edit ready to save.
  @Test
  fun preview_showsTheFilledFormWithAnEditReadyToSave() {
    composeTestRule.setContent { EditAnnouncementContentPreview() }

    composeTestRule.onNodeWithText("Stage B is closed").assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.priorityChip(AnnouncementPriority.URGENT)).assertIsSelected()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertIsEnabled()
  }

  // Breaks if the preview stops showing both field errors, with Save locked.
  @Test
  fun fieldErrorsPreview_showsBothErrorsAndADisabledSave() {
    composeTestRule.setContent { EditAnnouncementContentFieldErrorsPreview() }

    composeTestRule.onNodeWithTag(Tags.TITLE_ERROR).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.DESCRIPTION_ERROR).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertIsNotEnabled()
  }

  // Breaks if the preview stops showing the save error, or shows it with Save locked.
  @Test
  fun saveErrorPreview_showsTheErrorAndAnEnabledSave() {
    composeTestRule.setContent { EditAnnouncementContentSaveErrorPreview() }

    composeTestRule.onNodeWithTag(Tags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertIsEnabled()
  }

  // Breaks if the preview stops showing the load error with its retry, or draws a form with it.
  @Test
  fun loadErrorPreview_showsTheErrorAndTheRetryWithoutAForm() {
    composeTestRule.setContent { EditAnnouncementContentLoadErrorPreview() }

    composeTestRule.onNodeWithTag(Tags.LOAD_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.LOAD_RETRY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(Tags.TITLE_FIELD).assertDoesNotExist()
    composeTestRule.onNodeWithTag(Tags.SAVE_BUTTON).assertDoesNotExist()
  }
}
