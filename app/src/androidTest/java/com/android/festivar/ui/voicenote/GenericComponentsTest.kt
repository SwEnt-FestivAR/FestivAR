// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

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

/** The generic components of the voice note UI, which have no voice note logic inside. */
@RunWith(AndroidJUnit4::class)
class GenericComponentsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val events = mutableListOf<String>()

  private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
      composeTestRule.setContent {
        AppTheme { content() }
      }

  private fun text(text: String) = composeTestRule.onNodeWithText(text)

  @Test
  fun fixedBottomBar_showsItsContent() {
    show {
      FixedBottomBar {
        Text("first line")
        Text("second line")
      }
    }

    text("first line").assertIsDisplayed()
    text("second line").assertIsDisplayed()
  }

  @Test
  fun infoNote_showsTheMessageWithoutAction() {
    show { InfoNote("Microphone is off.") }

    text("Microphone is off.").assertIsDisplayed()
  }

  @Test
  fun infoNote_showsItsActionUnderTheMessage() {
    show {
      InfoNote(
          "Microphone is off.",
          action = { TextButton(onClick = { events += "settings" }) { Text("Open settings") } },
      )
    }

    text("Microphone is off.").assertIsDisplayed()
    text("Open settings").performClick()

    assertEquals(listOf("settings"), events)
  }

  private fun showDialog() = show {
    ConfirmDialog(
        title = "Delete this voice note?",
        text = "Nobody will be able to hear it.",
        confirmLabel = "Delete",
        dismissLabel = "Keep it",
        onConfirm = { events += "confirm" },
        onDismiss = { events += "dismiss" },
    )
  }

  @Test
  fun confirmDialog_showsTheQuestionAndConfirmCallsOnlyTheConfirmCallback() {
    showDialog()

    text("Delete this voice note?").assertIsDisplayed()
    text("Nobody will be able to hear it.").assertIsDisplayed()
    text("Delete").performClick()

    assertEquals(listOf("confirm"), events)
  }

  @Test
  fun confirmDialog_keepCallsOnlyTheDismissCallback() {
    showDialog()

    text("Keep it").performClick()

    assertEquals(listOf("dismiss"), events)
  }
}
