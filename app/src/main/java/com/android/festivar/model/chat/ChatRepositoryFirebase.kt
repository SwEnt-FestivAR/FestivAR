// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val CHAT_COLLECTION_PATH = "chats"

/**
 * Firestore-backed [ChatRepository] that persists chats and their messages and members.
 *
 * @param db Firestore instance used to access chat documents.
 */
class ChatRepositoryFirebase(private val db: FirebaseFirestore) : ChatRepository {
  private val chatCollection
    get() = db.collection(CHAT_COLLECTION_PATH)

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllChats(): List<Chat> {
    return chatCollection.get().await().documents.map { documentToChat(it) }
  }

  override suspend fun getChat(chatId: String): Chat {
    val document = chatCollection.document(chatId).get().await()
    if (!document.exists()) {
      throw NoSuchElementException("Chat '$chatId' does not exist.")
    }
    return documentToChat(document)
  }

  override suspend fun getChatsForEvent(eventId: String): List<Chat> =
      chatCollection.whereEqualTo("eventId", eventId).get().await().documents.map {
        documentToChat(it)
      }

  override suspend fun addChat(chat: Chat) {
    val document = chatCollection.document(chat.chatId)
    db.runTransaction { transaction ->
          require(!transaction.get(document).exists()) {
            "A Chat with chatId '${chat.chatId}' already exists."
          }
          transaction.set(document, toFirestoreData(chat))
        }
        .await()
  }

  override suspend fun editChat(chatId: String, newValue: Chat) {
    require(chatId == newValue.chatId) { "The edited Chat must keep its existing chatId." }
    val document = chatCollection.document(chatId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Chat '$chatId' does not exist.")
          }
          transaction.update(document, toFirestoreData(newValue))
        }
        .await()
  }

  override suspend fun deleteChat(chatId: String) {
    val document = chatCollection.document(chatId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Chat '$chatId' does not exist.")
          }
          transaction.delete(document)
        }
        .await()
  }

  override suspend fun addMessage(chatId: String, message: ChatMessage) {
    updateChat(chatId) { it.addMessage(message) }
  }

  override suspend fun deleteMessage(chatId: String, messageId: String) {
    updateChat(chatId) { it.deleteMessage(messageId) }
  }

  override suspend fun addMember(chatId: String, userId: String) {
    updateChat(chatId) { it.addMember(userId) }
  }

  override suspend fun removeMember(chatId: String, userId: String) {
    updateChat(chatId) { it.removeMember(userId) }
  }

  private suspend fun updateChat(chatId: String, change: (Chat) -> Chat) {
    val document = chatCollection.document(chatId)
    db.runTransaction { transaction ->
          val snapshot = transaction.get(document)
          if (!snapshot.exists()) {
            throw NoSuchElementException("Chat '$chatId' does not exist.")
          }
          transaction.update(
              document,
              toFirestoreData(change(documentToChat(snapshot))),
          )
        }
        .await()
  }

  private fun toFirestoreData(chat: Chat): Map<String, Any> =
      mapOf(
          "chatId" to chat.chatId,
          "eventId" to chat.eventId,
          "members" to chat.members,
          "messages" to chat.messages.map { it.toMap() },
      )

  private fun documentToChat(document: DocumentSnapshot): Chat {
    check(document.exists()) { "Chat '${document.id}' does not exist." }

    val eventId =
        document.getString("eventId")
            ?: throw IllegalArgumentException("Chat '${document.id}' has missing eventId.")

    val members =
        when (val storedMembers = document.get("members")) {
          null -> emptyList()
          is List<*> ->
              storedMembers.map { value ->
                value as? String
                    ?: throw IllegalArgumentException(
                        "Invalid chat member data for chat '${document.id}': $value"
                    )
              }
          else ->
              throw IllegalArgumentException(
                  "Invalid chat member data for chat '${document.id}': expected a list."
              )
        }

    val messages =
        when (val storedMessages = document.get("messages")) {
          null -> emptyList()
          is List<*> ->
              storedMessages.map { value ->
                val map =
                    value as? Map<*, *>
                        ?: throw IllegalArgumentException(
                            "Invalid chat message data for chat '${document.id}'."
                        )
                try {
                  ChatMessage.fromMap(map)
                } catch (e: IllegalArgumentException) {
                  throw IllegalArgumentException(
                      "Invalid chat message data for chat '${document.id}': ${e.message}",
                      e,
                  )
                }
              }
          else ->
              throw IllegalArgumentException(
                  "Invalid chat message data for chat '${document.id}': expected a list."
              )
        }

    return Chat(
        chatId = document.getString("chatId") ?: document.id,
        eventId = eventId,
        messages = messages,
        members = members,
    )
  }
}
