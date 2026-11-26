// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryFirebaseTest {
  private lateinit var repository: ChatRepositoryFirebase
  private val sentAt = ZonedDateTime.of(2026, 11, 20, 16, 0, 0, 0, ZoneOffset.UTC)

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    runBlocking { Firebase.auth.signInAnonymously().await() }
    repository = ChatRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      val chats = Firebase.firestore.collection(CHAT_COLLECTION_PATH).get().await()
      for (document in chats.documents) {
        if (document.id.startsWith("chat-test-") || document.id.startsWith("chat-corrupt-")) {
          document.reference.delete().await()
        }
      }
      Firebase.auth.signOut()
    }
  }

  /** Breaks if UID generation returns blank or duplicate identifiers. */
  @Test
  fun getNewUid_returnsUniqueNonBlankIds() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    assertTrue(first.isNotBlank())
    assertNotEquals(first, second)
  }

  /** Breaks if chat CRUD loses fields or does not validate duplicate and missing chats. */
  @Test
  fun chatCrud_roundTripsAndValidates() {
    runBlocking {
      val chat =
          Chat(
              chatId = "chat-test-${UUID.randomUUID()}",
              eventId = "event-test",
              members = listOf("u1"),
          )

      repository.addChat(chat)
      assertEquals(chat, repository.getChat(chat.chatId))
      assertEquals(listOf(chat), repository.getChatsForEvent(chat.eventId))
      assertTrue(repository.getAllChats().contains(chat))

      val updated = chat.copy(members = listOf("u1", "u2"))
      repository.editChat(chat.chatId, updated)
      assertEquals(updated, repository.getChat(chat.chatId))
      assertThrows(IllegalArgumentException::class.java) {
        runBlocking { repository.editChat(chat.chatId, updated.copy(chatId = "different-id")) }
      }

      assertThrows(IllegalArgumentException::class.java) {
        runBlocking { repository.addChat(chat) }
      }

      repository.deleteChat(chat.chatId)
      assertThrows(NoSuchElementException::class.java) {
        runBlocking { repository.getChat(chat.chatId) }
      }
      assertThrows(NoSuchElementException::class.java) {
        runBlocking { repository.editChat(chat.chatId, updated) }
      }
      assertThrows(NoSuchElementException::class.java) {
        runBlocking { repository.deleteChat(chat.chatId) }
      }
    }
  }

  /** Breaks if event lookup omits chats or includes chats from another event. */
  @Test
  fun getChatsForEvent_returnsEveryLinkedChatOnly() = runBlocking {
    val first = Chat("chat-test-${UUID.randomUUID()}", "event-a")
    val second = Chat("chat-test-${UUID.randomUUID()}", "event-a")
    val other = Chat("chat-test-${UUID.randomUUID()}", "event-b")

    repository.addChat(first)
    repository.addChat(second)
    repository.addChat(other)

    val found = repository.getChatsForEvent("event-a")
    assertEquals(setOf(first.chatId, second.chatId), found.map { it.chatId }.toSet())
  }

  /** Breaks if message operations lose ordering or allow invalid message changes. */
  @Test
  fun messageOperations_roundTripChronologicallyAndValidate() {
    runBlocking {
      val chat = Chat("chat-test-${UUID.randomUUID()}", "event-test")
      val first = ChatMessage("m1", "u1", sentAt, MessageContent.Text("First", listOf("image.jpg")))
      val second = ChatMessage("m2", "u2", sentAt.plusHours(1), MessageContent.Text("Second"))

      repository.addChat(chat)
      repository.addMessage(chat.chatId, first)
      repository.addMessage(chat.chatId, second)

      assertEquals(listOf(first, second), repository.getChat(chat.chatId).messages)
      repository.deleteMessage(chat.chatId, "m1")
      assertEquals(listOf(second), repository.getChat(chat.chatId).messages)

      assertThrows(IllegalArgumentException::class.java) {
        runBlocking { repository.deleteMessage(chat.chatId, "missing") }
      }
      assertThrows(NoSuchElementException::class.java) {
        runBlocking { repository.addMessage("chat-test-missing-${UUID.randomUUID()}", first) }
      }
    }
  }

  /** Breaks if member operations do not persist or validate chat membership. */
  @Test
  fun memberOperations_roundTripAndValidate() {
    runBlocking {
      val chat = Chat("chat-test-${UUID.randomUUID()}", "event-test", members = listOf("u1"))

      repository.addChat(chat)
      repository.addMember(chat.chatId, "u2")
      assertEquals(listOf("u1", "u2"), repository.getChat(chat.chatId).members)

      repository.removeMember(chat.chatId, "u1")
      assertEquals(listOf("u2"), repository.getChat(chat.chatId).members)

      assertThrows(IllegalArgumentException::class.java) {
        runBlocking { repository.addMember(chat.chatId, "u2") }
      }
    }
  }

  /** Breaks if corrupt chat fields are read silently instead of named with the chat ID. */
  @Test
  fun getChat_rejectsCorruptMessageData() = runBlocking {
    val chatId = "chat-corrupt-${UUID.randomUUID()}"
    Firebase.firestore
        .collection(CHAT_COLLECTION_PATH)
        .document(chatId)
        .set(
            mapOf(
                "chatId" to chatId,
                "eventId" to "event-test",
                "members" to emptyList<String>(),
                "messages" to listOf("not-a-message"),
            )
        )
        .await()

    val error =
        assertThrows(IllegalArgumentException::class.java) {
          runBlocking { repository.getChat(chatId) }
        }
    assertTrue(error.message!!.contains(chatId))
  }

  /** Breaks if missing or malformed stored fields are silently accepted or lose chat context. */
  @Test
  fun getChat_rejectsMissingEventAndInvalidMemberOrMessageData() = runBlocking {
    val chats = Firebase.firestore.collection(CHAT_COLLECTION_PATH)
    val missingEventChatId = "chat-corrupt-${UUID.randomUUID()}"
    val memberChatId = "chat-corrupt-${UUID.randomUUID()}"
    val invalidMemberChatId = "chat-corrupt-${UUID.randomUUID()}"
    val messageChatId = "chat-corrupt-${UUID.randomUUID()}"
    val malformedMessageChatId = "chat-corrupt-${UUID.randomUUID()}"
    chats.document(missingEventChatId).set(mapOf("members" to emptyList<String>())).await()
    chats.document(memberChatId).set(mapOf("eventId" to "event-test", "members" to "u1")).await()
    chats
        .document(invalidMemberChatId)
        .set(mapOf("eventId" to "event-test", "members" to listOf("u1", 42)))
        .await()
    chats
        .document(messageChatId)
        .set(mapOf("eventId" to "event-test", "messages" to "not-a-list"))
        .await()
    chats
        .document(malformedMessageChatId)
        .set(mapOf("eventId" to "event-test", "messages" to listOf(mapOf("messageId" to "m1"))))
        .await()

    for (chatId in
        listOf(
            missingEventChatId,
            memberChatId,
            invalidMemberChatId,
            messageChatId,
            malformedMessageChatId,
        )) {
      val error =
          assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.getChat(chatId) }
          }
      assertTrue(error.message!!.contains(chatId))
    }
  }
}
