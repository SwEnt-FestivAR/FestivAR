// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.create.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the previews of the Edit task screen, the ones the Figma comparison is made from, so that
 * a preview that crashes when drawn is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class EditTaskPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // Breaks if the loading state draws the form instead of the spinner.
  @Test
  fun loadingPreview_showsTheSpinner() {
    composeTestRule.setContent { EditTaskLoadingPreview() }

    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOADING).assertIsDisplayed()
  }

  // Breaks if the load error or its retry button is missing.
  @Test
  fun loadErrorPreview_showsTheErrorAndTheRetry() {
    composeTestRule.setContent { EditTaskLoadErrorPreview() }

    composeTestRule
        .onNodeWithTag(EditTaskScreenTestTags.LOAD_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.edit_task_error_load_failed))
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.LOAD_RETRY).assertIsDisplayed()
  }

  // Breaks if the form keeps the Create task title and button text.
  @Test
  fun loadedPreview_showsTheEditLabelsAndAnEnabledSave() {
    composeTestRule.setContent { EditTaskLoadedPreview() }

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE)
        .assertTextEquals(string(R.string.edit_task))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON)
        .assertTextEquals(string(R.string.edit_task_save))
        .assertIsEnabled()
  }

  // Breaks if the slots error is not shown under the people needed.
  @Test
  fun errorsPreview_showsTheTitleAndSlotsErrors() {
    composeTestRule.setContent { EditTaskErrorsPreview() }

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_ERROR)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.field_error_too_few_slots))
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
  }

  // Breaks if the discard dialog preview loses its buttons.
  @Test
  fun discardDialogPreview_showsTheDialog() {
    composeTestRule.setContent { EditTaskDiscardDialogPreview() }

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_BUTTON).assertIsDisplayed()
  }
}
