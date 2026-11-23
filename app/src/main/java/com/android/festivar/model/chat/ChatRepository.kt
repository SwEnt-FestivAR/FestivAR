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
   * @return The chats for the event.
   */
  suspend fun getChatsForEvent(eventId: String): List<Chat>

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
   * Adds a message to a chat.
   *
   * @param chatId The unique identifier of the chat.
   * @param message The message to add. Its [ChatMessage.messageId] must be unique in the chat.
   * @throws IllegalArgumentException if the message ID is duplicated.
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

  /**
   * Adds a member to a chat.
   *
   * @param chatId The unique identifier of the chat.
   * @param userId The unique identifier of the user to add.
   * @throws IllegalArgumentException if the user is already a member or has a blank identifier.
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun addMember(chatId: String, userId: String)

  /**
   * Removes a member from a chat.
   *
   * @param chatId The unique identifier of the chat.
   * @param userId The unique identifier of the user to remove.
   * @throws IllegalArgumentException if the user is not a member.
   * @throws NoSuchElementException if the chat is not found.
   */
  suspend fun removeMember(chatId: String, userId: String)
}
