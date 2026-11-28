// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.chat.overview

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "ChatsOverviewViewModel"

// Temporary: a Chat has no name in the model yet, so every chat is shown with this one. It goes
// away when Chat has a name.
private const val PLACEHOLDER_CHAT_NAME = "General"

/**
 * One chat of the overview, with what its row shows about its last message.
 *
 * @property chatId The unique identifier of the chat.
 * @property name The name shown for the chat. "General" for every chat until `Chat` has a name.
 * @property lastMessage The most recent message of the chat, with its `sentAt` in the time zone of
 *   the ViewModel's clock, or `null` if nobody wrote yet.
 * @property lastMessageAuthor The stored [User] who sent [lastMessage], or `null` without message.
 * @property isLastMessageOwn Whether the signed-in user sent [lastMessage], so that the row can say
 *   "You" instead of their name.
 */
data class ChatsOverviewItem(
    val chatId: String,
    val name: String,
    val lastMessage: ChatMessage? = null,
    val lastMessageAuthor: User? = null,
    val isLastMessageOwn: Boolean = false,
)

/**
 * Represents the UI state of the chats overview of an event.
 *
 * @property eventTitle The title of the event, for the title of the screen. Empty until the first
 *   successful fetch.
 * @property generalChat The chat of every member of the event, shown under "Everyone", or `null` if
 *   it is not loaded yet or the event has none.
 * @property yourChats The other chats of the event the signed-in user is a member of, the one with
 *   the most recent message first, and those without message last.
 * @property isLoading Whether the chats are currently being fetched.
 * @property errorMsg The string resource of the failed fetch, or `null` if there is no error. It
 *   stays until a fetch succeeds, so the screen can keep showing the error with a retry.
 */
data class ChatsOverviewUIState(
    val eventTitle: String = "",
    val generalChat: ChatsOverviewItem? = null,
    val yourChats: List<ChatsOverviewItem> = emptyList(),
    val isLoading: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /**
   * Whether there is no chat at all to show, neither the general one nor one of the user. Also
   * `true` during the first load, so the screen checks [isLoading] first to not flash its empty
   * state.
   */
  val hasNoChats: Boolean
    get() = generalChat == null && yourChats.isEmpty()
}

/**
 * ViewModel of the chats overview of an event.
 *
 * Manages the UI state by fetching the chats of an event through the [ChatRepository] of
 * [dataRepository], following the announcement feed: nothing is fetched until [refreshUIState] is
 * called, which the screen does each time it is shown, when the user pulls to refresh and when they
 * click on "Retry". Until the first fetch, the UI state is already loading.
 *
 * The general chat is the one every event is created with: its identifier is the identifier of the
 * event. Every member of the event sees it, whoever its members are. The other chats are shown only
 * to their members.
 *
 * @property eventId The unique identifier of the event whose chats are shown.
 * @property dataRepository Gives the repositories of the chats, the events and the users.
 * @property currentUserId The unique identifier of the signed-in user.
 * @property clock Gives the time zone of the last messages' `sentAt`. The device's clock by
 *   default.
 */
class ChatsOverviewViewModel(
    private val eventId: String,
    private val dataRepository: DataRepository,
    private val currentUserId: String,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository
  private val usersRepository: UsersRepository = dataRepository.usersRepository

  // Loading from the start: nothing is fetched until the screen calls refreshUIState on resume, and
  // until then the screen must show its progress indicator, not its empty state.
  private val _uiState = MutableStateFlow(ChatsOverviewUIState(isLoading = true))
  val uiState: StateFlow<ChatsOverviewUIState> = _uiState.asStateFlow()

  /** The fetch of the chats in progress, cancelled when a newer one starts. */
  private var getChatsJob: Job? = null

  /**
   * Refreshes the UI state by fetching the chats from the repository, e.g. when the screen is shown
   * again, or when the user pulls to refresh or retries.
   */
  fun refreshUIState() {
    getChats()
  }

  /**
   * Fetches the event, its chats and the authors of their last messages, and updates the UI state.
   * Cancels the previous fetch, so that an older result never overwrites a newer one. A failure
   * keeps the chats already shown, and the error stays until a fetch succeeds.
   */
  private fun getChats() {
    getChatsJob?.cancel()
    _uiState.update { it.copy(isLoading = true) }
    getChatsJob = viewModelScope.launch {
      try {
        // Not every data repository has chats yet. Without them the fetch fails like any other, so
        // the screen shows its error instead of crashing.
        val chatRepository: ChatRepository =
            checkNotNull(dataRepository.chatRepository) { "Chat repository is not configured." }
        val event = eventsRepository.getEvent(eventId)
        val chats = chatRepository.getChatsForEvent(eventId)
        val generalChat = chats.firstOrNull { it.chatId == eventId }
        val memberChats = chats.filter { it.chatId != eventId && currentUserId in it.members }
        // Only the chats that are shown need the author of their last message: a chat the user is
        // not a member of must neither cost a user fetch nor fail the overview.
        val authorIds =
            (listOfNotNull(generalChat) + memberChats)
                .mapNotNull { it.messages.lastOrNull()?.authorId }
                .distinct()
        val authors = usersRepository.getListUsers(authorIds).associateBy { it.uid }
        // A newer fetch may have cancelled this one while the repositories were fetching. A
        // repository that ignores cancellation still returns its data, so stop here before these
        // stale chats reach the UI state.
        ensureActive()

        val yourChats =
            memberChats
                .map { it.toItem(authors) }
                .sortedByDescending { it.lastMessage?.sentAt?.toInstant() ?: Instant.MIN }
        _uiState.update {
          it.copy(
              eventTitle = event.title,
              generalChat = generalChat?.toItem(authors),
              yourChats = yourChats,
              isLoading = false,
              errorMsg = null,
          )
        }
      } catch (e: Exception) {
        // A cancelled fetch must not touch the state, whatever it failed with. A
        // CancellationException is never a load error, even from a repository, so rethrow it.
        ensureActive()
        if (e is CancellationException) throw e

        Log.e(LOG_TAG, "Error loading the chats of event $eventId", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = R.string.chats_overview_load_failed)
        }
      }
    }
  }

  /**
   * What the row of this chat shows. The messages of a [Chat] are sorted oldest first, so the last
   * one is the most recent.
   */
  private fun Chat.toItem(authors: Map<String, User>): ChatsOverviewItem {
    val lastMessage = messages.lastOrNull()
    return ChatsOverviewItem(
        chatId = chatId,
        name = PLACEHOLDER_CHAT_NAME,
        lastMessage =
            lastMessage?.copy(sentAt = lastMessage.sentAt.withZoneSameInstant(clock.zone)),
        lastMessageAuthor = lastMessage?.let { authors[it.authorId] },
        isLastMessageOwn = lastMessage?.authorId == currentUserId,
    )
  }
}
