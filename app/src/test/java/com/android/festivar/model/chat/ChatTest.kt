// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import com.android.festivar.model.user.User
import com.android.festivar.model.voicenote.VoiceNote
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTest {
  private val sentAt = ZonedDateTime.of(2026, 11, 20, 16, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private fun message(
      messageId: String = "m1",
      authorId: String = "u1",
      time: ZonedDateTime = sentAt,
      content: MessageContent = MessageContent.Text("Hello"),
  ) = ChatMessage(messageId, authorId, time, content)

  private fun voiceNote() =
      VoiceNote(
          id = "n1",
          audioUrl = "https://example.com/n1.m4a",
          duration = Duration.ofSeconds(14),
          createdAt = sentAt,
      )

  private fun assertInvalid(expectedMessage: String, block: () -> Unit) {
    val exception = assertThrows(IllegalArgumentException::class.java) { block() }
    assertEquals(expectedMessage, exception.message)
  }

  /** Breaks if an otherwise valid chat no longer gets default values and rejects blank IDs. */
  @Test
  fun chatCreationUsesDefaultsAndRejectsBlankIds() {
    assertTrue(Chat("c1", "e1").messages.isEmpty())
    assertTrue(Chat("c1", "e1").members.isEmpty())
    assertInvalid("The chatId cannot be empty.") { Chat(" ", "e1") }
    assertInvalid("The eventId cannot be empty.") { Chat("c1", "") }
  }

  /** Breaks if duplicate message IDs are accepted in Chat. */
  @Test
  fun chatRejectsDuplicateMessageIds() {
    assertInvalid("A messageId is duplicated.") { Chat("c1", "e1", listOf(message(), message())) }
  }

  /** Breaks if Chat accepts duplicate users or member changes are not immutable. */
  @Test
  fun chatMemberValidationAndChanges() {
    val alice = User("u1")
    val bob = User("u2")
    val chat = Chat("c1", "e1", members = listOf(alice))

    assertInvalid("A user is registered twice.") {
      Chat("c1", "e1", members = listOf(alice, alice.copy(name = "Alice")))
    }
    assertInvalid("This user is already registered in the chat.") { chat.addMember(alice) }
    assertInvalid("This user is not registered in the chat.") { chat.removeMember("u2") }

    assertEquals(listOf(alice, bob), chat.addMember(bob).members)
    assertEquals(
        emptyList<User>(),
        chat.addMember(bob).removeMember("u1").removeMember("u2").members,
    )
    assertEquals(listOf(alice), chat.members)
  }

  /** Breaks if Chat accepts messages that are not ordered oldest first. */
  @Test
  fun chatRejectsUnsortedMessages() {
    assertInvalid("The messages must be sorted oldest first.") {
      Chat(
          "c1",
          "e1",
          listOf(message("m2", time = sentAt.plusHours(1)), message("m1")),
      )
    }
  }

  /** Breaks if blank chat-message IDs or author IDs are accepted. */
  @Test
  fun chatMessageRejectsBlankIds() {
    assertInvalid("The messageId cannot be empty.") { message(messageId = " ") }
    assertInvalid("The authorId cannot be empty.") { message(authorId = "") }
  }

  /** Breaks if blank text content is accepted. */
  @Test
  fun textContentRejectsBlankText() {
    assertInvalid("The text cannot be empty.") { MessageContent.Text(" \t") }
  }

  /** Breaks if addMessage accepts duplicate IDs or appends an older message. */
  @Test
  fun addMessageRejectsDuplicateOrOlderMessage() {
    val chat = Chat("c1", "e1", listOf(message()))

    assertInvalid("A messageId is duplicated.") { chat.addMessage(message()) }
    assertInvalid("The message cannot be older than the last message.") {
      chat.addMessage(message("m2", time = sentAt.minusNanos(1)))
    }
  }

  /** Breaks if addMessage changes the original or does not preserve chronological order. */
  @Test
  fun addMessageAddsAtEndWithoutChangingOriginal() {
    val chat = Chat("c1", "e1", listOf(message()))
    val later = message("m2", time = sentAt.plusHours(1))

    val result = chat.addMessage(later)

    assertEquals(listOf(message(), later), result.messages)
    assertEquals(listOf(message()), chat.messages)
  }

  /** Breaks if deleteMessage accepts an unknown ID or changes the original chat. */
  @Test
  fun deleteMessageRemovesKnownMessageWithoutChangingOriginal() {
    val chat = Chat("c1", "e1", listOf(message(), message("m2", time = sentAt.plusHours(1))))

    assertInvalid("The messageId is unknown.") { chat.deleteMessage("missing") }
    assertEquals(listOf(chat.messages[1]), chat.deleteMessage("m1").messages)
    assertEquals(2, chat.messages.size)
  }

  /** Breaks if text message serialization loses fields or image URLs. */
  @Test
  fun textMessageMapRoundTrips() {
    val original =
        message(
            content =
                MessageContent.Text(
                    "Hello",
                    listOf(
                        "https://example.com/1.jpg",
                        "https://example.com/2.jpg",
                    ),
                ),
        )

    assertEquals(
        mapOf(
            "messageId" to "m1",
            "authorId" to "u1",
            "sentAt" to sentAt.toString(),
            "type" to "TEXT",
            "text" to "Hello",
            "imageUrls" to
                listOf(
                    "https://example.com/1.jpg",
                    "https://example.com/2.jpg",
                ),
        ),
        original.toMap(),
    )
    assertEquals(original, ChatMessage.fromMap(original.toMap()))
  }

  /** Breaks if voice message serialization loses the voice note or type. */
  @Test
  fun voiceMessageMapRoundTrips() {
    val original = message(content = MessageContent.Voice(voiceNote()))

    assertEquals("VOICE", original.toMap()["type"])
    assertEquals(original, ChatMessage.fromMap(original.toMap()))
  }

  /** Breaks if text messages saved without imageUrls can no longer be read. */
  @Test
  fun textMessageMapDefaultsMissingImageUrls() {
    val original = message(content = MessageContent.Text("Hello"))
    val stored = original.toMap() - "imageUrls"

    assertEquals(original, ChatMessage.fromMap(stored))
  }

  /** Breaks if invalid stored message data is accepted or parsed with a made-up value. */
  @Test
  fun chatMessageMapRejectsInvalidData() {
    val stored = message(content = MessageContent.Text("Hello")).toMap()
    val required = listOf("messageId", "authorId", "sentAt", "type", "text")

    required.forEach { name ->
      assertInvalid("Invalid chat message data: '$name' is missing or invalid.") {
        ChatMessage.fromMap(stored - name)
      }
    }
    assertInvalid("Invalid chat message data: bad sentAt 'yesterday'.") {
      ChatMessage.fromMap(stored + ("sentAt" to "yesterday"))
    }
    assertInvalid("Invalid chat message data: bad type 'UNKNOWN'.") {
      ChatMessage.fromMap(stored + ("type" to "UNKNOWN"))
    }
    assertInvalid("Invalid chat message data: 'imageUrls' is missing or invalid.") {
      ChatMessage.fromMap(stored + ("imageUrls" to listOf("ok", 42)))
    }
  }

  /** Breaks if voice data stored in a message is no longer rebuilt through VoiceNote. */
  @Test
  fun chatMessageMapRejectsInvalidVoiceNote() {
    val stored = message(content = MessageContent.Voice(voiceNote())).toMap()

    assertInvalid("Invalid chat message data: 'voiceNote' is missing or invalid.") {
      ChatMessage.fromMap(stored - "voiceNote")
    }
  }
}
