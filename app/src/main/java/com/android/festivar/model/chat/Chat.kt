// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import com.android.festivar.model.voicenote.VoiceNote
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

const val CHAT_NAME_MAX_LENGTH = 80
const val DEFAULT_CHAT_NAME = "General"

/**
 * Conversation for one event.
 *
 * The chat is immutable. Changes return a new chat, with messages ordered oldest first.
 *
 * @property chatId Unique identifier of the chat.
 * @property eventId Unique identifier of the event.
 * @property name Name of the chat.
 * @property messages Messages in chronological order.
 * @property members IDs of users who belong to the chat.
 */
data class Chat(
    val chatId: String,
    val eventId: String,
    val name: String = DEFAULT_CHAT_NAME,
    val messages: List<ChatMessage> = emptyList(),
    val members: List<String> = emptyList(),
) {
  init {
    require(chatId.isNotBlank()) { "The chatId cannot be empty." }
    require(eventId.isNotBlank()) { "The eventId cannot be empty." }
    require(name.isNotBlank()) { "The chat name cannot be empty." }
    require(name.length <= CHAT_NAME_MAX_LENGTH) {
      "The chat name cannot exceed $CHAT_NAME_MAX_LENGTH characters."
    }
    require(members.all { it.isNotBlank() }) { "The userId cannot be empty." }
    require(members.distinct().size == members.size) { "A user is registered twice." }
    require(messages.map { it.messageId }.distinct().size == messages.size) {
      "A messageId is duplicated."
    }
    require(messages.zipWithNext().all { (older, newer) -> !newer.sentAt.isBefore(older.sentAt) }) {
      "The messages must be sorted oldest first."
    }
  }

  /**
   * Inserts [message] by sent time if its ID is unique.
   *
   * @throws IllegalArgumentException if the message ID is duplicated.
   */
  fun addMessage(message: ChatMessage): Chat {
    require(messages.none { it.messageId == message.messageId }) { "A messageId is duplicated." }

    val insertAt =
        messages
            .indexOfFirst { it.sentAt.isAfter(message.sentAt) }
            .let { index -> if (index == -1) messages.size else index }
    val updatedMessages = messages.toMutableList().apply { add(insertAt, message) }
    return copy(messages = updatedMessages)
  }

  /**
   * Deletes the message identified by [messageId].
   *
   * @throws IllegalArgumentException if [messageId] is unknown.
   */
  fun deleteMessage(messageId: String): Chat {
    require(messages.any { it.messageId == messageId }) { "The messageId is unknown." }
    return copy(messages = messages.filterNot { it.messageId == messageId })
  }

  /**
   * Adds the user identified by [userId] to this chat.
   *
   * @throws IllegalArgumentException if [userId] is already a member or is blank.
   */
  fun addMember(userId: String): Chat {
    require(userId.isNotBlank()) { "The userId cannot be empty." }
    require(members.none { it == userId }) { "This user is already registered in the chat." }
    return copy(members = members + userId)
  }

  /**
   * Removes the member identified by [userId] from this chat.
   *
   * @throws IllegalArgumentException if [userId] is not a member.
   */
  fun removeMember(userId: String): Chat {
    require(members.any { it == userId }) { "This user is not registered in the chat." }
    return copy(members = members.filterNot { it == userId })
  }
}

/**
 * One message in a chat.
 *
 * @property messageId Unique identifier of the message.
 * @property authorId Unique identifier of the message author.
 * @property sentAt Time at which the message was sent.
 * @property content Text or voice content.
 */
data class ChatMessage(
    val messageId: String,
    val authorId: String,
    val sentAt: ZonedDateTime,
    val content: MessageContent,
) {
  init {
    require(messageId.isNotBlank()) { "The messageId cannot be empty." }
    require(authorId.isNotBlank()) { "The authorId cannot be empty." }
  }

  /** The message as a map stored in the database. */
  fun toMap(): Map<String, Any> =
      mapOf(
          "messageId" to messageId,
          "authorId" to authorId,
          "sentAt" to sentAt.toString(),
      ) +
          when (val value = content) {
            is MessageContent.Text ->
                mapOf(
                    "type" to "TEXT",
                    "text" to value.text,
                    "imageUrls" to value.imageUrls,
                )
            is MessageContent.Voice ->
                mapOf(
                    "type" to "VOICE",
                    "voiceNote" to value.voiceNote.toMap(),
                )
          }

  companion object {
    /**
     * Rebuilds a message from a map written by [toMap].
     *
     * @throws IllegalArgumentException if required or stored data is missing or invalid.
     */
    fun fromMap(map: Map<*, *>): ChatMessage {
      fun <T> field(name: String, value: T?): T =
          requireNotNull(value) { "Invalid chat message data: '$name' is missing or invalid." }

      fun stringField(name: String): String {
        val value = field(name, map[name] as? String)
        require(value.isNotBlank()) { "Invalid chat message data: '$name' is missing or invalid." }
        return value
      }

      val messageId = stringField("messageId")
      val authorId = stringField("authorId")
      val sentAtString = stringField("sentAt")
      val sentAt =
          try {
            ZonedDateTime.parse(sentAtString)
          } catch (e: DateTimeParseException) {
            throw IllegalArgumentException(
                "Invalid chat message data: bad sentAt '$sentAtString'.",
                e,
            )
          }

      val type = stringField("type")
      val content: MessageContent =
          when (type) {
            "TEXT" -> {
              val text = stringField("text")
              val imageUrls =
                  map["imageUrls"]?.let { value ->
                    requireNotNull((value as? List<*>)?.map { field("imageUrls", it as? String) }) {
                      "Invalid chat message data: 'imageUrls' is missing or invalid."
                    }
                  } ?: emptyList()
              MessageContent.Text(text, imageUrls)
            }
            "VOICE" -> {
              val voiceNote = field("voiceNote", map["voiceNote"] as? Map<*, *>)
              MessageContent.Voice(VoiceNote.fromMap(voiceNote))
            }
            else -> throw IllegalArgumentException("Invalid chat message data: bad type '$type'.")
          }

      return ChatMessage(messageId, authorId, sentAt, content)
    }
  }
}

/** Content carried by a chat message. */
sealed interface MessageContent {
  /**
   * Text content with optional image URLs.
   *
   * @property text Text displayed in the message.
   * @property imageUrls URLs of images attached to the text message.
   */
  data class Text(
      val text: String,
      val imageUrls: List<String> = emptyList(),
  ) : MessageContent {
    init {
      require(text.isNotBlank()) { "The text cannot be empty." }
    }
  }

  /**
   * Voice content backed by an existing voice note.
   *
   * @property voiceNote Recorded voice note attached to the message.
   */
  data class Voice(val voiceNote: VoiceNote) : MessageContent
}
