// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

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
class ConfirmDialogTest {
  @get:Rule val composeTestRule = createComposeRule()

  private var confirmed = 0
  private var dismissed = 0

  private fun setDialog(destructive: Boolean = true) {
    composeTestRule.setContent {
      AppTheme {
        FestivarConfirmDialog(
            title = "Discard this task?",
            message = "What you typed will be lost.",
            confirmText = "Discard",
            dismissText = "Keep editing",
            onConfirm = { confirmed++ },
            onDismiss = { dismissed++ },
            destructive = destructive,
        )
      }
    }
  }

  private fun setDiscardDialog() {
    composeTestRule.setContent {
      AppTheme {
        FestivarDiscardDialog(
            title = "Discard this task?",
            onConfirm = { confirmed++ },
            onDismiss = { dismissed++ },
        )
      }
    }
  }

  @Test
  fun showsTheTitleTheMessageAndBothButtons() {
    setDialog()

    composeTestRule.onNodeWithText("Discard this task?").assertIsDisplayed()
    composeTestRule.onNodeWithText("What you typed will be lost.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
    composeTestRule.onNodeWithText("Keep editing").assertIsDisplayed()
  }

  @Test
  fun confirmButtonCallsOnConfirmOnly() {
    setDialog()

    composeTestRule.onNodeWithText("Discard").performClick()

    assertEquals(1, confirmed)
    assertEquals(0, dismissed)
  }

  @Test
  fun aNonDestructiveDialogConfirmsAndDismissesTheSame() {
    setDialog(destructive = false)

    composeTestRule.onNodeWithText("Discard").performClick()
    composeTestRule.onNodeWithText("Keep editing").performClick()

    assertEquals(1, confirmed)
    assertEquals(1, dismissed)
  }

  @Test
  fun dismissButtonCallsOnDismissOnly() {
    setDialog()

    composeTestRule.onNodeWithText("Keep editing").performClick()

    assertEquals(0, confirmed)
    assertEquals(1, dismissed)
  }

  @Test
  fun discardDialog_showsTheTitleAndTheSharedTexts() {
    setDiscardDialog()

    composeTestRule.onNodeWithText("Discard this task?").assertIsDisplayed()
    composeTestRule.onNodeWithText("What you typed will be lost.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
    composeTestRule.onNodeWithText("Keep editing").assertIsDisplayed()
  }

  @Test
  fun discardDialog_discardCallsOnConfirmOnly() {
    setDiscardDialog()

    composeTestRule.onNodeWithText("Discard").performClick()

    assertEquals(1, confirmed)
    assertEquals(0, dismissed)
  }

  @Test
  fun discardDialog_keepEditingCallsOnDismissOnly() {
    setDiscardDialog()

    composeTestRule.onNodeWithText("Keep editing").performClick()

    assertEquals(0, confirmed)
    assertEquals(1, dismissed)
  }
}
