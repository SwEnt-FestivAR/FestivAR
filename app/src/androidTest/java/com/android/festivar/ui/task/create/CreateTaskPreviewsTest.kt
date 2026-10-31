// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the previews of the Create task screen, the ones the Figma comparison and the PR
 * screenshots are made from, so that a preview that no longer compiles or crashes when drawn is
 * caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class CreateTaskPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun checkPeopleNeededIs(value: Int) {
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_FIELD)
        .performScrollTo()
        .assertIsDisplayed()
        .assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value.toString())
        )
  }

  // The empty form starts with one person, so the minus button of the stepper is disabled.
  @Test
  fun emptyPreview_showsTheDefaults() {
    composeTestRule.setContent { CreateTaskEmptyPreview() }

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.PRIORITY_SELECTOR)
        .performScrollTo()
        .assertIsDisplayed()
    checkPeopleNeededIs(1)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.MAX_ASSIGN_DECREASE_BUTTON)
        .performScrollTo()
        .assertIsNotEnabled()
  }

  // The filled form shows the three new fields with their values.
  @Test
  fun filledPreview_showsTheNewFields() {
    composeTestRule.setContent { CreateTaskFilledPreview() }

    composeTestRule.onNodeWithText("Run power to stage").assertIsDisplayed()
    checkPeopleNeededIs(3)
    composeTestRule.onNodeWithText("90").performScrollTo().assertIsDisplayed()
  }

  // The errors preview shows the title error and the estimated time error, and no error for the
  // number of people.
  @Test
  fun errorsPreview_showsTheTwoErrors() {
    composeTestRule.setContent { CreateTaskErrorsPreview() }

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_ERROR)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.ESTIMATED_TIME_ERROR)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextEquals(string(R.string.field_error_invalid_estimated_time))
  }

  // The dialog preview shows the dialog with both buttons over the form.
  @Test
  fun discardDialogPreview_showsTheDialog() {
    composeTestRule.setContent { CreateTaskDiscardDialogPreview() }

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.KEEP_EDITING_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.DISCARD_BUTTON).assertIsDisplayed()
  }
}
