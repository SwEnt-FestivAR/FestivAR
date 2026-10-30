// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

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

@RunWith(AndroidJUnit4::class)
class NoticeSnackbarTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun showsTheMessageAndItsActionCallsTheCallback() {
    val events = mutableListOf<String>()
    composeTestRule.setContent {
      AppTheme {
        NoticeSnackbar("Voice note not sent.", "Retry", onAction = { events += "action" })
      }
    }

    composeTestRule.onNodeWithText("Voice note not sent.").assertIsDisplayed()
    composeTestRule.onNodeWithText("Retry").performClick()

    assertEquals(listOf("action"), events)
  }

  @Test
  fun actionModifierIsAppliedToTheActionButton() {
    val events = mutableListOf<String>()
    composeTestRule.setContent {
      AppTheme {
        NoticeSnackbar(
            "Voice note not sent.",
            "Retry",
            onAction = { events += "action" },
            actionModifier = Modifier.testTag("retry"),
        )
      }
    }

    composeTestRule.onNodeWithTag("retry").performClick()

    assertEquals(listOf("action"), events)
  }
}
