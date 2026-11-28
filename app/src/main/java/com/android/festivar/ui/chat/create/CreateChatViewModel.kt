// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.chat.create

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import com.android.festivar.ui.chat.ChatFormEditor
import com.android.festivar.ui.chat.ChatFormState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State displayed by the Create chat screen. */
data class CreateChatUiState(
    val event: Event? = null,
    val form: ChatFormState = ChatFormState(),
    val members: List<User> = emptyList(),
    /** Selected IDs exclude the current user, who is included automatically. */
    val selectedMemberIds: List<String> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isCreated: Boolean = false,
    @param:StringRes val errorMsg: Int? = null,
) {
  /** Total selected members, including the current user. */
  val selectedMemberCount: Int
    get() = selectedMemberIds.size + if (currentUserId.isNotBlank()) 1 else 0

  /** Whether the required name and at least one other event member are selected. */
  val canCreate: Boolean
    get() {
      val eventMemberIds = members.mapTo(mutableSetOf()) { it.uid }
      return event != null &&
          form.isValid &&
          currentUserId in eventMemberIds &&
          selectedMemberIds.isNotEmpty() &&
          selectedMemberIds.all { it != currentUserId && it in eventMemberIds } &&
          !isLoading &&
          !isCreating &&
          !isCreated
    }

  /** Whether the form can still be changed. */
  val isEditable: Boolean
    get() = !isCreating && !isCreated
}

private const val TAG = "CreateChatViewModel"

/**
 * Loads an event's members and creates a named chat for that event.
 *
 * @param eventId Event whose members can join the chat. Must not be blank.
 * @param currentUserId Signed-in member, automatically included in the new chat. Must not be blank.
 * @param dataRepository Repositories used to load the event and create the linked chat.
 */
class CreateChatViewModel(
    private val eventId: String,
    private val currentUserId: String,
    private val dataRepository: DataRepository,
) : ViewModel(), ChatFormEditor {

  init {
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(currentUserId.isNotBlank()) { "currentUserId must not be blank" }
  }

  private val _uiState = MutableStateFlow(CreateChatUiState(currentUserId = currentUserId))

  /** The state of the form and event members, read by the screen. */
  val uiState: StateFlow<CreateChatUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  /** Loads the event once; a failed load can be retried without losing the form name. */
  fun load() {
    if (loadJob?.isActive == true || _uiState.value.event != null) return
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    loadJob = viewModelScope.launch {
      try {
        val event = dataRepository.eventsRepository.getEvent(eventId)
        check(event.members.any { it.uid == currentUserId }) {
          "Current user $currentUserId is not a member of event $eventId."
        }
        _uiState.update {
          it.copy(
              event = event,
              members = event.members,
              isLoading = false,
              errorMsg = null,
          )
        }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isLoading = false) }
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error loading event $eventId for chat creation", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = R.string.create_chat_error_load_failed)
        }
      }
    }
  }

  override fun applyEdit(change: (ChatFormState) -> ChatFormState) {
    _uiState.update { if (!it.isEditable) it else it.copy(form = change(it.form), errorMsg = null) }
  }

  /**
   * Selects or deselects an event member. The current user stays included automatically.
   *
   * @param userId ID of the event member to toggle.
   */
  fun toggleMember(userId: String) {
    _uiState.update { state ->
      if (
          !state.isEditable ||
              state.event == null ||
              userId == currentUserId ||
              state.members.none { it.uid == userId }
      ) {
        state
      } else {
        val selected =
            if (userId in state.selectedMemberIds) {
              state.selectedMemberIds - userId
            } else {
              state.selectedMemberIds + userId
            }
        state.copy(selectedMemberIds = selected, errorMsg = null)
      }
    }
  }

  /** Creates the chat once and links it to its event through [DataRepository]. */
  fun createChat() {
    val before = _uiState.getAndUpdate { state ->
      if (state.canCreate) state.copy(isCreating = true, errorMsg = null) else state
    }
    if (!before.canCreate) return

    viewModelScope.launch {
      try {
        val chatRepository =
            dataRepository.chatRepository
                ?: throw IllegalStateException("Chat repository is not configured.")
        val memberIds =
            before.members
                .asSequence()
                .map { it.uid }
                .filter { it == currentUserId || it in before.selectedMemberIds }
                .toList()
        val chat =
            Chat(
                chatId = chatRepository.getNewUid(),
                eventId = eventId,
                name = before.form.trimmedName,
                members = memberIds,
            )
        dataRepository.createChat(chat)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isCreating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error creating chat for event $eventId", e)
        _uiState.update {
          it.copy(isCreating = false, errorMsg = R.string.create_chat_error_save_failed)
        }
        return@launch
      }
      _uiState.update { it.copy(isCreating = false, isCreated = true, errorMsg = null) }
    }
  }
}
