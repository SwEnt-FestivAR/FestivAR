// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

/** Represents a repository that manages conversations and their messages. */
interface ChatRepository {

  /** Generates and returns a new unique identifier for a chat or chat message. */
  fun getNewUid(): String

  /**
   * Retrieves all chats in the repository.
   *
   * @return A list of all chats.
   */
  suspend fun getAllChats(): List<Chat>

  /**
   * Retrieves a chat by its unique identifier.
   *
   * @param chatId The unique identifier of the chat.
   * @return The chat with the specified identifier.
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun getChat(chatId: String): Chat

  /**
   * Retrieves the chat belonging to an event.
   *
   * @param eventId The unique identifier of the event.
   * @return The chat for the event.
   * @throws NoSuchElementException if the event has no chat.
   */
  suspend fun getChatForEvent(eventId: String): Chat

  /**
   * Adds a new chat to the repository.
   *
   * @param chat The chat to add. Its [Chat.chatId] must not already be used.
   * @throws IllegalArgumentException if a chat with the same identifier already exists.
   */
  suspend fun addChat(chat: Chat)

  /**
   * Edits an existing chat.
   *
   * @param chatId The unique identifier of the chat to edit.
   * @param newValue The new value for the chat. Its [Chat.chatId] must equal [chatId].
   * @throws IllegalArgumentException if [newValue] does not keep [chatId].
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun editChat(chatId: String, newValue: Chat)

  /**
   * Deletes a chat and all of its messages.
   *
   * @param chatId The unique identifier of the chat to delete.
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun deleteChat(chatId: String)

  /**
   * Appends a message to a chat.
   *
   * @param chatId The unique identifier of the chat.
   * @param message The message to add. Its [ChatMessage.messageId] must be unique in the chat.
   * @throws IllegalArgumentException if the message ID is duplicated or the message is out of
   *   chronological order.
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun addMessage(chatId: String, message: ChatMessage)

  /**
   * Deletes a message from a chat.
   *
   * @param chatId The unique identifier of the chat.
   * @param messageId The unique identifier of the message to delete.
   * @throws IllegalArgumentException if the message is not in the chat.
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun deleteMessage(chatId: String, messageId: String)
}
