// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the previews of the shared form components, so that a preview that no longer compiles or
 * crashes when drawn is caught by the tests and not only in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class ComponentPreviewsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // The form fields preview renders without crashing and shows a value, an error and a picker.
  @Test
  fun formFieldsPreview_showsEveryKindOfField() {
    composeTestRule.setContent { FormFieldsPreview() }

    composeTestRule.onNodeWithText("Run power to stage").assertIsDisplayed()
    composeTestRule.onNodeWithText("Title cannot be empty").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sat 17 Oct").assertIsDisplayed()
  }

  // The date dialog preview renders without crashing and shows its OK and Cancel buttons.
  @Test
  fun datePickerDialogPreview_showsTheDialog() {
    composeTestRule.setContent { FestivarDatePickerDialogPreview() }

    composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  // The time dialog preview renders without crashing and shows its OK and Cancel buttons.
  @Test
  fun timePickerDialogPreview_showsTheDialog() {
    composeTestRule.setContent { FestivarTimePickerDialogPreview() }

    composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }
}
