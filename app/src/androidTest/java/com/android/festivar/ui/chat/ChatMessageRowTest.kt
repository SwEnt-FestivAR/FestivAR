// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageRowTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val camille = User("u1", name = "Camille", surname = "Durand")
  private val vous = User("u2", name = "Vous", surname = "")

  private val incomingMessage =
      ChatMessage(
          messageId = "m1",
          authorId = "u1",
          sentAt = ZonedDateTime.of(2026, 10, 17, 13, 58, 0, 0, ZoneOffset.UTC),
          content = MessageContent.Text("On se retrouve à 18 h pour installer les tables ?"),
      )

  private val outgoingMessage =
      ChatMessage(
          messageId = "m2",
          authorId = "u2",
          sentAt = ZonedDateTime.of(2026, 10, 17, 13, 59, 0, 0, ZoneOffset.UTC),
          content = MessageContent.Text("Oui, je m'occupe de la grande salle."),
      )

  /**
   * Breaks if an incoming chat message does not display the author name, timestamp, and message
   * text.
   */
  @Test
  fun displaysIncomingMessageCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = incomingMessage,
            authorName = "Camille",
            authorUser = camille,
            isCurrentUser = false,
            formattedTime = "13:58",
        )
      }
    }

    composeTestRule.onNodeWithText("CAMILLE · 13:58").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("On se retrouve à 18 h pour installer les tables ?")
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /**
   * Breaks if an outgoing chat message does not display the author name, timestamp, and message
   * text.
   */
  @Test
  fun displaysOutgoingMessageCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = outgoingMessage,
            authorName = "Vous",
            authorUser = vous,
            isCurrentUser = true,
            formattedTime = "13:59",
        )
      }
    }

    composeTestRule.onNodeWithText("VOUS · 13:59").assertIsDisplayed()
    composeTestRule.onNodeWithText("Oui, je m'occupe de la grande salle.").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
