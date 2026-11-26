// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatRepositoryLocalTest {
  private lateinit var repository: ChatRepositoryLocal

  private val sentAt = ZonedDateTime.of(2026, 11, 20, 16, 0, 0, 0, ZoneOffset.UTC)
  private val message = ChatMessage("m1", "u1", sentAt, MessageContent.Text("Hello"))
  private val chat = Chat("c1", "e1", listOf(message))
  private val otherChat = Chat("c2", "e2")

  @Before
  fun setUp() {
    repository = ChatRepositoryLocal()
  }

  /** Breaks if UID generation stops producing a non-empty unique value. */
  @Test
  fun getNewUidProducesUniqueIds() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    assertTrue(first.isNotBlank())
    assertTrue(second.isNotBlank())
    assertTrue(first != second)
  }

  /** Breaks if initial chats are not validated or made available in insertion order. */
  @Test
  fun initialChatsAreValidatedAndReturnedInOrder() = runTest {
    repository = ChatRepositoryLocal(listOf(chat, otherChat))

    assertEquals(listOf(chat, otherChat), repository.getAllChats())
    assertThrows(IllegalArgumentException::class.java) {
      ChatRepositoryLocal(listOf(chat, chat.copy(eventId = "e2")))
    }
  }

  /** Breaks if adding and retrieving chats changes chat lookup behavior. */
  @Test
  fun addAndGetChatSucceeds() = runTest {
    repository.addChat(chat)

    assertEquals(listOf(chat), repository.getAllChats())
    assertEquals(chat, repository.getChat(chat.chatId))
    assertEquals(listOf(chat), repository.getChatsForEvent(chat.eventId))
  }

  /** Breaks if duplicate chat IDs or missing chats are accepted. */
  @Test
  fun addAndGetChatRejectInvalidIds() = runTest {
    repository.addChat(chat)

    val duplicateAddFailure = runCatching {
      repository.addChat(chat.copy(eventId = "e2"))
    }
        .exceptionOrNull()
    assertTrue(duplicateAddFailure is IllegalArgumentException)
    val missingChatFailure = runCatching { repository.getChat("missing") }.exceptionOrNull()
    assertTrue(missingChatFailure is NoSuchElementException)
    assertEquals(emptyList<Chat>(), repository.getChatsForEvent("missing"))
  }

  /** Breaks if editChat overwrites a chat or does not preserve the original identifier. */
  @Test
  fun editChatSucceedsAndValidatesId() = runTest {
    repository.addChat(chat)
    val updated = chat.copy(eventId = "e3")

    repository.editChat(chat.chatId, updated)

    assertEquals(updated, repository.getChat(chat.chatId))
    assertThrows(IllegalArgumentException::class.java) {
      kotlinx.coroutines.runBlocking { repository.editChat(chat.chatId, otherChat) }
    }
  }

  /** Breaks if editing or deleting an unknown chat modifies repository state. */
  @Test
  fun editAndDeleteUnknownChatsFail() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      kotlinx.coroutines.runBlocking {
        repository.editChat("missing", chat.copy(chatId = "missing"))
      }
    }
    assertThrows(NoSuchElementException::class.java) {
      kotlinx.coroutines.runBlocking { repository.deleteChat("missing") }
    }
  }

  /** Breaks if deleting a chat leaves it retrievable. */
  @Test
  fun deleteChatSucceeds() = runTest {
    repository.addChat(chat)

    repository.deleteChat(chat.chatId)

    assertTrue(repository.getAllChats().isEmpty())
  }

  /** Breaks if message append or deletion does not update the chat immutably. */
  @Test
  fun addAndDeleteMessageSucceed() = runTest {
    repository.addChat(chat.copy(messages = emptyList()))
    val later = ChatMessage("m2", "u2", sentAt.plusHours(1), MessageContent.Text("Later"))

    repository.addMessage(chat.chatId, message)
    repository.addMessage(chat.chatId, later)
    repository.deleteMessage(chat.chatId, message.messageId)

    assertEquals(listOf(later), repository.getChat(chat.chatId).messages)
  }

  /** Breaks if message operations accept duplicate, older, or unknown messages. */
  @Test
  fun messageOperationsRejectInvalidChanges() = runTest {
    repository.addChat(chat)

    assertThrows(IllegalArgumentException::class.java) {
      kotlinx.coroutines.runBlocking { repository.addMessage(chat.chatId, message) }
    }
    assertThrows(IllegalArgumentException::class.java) {
      kotlinx.coroutines.runBlocking { repository.deleteMessage(chat.chatId, "missing") }
    }
  }

  /** Breaks if event lookup returns only one chat or excludes a second chat from the event. */
  @Test
  fun getChatsForEventReturnsEveryLinkedChat() = runTest {
    val secondChat = Chat("c3", "e1")
    repository.addChat(chat)
    repository.addChat(secondChat)

    assertEquals(listOf(chat, secondChat), repository.getChatsForEvent("e1"))
  }

  /** Breaks if member operations do not update membership immutably or reject invalid users. */
  @Test
  fun addAndRemoveMemberSucceedAndValidate() = runTest {
    repository.addChat(chat.copy(members = listOf("u1")))

    repository.addMember(chat.chatId, "u2")
    assertEquals(listOf("u1", "u2"), repository.getChat(chat.chatId).members)

    repository.removeMember(chat.chatId, "u1")
    assertEquals(listOf("u2"), repository.getChat(chat.chatId).members)
    assertThrows(IllegalArgumentException::class.java) {
      kotlinx.coroutines.runBlocking { repository.addMember(chat.chatId, "u2") }
    }
    assertThrows(IllegalArgumentException::class.java) {
      kotlinx.coroutines.runBlocking { repository.removeMember(chat.chatId, "missing") }
    }
  }

  /** Breaks if member operations accept changes for unknown chats. */
  @Test
  fun memberOperationsRejectUnknownChats() = runTest {
    assertThrows(NoSuchElementException::class.java) {
      kotlinx.coroutines.runBlocking { repository.addMember("missing", "u1") }
    }
    assertThrows(NoSuchElementException::class.java) {
      kotlinx.coroutines.runBlocking { repository.removeMember("missing", "u1") }
    }
  }
}
