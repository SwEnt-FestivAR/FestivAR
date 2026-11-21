// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import java.util.UUID

/**
 * Represents a repository that manages a local list of chats.
 *
 * @param initialChats The [Chat] items the repository starts with. Their chat identifiers must be
 *   unique.
 */
class ChatRepositoryLocal(initialChats: List<Chat> = emptyList()) : ChatRepository {
  private val chats: MutableList<Chat> = initialChats.toMutableList()

  init {
    require(chats.map { it.chatId }.distinct().size == chats.size) {
      "ChatRepositoryLocal: initialChats contains two Chats with the same chatId"
    }
  }

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun getAllChats(): List<Chat> = chats.toList()

  override suspend fun getChat(chatId: String): Chat =
      chats.find { it.chatId == chatId }
          ?: throw NoSuchElementException("ChatRepositoryLocal: Chat $chatId not found")

  override suspend fun getChatForEvent(eventId: String): Chat =
      chats.find { it.eventId == eventId }
          ?: throw NoSuchElementException("ChatRepositoryLocal: Chat for event $eventId not found")

  override suspend fun addChat(chat: Chat) {
    require(chats.none { it.chatId == chat.chatId }) {
      "ChatRepositoryLocal: a Chat with chatId ${chat.chatId} already exists"
    }
    chats.add(chat)
  }

  override suspend fun editChat(chatId: String, newValue: Chat) {
    require(newValue.chatId == chatId) {
      "ChatRepositoryLocal: newValue.chatId (${newValue.chatId}) must be equal to chatId " +
          "($chatId)"
    }

    val index = chats.indexOfFirst { it.chatId == chatId }
    if (index != -1) {
      chats[index] = newValue
    } else {
      throw NoSuchElementException("ChatRepositoryLocal: Chat $chatId not found")
    }
  }

  override suspend fun deleteChat(chatId: String) {
    val index = chats.indexOfFirst { it.chatId == chatId }
    if (index != -1) {
      chats.removeAt(index)
    } else {
      throw NoSuchElementException("ChatRepositoryLocal: Chat $chatId not found")
    }
  }

  override suspend fun addMessage(chatId: String, message: ChatMessage) {
    val chat = getChat(chatId)
    editChat(chatId, chat.addMessage(message))
  }

  override suspend fun deleteMessage(chatId: String, messageId: String) {
    val chat = getChat(chatId)
    editChat(chatId, chat.deleteMessage(messageId))
  }
}
