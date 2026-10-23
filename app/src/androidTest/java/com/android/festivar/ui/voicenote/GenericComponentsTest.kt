// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

  // FixedBottomBar

  @Test
  fun fixedBottomBar_showsItsContent() {
    composeTestRule.setContent {
      AppTheme {
        FixedBottomBar {
          Text("first line")
          Text("second line")
        }
      }
    }

    composeTestRule.onNodeWithText("first line").assertIsDisplayed()
    composeTestRule.onNodeWithText("second line").assertIsDisplayed()
  }

  // NoticeSnackbar

  @Test
  fun noticeSnackbar_showsTheMessageAndTheAction() {
    composeTestRule.setContent {
      AppTheme { NoticeSnackbar("Voice note not sent.", "Retry", onAction = {}) }
    }

    composeTestRule.onNodeWithText("Voice note not sent.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
  }

  @Test
  fun noticeSnackbar_actionCallsTheCallbackOnce() {
    composeTestRule.setContent {
      AppTheme {
        NoticeSnackbar(
            "Voice note not sent.",
            "Retry",
            onAction = { events += "action" },
            actionModifier = Modifier.testTag("snackbar-action"),
        )
      }
    }

    composeTestRule.onNodeWithTag("snackbar-action").performClick()

    assertEquals(listOf("action"), events)
  }

  // InfoNote

  @Test
  fun infoNote_showsTheMessageWithoutAction() {
    composeTestRule.setContent { AppTheme { InfoNote("Microphone is off.") } }

    composeTestRule.onNodeWithText("Microphone is off.").assertIsDisplayed()
  }

  @Test
  fun infoNote_showsItsActionUnderTheMessage() {
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

  // ConfirmDialog

  private fun showDialog() {
    composeTestRule.setContent {
      AppTheme {
        ConfirmDialog(
            title = "Delete this voice note?",
            text = "Nobody will be able to hear it.",
            confirmLabel = "Delete",
            dismissLabel = "Keep it",
            onConfirm = { events += "confirm" },
            onDismiss = { events += "dismiss" },
        )
      }
    }
  }

  @Test
  fun confirmDialog_showsTheQuestionAndBothButtons() {
    showDialog()

    composeTestRule.onNodeWithText("Delete this voice note?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Nobody will be able to hear it.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    composeTestRule.onNodeWithText("Keep it").assertIsDisplayed()
  }

  @Test
  fun confirmDialog_confirmCallsOnlyTheConfirmCallback() {
    showDialog()

    composeTestRule.onNodeWithText("Delete").performClick()

    assertEquals(listOf("confirm"), events)
  }

  @Test
  fun confirmDialog_keepCallsOnlyTheDismissCallback() {
    showDialog()

    composeTestRule.onNodeWithText("Keep it").performClick()

    assertEquals(listOf("dismiss"), events)
  }
}
