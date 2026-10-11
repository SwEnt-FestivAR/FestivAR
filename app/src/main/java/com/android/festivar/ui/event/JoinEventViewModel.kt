// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.JOIN_CODE_LENGTH
import com.android.festivar.model.event.joinCodeOf
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The event a typed code matched.
 *
 * @property id the event identifier, passed back to join it.
 * @property name the event title shown in the result card.
 * @property dateLine when it starts, e.g. `Sat 17 Oct, 10:00`.
 * @property members how many people already belong to it.
 */
data class FoundEventUi(val id: String, val name: String, val dateLine: String, val members: Int)

/**
 * UI state of the join screen.
 *
 * @property code what was typed: uppercase letters and digits, at most six.
 * @property notFound true when six characters match no event.
 * @property found the event the code matched, or `null` while none did.
 * @property isJoining whether a join is in progress, so a second tap is ignored.
 * @property joined true once the person is a member; the screen then opens the event.
 * @property joinedEventId the id of the event just joined, consumed by
 *   [JoinEventViewModel.onEventOpened] once the screen opened it.
 * @property errorMsg an error message to show when the lookup or the join fails, or `null`.
 */
data class JoinEventUiState(
    val code: String = "",
    val found: FoundEventUi? = null,
    val notFound: Boolean = false,
    val isJoining: Boolean = false,
    val joined: Boolean = false,
    val joinedEventId: String? = null,
    val errorMsg: String? = null,
)

/**
 * Looks an event up by its code as it is typed, and joins it as the signed-in [user].
 *
 * @property user The signed-in [User] who joins.
 * @property eventsRepository The repository used to look the event up and to add the member.
 * @property usersRepository The repository holding the [User], whose joined events are updated.
 */
class JoinEventViewModel(
    private val user: User,
    private val eventsRepository: EventsRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(JoinEventUiState())
  val uiState: StateFlow<JoinEventUiState> = _uiState.asStateFlow()

  /** Keeps the letters and digits of [raw], uppercased, and looks the code up once complete. */
  fun setCode(raw: String) {
    val code = raw.uppercase().filter { it.isLetterOrDigit() }.take(JOIN_CODE_LENGTH)
    val current = _uiState.value
    // A join keeps its code, and a keystroke that filtering drops changes nothing, unless the
    // last attempt failed: retyping the same code then retries.
    if (current.isJoining || current.joined) return
    if (code == current.code && current.errorMsg == null) return
    _uiState.update { it.copy(code = code, found = null, notFound = false, errorMsg = null) }
    if (code.length < JOIN_CODE_LENGTH) return
    viewModelScope.launch {
      try {
        val found =
            eventsRepository
                .getAllEvents()
                // Two ids sharing their first six characters would give one event; negligible.
                .firstOrNull { joinCodeOf(it) == code }
                ?.let {
                  FoundEventUi(
                      id = it.eventId,
                      name = it.title,
                      dateLine =
                          "${formatDate(it.startDate.toLocalDate())}, " +
                              formatTime(it.startDate.toLocalTime()),
                      members = it.members.size,
                  )
                }
        // A later keystroke may have changed the code while this lookup ran.
        _uiState.update {
          if (it.code != code) it else it.copy(found = found, notFound = found == null)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // Same guard as the success path: an old code's failure is not the new code's.
        _uiState.update {
          if (it.code != code) it else it.copy(errorMsg = e.message ?: "Could not find the event")
        }
      }
    }
  }

  /** Makes the signed-in person a member of the found event, writing only what is still missing. */
  fun join() {
    val state = _uiState.value
    val found = state.found ?: return
    if (state.isJoining) return
    _uiState.update { it.copy(isJoining = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        // Both sides are read again and only the missing one is written, so a join that stopped
        // halfway earlier is repaired instead of failing as "already joined" forever.
        val stored = usersRepository.getUser(user.uid)
        val event = eventsRepository.getEvent(found.id)
        if (event.members.none { it.uid == stored.uid }) {
          eventsRepository.editEvent(event.eventId, event.addMember(stored))
        }
        if (event.eventId !in stored.joinedEvents) {
          usersRepository.editUser(stored.uid, stored.addJoinedEvent(event.eventId))
        }
        _uiState.update { it.copy(isJoining = false, joined = true, joinedEventId = found.id) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isJoining = false, errorMsg = e.message ?: "Could not join the event")
        }
      }
    }
  }

  /** Called by the screen once it navigated, so a re-entry does not open the event again. */
  fun onEventOpened() {
    _uiState.update { it.copy(joined = false, joinedEventId = null) }
  }
}
