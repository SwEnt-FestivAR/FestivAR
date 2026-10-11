// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.CODE_LENGTH
import com.android.festivar.model.event.EventsRepository
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
 * @property dateLine when it starts, e.g. `Sat 17 Oct, 10:00`.
 * @property members how many people already belong to it.
 */
data class FoundEventUi(val id: String, val name: String, val dateLine: String, val members: Int)

/**
 * UI state of the join screen.
 *
 * @property code what was typed: uppercase letters and digits, at most six.
 * @property notFound true when six characters match no event.
 * @property joined true once the person is a member; the screen then opens the event.
 */
data class JoinEventUiState(
    val code: String = "",
    val found: FoundEventUi? = null,
    val notFound: Boolean = false,
    val isJoining: Boolean = false,
    val joined: Boolean = false,
    val errorMsg: String? = null,
)

/** Looks an event up by its code as it is typed, and joins it as the signed-in [user]. */
class JoinEventViewModel(
    private val user: User,
    private val eventsRepository: EventsRepository,
    private val usersRepository: UsersRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(JoinEventUiState())
  val uiState: StateFlow<JoinEventUiState> = _uiState.asStateFlow()

  /** Keeps the letters and digits of [raw], uppercased, and looks the code up once complete. */
  fun setCode(raw: String) {
    val code = raw.uppercase().filter { it.isLetterOrDigit() }.take(CODE_LENGTH)
    _uiState.update { it.copy(code = code, found = null, notFound = false, errorMsg = null) }
    if (code.length < CODE_LENGTH) return
    viewModelScope.launch {
      try {
        val found =
            eventsRepository
                .getAllEvents()
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
        _uiState.update { it.copy(errorMsg = e.message ?: "Could not find the event") }
      }
    }
  }

  /** Adds the signed-in person to the found event, unless they already belong to it. */
  fun join() {
    val state = _uiState.value
    val found = state.found ?: return
    if (state.isJoining) return
    _uiState.update { it.copy(isJoining = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        val event = eventsRepository.getEvent(found.id)
        if (event.members.none { it.uid == user.uid }) {
          usersRepository.editUser(user.uid, user.addJoinedEvent(event.eventId))
          eventsRepository.editEvent(event.eventId, event.addMember(user))
        }
        _uiState.update { it.copy(isJoining = false, joined = true) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isJoining = false, errorMsg = e.message ?: "Could not join the event")
        }
      }
    }
  }

  /** Dismisses the error once the screen has shown it. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
