// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InfoNoteTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val events = mutableListOf<String>()

  @Test
  fun showsTheMessageWithoutAction() {
    composeTestRule.setContent { AppTheme { InfoNote("Microphone is off.") } }

    composeTestRule.onNodeWithText("Microphone is off.").assertIsDisplayed()
  }

  @Test
  fun showsItsActionUnderTheMessage() {
    composeTestRule.setContent {
      AppTheme {
        InfoNote(
            "Microphone is off.",
            action = { TextButton(onClick = { events += "settings" }) { Text("Open settings") } },
        )
      }
    }

    composeTestRule.onNodeWithText("Microphone is off.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Open settings").performClick()

    assertEquals(listOf("settings"), events)
  }

  // The preview renders without crashing, so that one that no longer compiles is caught here and
  // not only in Android Studio.
  @Test
  fun preview_showsTheNoteAndItsAction() {
    composeTestRule.setContent { InfoNotePreview() }

    composeTestRule.onNodeWithText("Open settings").assertIsDisplayed()
  }
}
