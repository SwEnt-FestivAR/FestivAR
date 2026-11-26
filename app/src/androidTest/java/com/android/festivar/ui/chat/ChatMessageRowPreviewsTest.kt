// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageRowPreviewsTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the incoming message preview fails to display author "KIM", timestamp "13:58", or text "Every table in the right place ?". */
  @Test
  fun incomingMessagePreview_ShowsContent() {
    composeTestRule.setContent { IncomingMessagePreview() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("KIM · 13:58").assertIsDisplayed()
    composeTestRule.onNodeWithText("Every table in the right place ?").assertIsDisplayed()
  }

  /** Breaks if the outgoing message preview fails to display author "MANON", timestamp "13:59", or text "Chaque table à sa place !". */
  @Test
  fun outgoingMessagePreview_ShowsContent() {
    composeTestRule.setContent { OutgoingMessagePreview() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("MANON · 13:59").assertIsDisplayed()
    composeTestRule.onNodeWithText("Chaque table à sa place !").assertIsDisplayed()
  }
}
