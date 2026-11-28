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
import io.mockk.mockk
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [ImageChatMessageRow] running on Robolectric to ensure code coverage. */
@RunWith(AndroidJUnit4::class)
class ImageChatMessageRowRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val userCamille = User("u1", name = "Camille", surname = "Durand")

  private val incomingImageMessage =
      ChatMessage(
          messageId = "m1",
          authorId = "u1",
          sentAt = ZonedDateTime.of(2026, 10, 17, 13, 58, 0, 0, ZoneOffset.UTC),
          content =
              MessageContent.Text(
                  text = "Voici la photo du matériel.",
                  imageUrls = listOf("https://example.com/image.png"),
              ),
      )

  private val outgoingImageMessage =
      ChatMessage(
          messageId = "m2",
          authorId = "u2",
          sentAt = ZonedDateTime.of(2026, 10, 17, 13, 59, 0, 0, ZoneOffset.UTC),
          content =
              MessageContent.Text(
                  text = "Super reçu !",
                  imageUrls = listOf("https://example.com/image2.png"),
              ),
      )

  /**
   * Breaks if an incoming image message fails to display author name, timestamp, avatar, image, or
   * text content.
   */
  @Test
  fun displaysIncomingImageMessageCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ImageChatMessageRow(
            message = incomingImageMessage,
            authorName = "Camille",
            authorUser = userCamille,
            isCurrentUser = false,
            formattedTime = "13:58",
        )
      }
    }

    composeTestRule.onNodeWithText("CAMILLE · 13:58").assertIsDisplayed()
    composeTestRule.onNodeWithText("Voici la photo du matériel.").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /** Breaks if an incoming image message without author user displays an avatar. */
  @Test
  fun displaysIncomingImageMessageWithoutUserCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ImageChatMessageRow(
            message = incomingImageMessage,
            authorName = "Camille",
            authorUser = null,
            isCurrentUser = false,
            formattedTime = "13:58",
        )
      }
    }

    composeTestRule.onNodeWithText("CAMILLE · 13:58").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  /**
   * Breaks if an outgoing image message fails to display author name, timestamp, avatar, image, or
   * text content.
   */
  @Test
  fun displaysOutgoingImageMessageCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ImageChatMessageRow(
            message = outgoingImageMessage,
            authorName = "Vous",
            authorUser = userCamille,
            isCurrentUser = true,
            formattedTime = "13:59",
        )
      }
    }

    composeTestRule.onNodeWithText("VOUS · 13:59").assertIsDisplayed()
    composeTestRule.onNodeWithText("Super reçu !").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /** Breaks if an outgoing image message without author user displays an avatar. */
  @Test
  fun displaysOutgoingImageMessageWithoutUserCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ImageChatMessageRow(
            message = outgoingImageMessage,
            authorName = "Vous",
            authorUser = null,
            isCurrentUser = true,
            formattedTime = "13:59",
        )
      }
    }

    composeTestRule.onNodeWithText("VOUS · 13:59").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.AVATAR, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  /**
   * Breaks if a non-text/image message (e.g. voice) fails to return early without displaying row.
   */
  @Test
  fun nonTextOrImageMessageReturnsEarlyWithoutDisplayingRow() {
    val voiceMessage =
        ChatMessage(
            messageId = "m5",
            authorId = "u1",
            sentAt = ZonedDateTime.of(2026, 10, 17, 14, 0, 0, 0, ZoneOffset.UTC),
            content = MessageContent.Voice(mockk(relaxed = true)),
        )
    composeTestRule.setContent {
      AppTheme {
        ImageChatMessageRow(
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

  /**
   * Breaks if an image message with a failing image URL does not display the image failure
   * placeholder.
   */
  @Test
  fun displaysImageFailurePlaceholderWhenImageLoadFails() {
    val failingImageMessage =
        ChatMessage(
            messageId = "m_fail",
            authorId = "u1",
            sentAt = ZonedDateTime.of(2026, 10, 17, 14, 10, 0, 0, ZoneOffset.UTC),
            content =
                MessageContent.Text(
                    text = "Image loading failed",
                    imageUrls = listOf("invalid://failing-url"),
                ),
        )

    composeTestRule.setContent {
      AppTheme {
        ImageChatMessageRow(
            message = failingImageMessage,
            authorName = "Camille",
            authorUser = userCamille,
            isCurrentUser = false,
            formattedTime = "14:10",
        )
      }
    }

    composeTestRule
        .onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE_FAILURE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /** Breaks if incoming image message preview fails to render without errors. */
  @Test
  fun incomingImageMessagePreviewRendersSuccessfully() {
    composeTestRule.setContent { IncomingImageMessagePreview() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("KIM · 06:07").assertIsDisplayed()
    composeTestRule.onNodeWithText("Chaque table à sa place !").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE).assertIsDisplayed()
  }

  /** Breaks if outgoing image message preview fails to render without errors. */
  @Test
  fun outgoingImageMessagePreviewRendersSuccessfully() {
    composeTestRule.setContent { OutgoingImageMessagePreview() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("MANON · 06:07").assertIsDisplayed()
    composeTestRule.onNodeWithText("Chaque table à sa place !").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatMessageTestTags.MESSAGE_IMAGE).assertIsDisplayed()
  }
}
