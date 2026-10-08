// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateTaskSavingTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun show(state: CreateTaskUiState) {
    composeTestRule.setContent { AppTheme { CreateTaskContent(state, CreateTaskActions()) } }
  }

  // While the task is being saved, the spinner replaces the button so it cannot be tapped again.
  @Test
  fun whileSaving_aSpinnerReplacesTheCreateButton() {
    show(CreateTaskUiState(title = "Run power to stage", isSaving = true))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SAVING_INDICATOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertDoesNotExist()
  }

  // Counterpart of the test above: when nothing is being saved, the button is shown, no spinner.
  @Test
  fun whenNotSaving_theCreateButtonIsShownWithoutASpinner() {
    show(CreateTaskUiState(title = "Run power to stage"))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SAVING_INDICATOR).assertDoesNotExist()
  }
}
