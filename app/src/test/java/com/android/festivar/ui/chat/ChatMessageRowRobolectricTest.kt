package com.android.festivar.ui.chat

// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import io.mockk.mockk
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [ChatMessageRow] running on Robolectric to ensure code coverage. */
@RunWith(AndroidJUnit4::class)
class ChatMessageRowRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val userCamille = User("u1", name = "Camille", surname = "Durand")

  private val incomingTextMessage =
      ChatMessage(
          messageId = "m1",
          authorId = "u1",
          sentAt = ZonedDateTime.of(2026, 10, 17, 13, 58, 0, 0, ZoneOffset.UTC),
          content = MessageContent.Text("On se retrouve à 18 h pour installer les tables ?"),
      )

  private val outgoingTextMessage =
      ChatMessage(
          messageId = "m2",
          authorId = "u2",
          sentAt = ZonedDateTime.of(2026, 10, 17, 13, 59, 0, 0, ZoneOffset.UTC),
          content = MessageContent.Text("Oui, je m'occupe de la grande salle."),
      )

  private val voiceMessage =
      ChatMessage(
          messageId = "m3",
          authorId = "u1",
          sentAt = ZonedDateTime.of(2026, 10, 17, 14, 0, 0, 0, ZoneOffset.UTC),
          content = MessageContent.Voice(mockk(relaxed = true)),
      )

  /**
   * Breaks if an incoming message fails to display author name, timestamp, avatar, or text content.
   */
  @Test
  fun displaysIncomingMessageWithUserCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = incomingTextMessage,
            authorName = "Camille",
            authorUser = userCamille,
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

  /** Breaks if an incoming message without author user displays an avatar. */
  @Test
  fun displaysIncomingMessageWithoutUserCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = incomingTextMessage,
            authorName = "Camille",
            authorUser = null,
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
        .assertDoesNotExist()
  }

  /**
   * Breaks if an outgoing message fails to display author name, timestamp, avatar, or text content.
   */
  @Test
  fun displaysOutgoingMessageWithUserCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = outgoingTextMessage,
            authorName = "Vous",
            authorUser = userCamille,
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

  /** Breaks if an outgoing message without author user displays an avatar. */
  @Test
  fun displaysOutgoingMessageWithoutUserCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = outgoingTextMessage,
            authorName = "Vous",
            authorUser = null,
            isCurrentUser = true,
            formattedTime = "13:59",
        )
      }
    }

    composeTestRule.onNodeWithText("VOUS · 13:59").assertIsDisplayed()
    composeTestRule.onNodeWithText("Oui, je m'occupe de la grande salle.").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  /** Breaks if a non-text message displays a chat message row. */
  @Test
  fun nonTextMessageReturnsEarlyWithoutDisplayingRow() {
    composeTestRule.setContent {
      AppTheme {
        ChatMessageRow(
            message = voiceMessage,
            authorName = "Camille",
            authorUser = userCamille,
            isCurrentUser = false,
            formattedTime = "14:00",
        )
      }
    }

    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_ROW).assertDoesNotExist()
  }

  /** Breaks if incoming message preview fails to render without errors. */
  @Test
  fun incomingMessagePreviewRendersSuccessfully() {
    composeTestRule.setContent { IncomingMessagePreview() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("KIM · 13:58").assertIsDisplayed()
    composeTestRule.onNodeWithText("Every table in the right place ?").assertIsDisplayed()
  }

  /** Breaks if outgoing message preview fails to render without errors. */
  @Test
  fun outgoingMessagePreviewRendersSuccessfully() {
    composeTestRule.setContent { OutgoingMessagePreview() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("MANON · 13:59").assertIsDisplayed()
    composeTestRule.onNodeWithText("Chaque table à sa place !").assertIsDisplayed()
  }
}
