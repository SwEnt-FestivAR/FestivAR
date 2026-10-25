// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Event
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
 * An event a typed code matched.
 *
 * @property id the event identifier, passed back to [JoinEventViewModel.join] it.
 * @property name the event title shown in the result card.
 * @property dateLine when it starts, e.g. `Sat 17 Oct, 10:00`.
 * @property members how many people already belong to it.
 */
data class FoundEventUi(val id: String, val name: String, val dateLine: String, val members: Int)

/**
 * UI state of the join screen.
 *
 * @property code what was typed: uppercase letters and digits, at most six.
 * @property matches every event the code matched, one card each; usually one, more when two event
 *   ids share their first six letters and digits, so the person picks the right one.
 * @property notFound true when six characters match no event.
 * @property isJoining whether a join is in progress, so a second tap is ignored.
 * @property joined true once the person is a member; the screen then opens the event.
 * @property joinedEventId the id of the event just joined, consumed by
 *   [JoinEventViewModel.onEventOpened] once the screen opened it.
 * @property errorMsg an error message to show when the lookup or the join fails, or `null`.
 */
data class JoinEventUIState(
    val code: String = "",
    val matches: List<FoundEventUi> = emptyList(),
    val notFound: Boolean = false,
    val isJoining: Boolean = false,
    val joined: Boolean = false,
    val joinedEventId: String? = null,
    val errorMsg: String? = null,
)

/**
 * Looks events up by their code as it is typed, and joins the chosen one as the signed-in [user].
 *
 * @property user The signed-in [User] who joins.
 * @property eventsRepository The repository used to look the events up and to add the member.
 * @property usersRepository The repository holding the [User], whose joined events are updated.
 */
class JoinEventViewModel(
    private val user: User,
    private val eventsRepository: EventsRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(JoinEventUIState())
  val uiState: StateFlow<JoinEventUIState> = _uiState.asStateFlow()

  /** Keeps the letters and digits of [raw], uppercased, and looks the code up once complete. */
  fun setCode(raw: String) {
    val code = raw.uppercase().filter { it.isLetterOrDigit() }.take(JOIN_CODE_LENGTH)
    val current = _uiState.value
    // A join keeps its code, and a keystroke that filtering drops changes nothing, unless the
    // last attempt failed: retyping the same code then retries.
    if (current.isJoining || current.joined) return
    if (code == current.code && current.errorMsg == null) return
    _uiState.update {
      it.copy(code = code, matches = emptyList(), notFound = false, errorMsg = null)
    }
    if (code.length == JOIN_CODE_LENGTH) viewModelScope.launch { lookUp(code) }
  }

  /** Lists every event whose code is [code], unless the code changed while the lookup ran. */
  private suspend fun lookUp(code: String) {
    try {
      val matches = eventsRepository.getAllEvents().filter { joinCodeOf(it) == code }.map(::cardOf)
      updateIfStillTyped(code) { it.copy(matches = matches, notFound = matches.isEmpty()) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      updateIfStillTyped(code) { it.copy(errorMsg = e.message ?: "Could not find the event") }
    }
  }

  /**
   * Applies [change] only while [code] is still the typed one: a later keystroke owns the state.
   */
  private fun updateIfStillTyped(code: String, change: (JoinEventUIState) -> JoinEventUIState) {
    _uiState.update { if (it.code == code) change(it) else it }
  }

  private fun cardOf(event: Event) =
      FoundEventUi(
          id = event.eventId,
          name = event.title,
          dateLine =
              "${formatDate(event.startDate.toLocalDate())}, " +
                  formatTime(event.startDate.toLocalTime()),
          members = event.members.size,
      )

  /**
   * Makes the signed-in person a member of the matched event [eventId], writing only what is still
   * missing. Ignored unless [eventId] is among [JoinEventUIState.matches] and no join is running.
   */
  fun join(eventId: String) {
    val state = _uiState.value
    if (state.isJoining || state.matches.none { it.id == eventId }) return
    _uiState.update { it.copy(isJoining = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        // Both sides are read again and only the missing one is written, so a join that stopped
        // halfway earlier is repaired instead of failing as "already joined" forever.
        val stored = usersRepository.getUser(user.uid)
        val event = eventsRepository.getEvent(eventId)
        if (event.members.none { it.uid == stored.uid }) {
          eventsRepository.editEvent(event.eventId, event.addMember(stored))
        }
        if (event.eventId !in stored.joinedEvents) {
          usersRepository.editUser(stored.uid, stored.addJoinedEvent(event.eventId))
        }
        _uiState.update { it.copy(isJoining = false, joined = true, joinedEventId = eventId) }
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
