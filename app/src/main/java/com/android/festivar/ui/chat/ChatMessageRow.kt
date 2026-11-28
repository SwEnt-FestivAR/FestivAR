// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.Avatar
import com.android.festivar.ui.theme.AppTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import java.time.ZoneOffset
import java.time.ZonedDateTime

object ChatMessageTestTags {
  const val MESSAGE_ROW = "chatMessageRow"
  const val MESSAGE_TEXT = "chatMessageText"
  const val MESSAGE_IMAGE = "chatMessageImage"
  const val MESSAGE_IMAGE_FAILURE = "chatMessageImageFailure"
  const val AUTHOR_TIME = "chatMessageAuthorTime"
  const val AVATAR = "chatMessageAvatar"
}

/**
 * A UI component representing a chat message (text or image) in a conversation.
 *
 * @param message The [ChatMessage] to display.
 * @param authorName The name of the message author (e.g., "Kim" or "Marc").
 * @param authorUser The [User] object of the author (used for avatar rendering).
 * @param isCurrentUser Whether the message was sent by the current user.
 * @param formattedTime The formatted timestamp string (e.g., "13:58").
 * @param modifier The [Modifier] applied to this component.
 */
@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun ChatMessageRow(
    message: ChatMessage,
    authorName: String,
    authorUser: User?,
    isCurrentUser: Boolean,
    formattedTime: String,
    modifier: Modifier = Modifier,
) {
  val textContent = (message.content as? MessageContent.Text)?.text ?: ""
  val imageUrl = (message.content as? MessageContent.Text)?.imageUrls?.firstOrNull()
  if (textContent.isBlank() && imageUrl == null) return

  val bubbleColor =
      if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      }

  val textColor =
      if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
      } else {
        MaterialTheme.colorScheme.onSurface
      }

  val shape =
      RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (isCurrentUser) 16.dp else 4.dp,
          bottomEnd = if (isCurrentUser) 4.dp else 16.dp,
      )

  Row(
      modifier =
          modifier.fillMaxWidth().padding(vertical = 4.dp).testTag(ChatMessageTestTags.MESSAGE_ROW),
      horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
      verticalAlignment = Alignment.Top,
  ) {
    if (!isCurrentUser) {
      if (authorUser != null) {
        Avatar(user = authorUser, modifier = Modifier.testTag(ChatMessageTestTags.AVATAR))
        Spacer(Modifier.width(8.dp))
      }
      MessageContentColumn(
          authorName = authorName,
          formattedTime = formattedTime,
          text = textContent,
          imageUrl = imageUrl,
          bubbleColor = bubbleColor,
          textColor = textColor,
          shape = shape,
          isCurrentUser = false,
      )
    } else {
      MessageContentColumn(
          authorName = authorName,
          formattedTime = formattedTime,
          text = textContent,
          imageUrl = imageUrl,
          bubbleColor = bubbleColor,
          textColor = textColor,
          shape = shape,
          isCurrentUser = true,
      )
      if (authorUser != null) {
        Spacer(Modifier.width(8.dp))
        Avatar(user = authorUser, modifier = Modifier.testTag(ChatMessageTestTags.AVATAR))
      }
    }
  }
}

/** A UI component representing a chat message with an image in a conversation. */
@Composable
fun ImageChatMessageRow(
    message: ChatMessage,
    authorName: String,
    authorUser: User?,
    isCurrentUser: Boolean,
    formattedTime: String,
    modifier: Modifier = Modifier,
) {
  ChatMessageRow(
      message = message,
      authorName = authorName,
      authorUser = authorUser,
      isCurrentUser = isCurrentUser,
      formattedTime = formattedTime,
      modifier = modifier,
  )
}

@Composable
@OptIn(ExperimentalGlideComposeApi::class)
private fun MessageContentColumn(
    authorName: String,
    formattedTime: String,
    text: String,
    imageUrl: String?,
    bubbleColor: Color,
    textColor: Color,
    shape: RoundedCornerShape,
    isCurrentUser: Boolean,
) {
  val isPreview = LocalInspectionMode.current
  Column(
      horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
  ) {
    Text(
        text = "$authorName · $formattedTime".uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 2.dp).testTag(ChatMessageTestTags.AUTHOR_TIME),
    )
    Surface(
        color = bubbleColor,
        shape = shape,
    ) {
      if (imageUrl != null) {
        Column(
            modifier = Modifier.width(220.dp).padding(8.dp),
        ) {
          GlideImage(
              model = imageUrl,
              contentDescription = "Chat image",
              contentScale = ContentScale.Crop,
              failure =
                  placeholder {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(160.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .testTag(ChatMessageTestTags.MESSAGE_IMAGE_FAILURE),
                        contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                          painter = painterResource(R.drawable.ic_event),
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  },
              loading = if (isPreview) placeholder(R.drawable.app_logo) else null,
              modifier =
                  Modifier.fillMaxWidth()
                      .height(160.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .testTag(ChatMessageTestTags.MESSAGE_IMAGE),
          )
          if (text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.testTag(ChatMessageTestTags.MESSAGE_TEXT),
            )
          }
        }
      } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(12.dp).testTag(ChatMessageTestTags.MESSAGE_TEXT),
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun IncomingMessagePreview() {
  AppTheme {
    ChatMessageRow(
        message =
            ChatMessage(
                messageId = "m1",
                authorId = "u1",
                sentAt = ZonedDateTime.of(2026, 10, 17, 13, 58, 0, 0, ZoneOffset.UTC),
                content = MessageContent.Text("Every table in the right place ?"),
            ),
        authorName = "Kim",
        authorUser = User("u1", name = "Kim", surname = "Keller"),
        isCurrentUser = false,
        formattedTime = "13:58",
        modifier = Modifier.padding(8.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun OutgoingMessagePreview() {
  AppTheme {
    ChatMessageRow(
        message =
            ChatMessage(
                messageId = "m2",
                authorId = "u2",
                sentAt = ZonedDateTime.of(2026, 10, 17, 13, 59, 0, 0, ZoneOffset.UTC),
                content = MessageContent.Text("Chaque table à sa place !"),
            ),
        authorName = "Manon",
        authorUser = User("u2", name = "Manon", surname = "Servoz"),
        isCurrentUser = true,
        formattedTime = "13:59",
        modifier = Modifier.padding(8.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun IncomingImageMessagePreview() {
  AppTheme {
    ImageChatMessageRow(
        message =
            ChatMessage(
                messageId = "m3",
                authorId = "u1",
                sentAt = ZonedDateTime.of(2026, 10, 17, 6, 7, 0, 0, ZoneOffset.UTC),
                content =
                    MessageContent.Text(
                        text = "Chaque table à sa place !",
                        imageUrls =
                            listOf("android.resource://com.android.festivar/drawable/app_logo"),
                    ),
            ),
        authorName = "Kim",
        authorUser = User("u1", name = "Kim", surname = "Keller"),
        isCurrentUser = false,
        formattedTime = "06:07",
        modifier = Modifier.padding(8.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun OutgoingImageMessagePreview() {
  AppTheme {
    ImageChatMessageRow(
        message =
            ChatMessage(
                messageId = "m4",
                authorId = "u2",
                sentAt = ZonedDateTime.of(2026, 10, 17, 6, 7, 0, 0, ZoneOffset.UTC),
                content =
                    MessageContent.Text(
                        text = "Chaque table à sa place !",
                        imageUrls =
                            listOf("android.resource://com.android.festivar/drawable/app_logo"),
                    ),
            ),
        authorName = "Manon",
        authorUser = User("u2", name = "Manon", surname = "Servoz"),
        isCurrentUser = true,
        formattedTime = "06:07",
        modifier = Modifier.padding(8.dp),
    )
  }
}
