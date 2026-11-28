// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.Avatar
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneOffset
import java.time.ZonedDateTime

object ChatMessageTestTags {
  const val MESSAGE_ROW = "chatMessageRow"
  const val MESSAGE_TEXT = "chatMessageText"
  const val MESSAGE_IMAGE = "chatMessageImage"
  const val AUTHOR_TIME = "chatMessageAuthorTime"
  const val AVATAR = "chatMessageAvatar"
}

/**
 * A UI component representing a text chat message in a conversation.
 *
 * @param message The [ChatMessage] to display.
 * @param authorName The name of the message author (e.g., "Kim" or "Marc").
 * @param authorUser The [User] object of the author (used for avatar rendering).
 * @param isCurrentUser Whether the message was sent by the current user.
 * @param formattedTime The formatted timestamp string (e.g., "13:58").
 * @param modifier The [Modifier] applied to this component.
 */
@Composable
fun ChatMessageRow(
    message: ChatMessage,
    authorName: String,
    authorUser: User?,
    isCurrentUser: Boolean,
    formattedTime: String,
    modifier: Modifier = Modifier,
) {
  val textContent = (message.content as? MessageContent.Text)?.text ?: return

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

@Composable
private fun MessageContentColumn(
    authorName: String,
    formattedTime: String,
    text: String,
    bubbleColor: Color,
    textColor: Color,
    shape: RoundedCornerShape,
    isCurrentUser: Boolean,
) {
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
      Text(
          text = text,
          style = MaterialTheme.typography.bodyMedium,
          color = textColor,
          modifier = Modifier.padding(12.dp).testTag(ChatMessageTestTags.MESSAGE_TEXT),
      )
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
