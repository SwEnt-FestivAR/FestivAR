// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import java.time.Clock
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "ChatViewModel"

/**
 * UI state of the chat screen.
 *
 * @property chat The loaded chat, or null until loaded.
 * @property authors Map of author user IDs to their [User] objects.
 * @property isLoading Whether the chat is currently being loaded.
 * @property isSending Whether a message is currently being sent.
 * @property errorMsg The string resource of the latest load or send failure, or null.
 */
data class ChatUiState(
    val chat: Chat? = null,
    val authors: Map<String, User> = emptyMap(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    @param:StringRes val errorMsg: Int? = null,
) {
  /** Whether messages can be sent. */
  val canSend: Boolean
    get() = (chat != null) && (!isLoading && !isSending)
}

/**
 * ViewModel for the chat screen.
 *
 * Manages loading chat messages and members, and sending new messages through [ChatRepository].
 *
 * @property chatId Unique identifier of the chat.
 * @property dataRepository Gives access to [ChatRepository] and [UsersRepository].
 * @property currentUserId Unique identifier of the signed-in user sending messages.
 * @property clock Clock used for timestamping new messages.
 */
class ChatViewModel(
    private val chatId: String,
    private val dataRepository: DataRepository,
    val currentUserId: String,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

  private val chatRepository: ChatRepository =
      dataRepository.chatRepository
          ?: throw IllegalStateException("Chat repository is not configured.")
  private val usersRepository: UsersRepository = dataRepository.usersRepository

  init {
    require(chatId.isNotBlank()) { "chatId must not be blank" }
    require(currentUserId.isNotBlank()) { "currentUserId must not be blank" }
  }

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  /** Loads the chat and the user profiles of its members and message authors. */
  fun loadChat() {
    if (_uiState.value.isSending || (loadJob?.isActive == true)) return
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val chat = fetchOrCreateChat(chatId, currentUserId)
        val userIds = (chat.members + chat.messages.map { it.authorId }).distinct()
        val authorsMap = fetchUserMap(userIds)

        _uiState.update {
          it.copy(
              chat = chat,
              authors = authorsMap,
              isLoading = false,
              errorMsg = null,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading chat $chatId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = R.string.chat_load_failed) }
      }
    }
  }

  private suspend fun fetchOrCreateChat(chatId: String, currentUserId: String): Chat {
    return try {
      chatRepository.getChat(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      chatRepository.getChatsForEvent(chatId).firstOrNull()
          ?: createInitialChat(chatId, currentUserId)
    }
  }

  private suspend fun createInitialChat(chatId: String, currentUserId: String): Chat {
    val newChat = Chat(chatId = chatId, eventId = chatId, members = listOf(currentUserId))
    runCatching { chatRepository.addChat(newChat) }
    return newChat
  }

  private suspend fun fetchUserMap(userIds: List<String>): Map<String, User> {
    if (userIds.isEmpty()) return emptyMap()
    val users = runCatching { usersRepository.getListUsers(userIds) }.getOrElse { emptyList() }
    return users.associateBy { it.uid }
  }

  /**
   * Sends a message with the given [content] to the chat.
   *
   * Generates a unique message ID and uses [currentUserId] as the author.
   */
  fun sendMessage(content: MessageContent) {
    if (!_uiState.value.canSend) return

    val currentChat = _uiState.value.chat ?: return
    val targetChatId = currentChat.chatId

    _uiState.update { it.copy(isSending = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        val messageId = chatRepository.getNewUid()
        val message =
            ChatMessage(
                messageId = messageId,
                authorId = currentUserId,
                sentAt = ZonedDateTime.now(clock),
                content = content,
            )
        try {
          chatRepository.addMessage(targetChatId, message)
        } catch (_: NoSuchElementException) {
          chatRepository.addChat(currentChat.addMessage(message))
        }

        val updatedChat =
            try {
              chatRepository.getChat(targetChatId)
            } catch (_: Exception) {
              currentChat.addMessage(message)
            }

        val userIds = (updatedChat.members + updatedChat.messages.map { it.authorId }).distinct()
        val users =
            try {
              usersRepository.getListUsers(userIds)
            } catch (_: Exception) {
              emptyList()
            }
        val authorsMap = users.associateBy { it.uid }

        _uiState.update {
          it.copy(
              chat = updatedChat,
              authors = authorsMap,
              isSending = false,
          )
        }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isSending = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error sending message to chat $targetChatId", e)
        _uiState.update { it.copy(isSending = false, errorMsg = R.string.chat_send_failed) }
      }
    }
  }

  /** Clears [ChatUiState.errorMsg]. */
  fun clearError() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
