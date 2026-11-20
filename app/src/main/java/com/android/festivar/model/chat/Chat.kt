// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import com.android.festivar.model.voicenote.VoiceNote
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Conversation for one event.
 *
 * The chat is immutable. Changes return a new chat, with messages ordered oldest first.
 *
 * @property chatId Unique identifier of the chat.
 * @property eventId Unique identifier of the event.
 * @property messages Messages in chronological order.
 */
data class Chat(
    val chatId: String,
    val eventId: String,
    val messages: List<ChatMessage> = emptyList(),
) {
  init {
    require(chatId.isNotBlank()) { "The chatId cannot be empty." }
    require(eventId.isNotBlank()) { "The eventId cannot be empty." }
    require(messages.map { it.messageId }.distinct().size == messages.size) {
      "A messageId is duplicated."
    }
    require(messages.zipWithNext().all { (older, newer) -> !newer.sentAt.isBefore(older.sentAt) }) {
      "The messages must be sorted oldest first."
    }
  }

  /**
   * Adds [message] if its ID is unique and it is not older than the current last message.
   *
   * @throws IllegalArgumentException if the message ID is duplicated or if the message is older
   *   than the last message.
   */
  fun addMessage(message: ChatMessage): Chat =
      when {
        messages.any { it.messageId == message.messageId } ->
            throw IllegalArgumentException("A messageId is duplicated.")
        messages.isNotEmpty() && message.sentAt.isBefore(messages.last().sentAt) ->
            throw IllegalArgumentException("The message cannot be older than the last message.")
        else -> copy(messages = messages + message)
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

      val messageId = field("messageId", map["messageId"] as? String)
      val authorId = field("authorId", map["authorId"] as? String)
      val sentAt =
          field("sentAt", map["sentAt"] as? String).let {
            try {
              ZonedDateTime.parse(it)
            } catch (e: DateTimeParseException) {
              throw IllegalArgumentException("Invalid chat message data: bad sentAt '$it'.", e)
            }
          }

      val type = field("type", map["type"] as? String)
      val content: MessageContent =
          when (type) {
            "TEXT" -> {
              val text = field("text", map["text"] as? String)
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
  /** Text content with optional image URLs. */
  data class Text(
      val text: String,
      val imageUrls: List<String> = emptyList(),
  ) : MessageContent {
    init {
      require(text.isNotBlank()) { "The text cannot be empty." }
    }
  }

  /** Voice content backed by an existing voice note. */
  data class Voice(val voiceNote: VoiceNote) : MessageContent
}
