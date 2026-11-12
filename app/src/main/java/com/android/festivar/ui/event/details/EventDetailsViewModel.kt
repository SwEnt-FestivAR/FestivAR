package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the Event Details screen.
 *
 * @property event the loaded event, or null until loading succeeds.
 * @property errorMsg the string resource of the latest loading failure, or null.
 * @property isOwner whether the current user organizes the loaded event; only then the screen
 *   offers to edit it.
 */
data class EventDetailsUIState(
    val event: Event? = null,
    @StringRes val errorMsg: Int? = null,
    val isOwner: Boolean = false,
)

/**
 * Loads and exposes the event shown by the Event Details screen.
 *
 * Loading starts when the screen calls [fetchEvent]. A failed reload clears the currently displayed
 * event and exposes [EventDetailsUIState.errorMsg] so the screen can show an error.
 *
 * @param dataRepository repository giving access to the events repository used to retrieve the
 *   event.
 * @param eventId identifier of the event to load.
 * @param currentUserId identifier of the signed-in user, who owns the event when it is the event's
 *   [Event.ownerId].
 */
class EventDetailsViewModel(
    private val dataRepository: DataRepository,
    private val eventId: String,
    private val currentUserId: String,
) : ViewModel() {

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository

  private val _uiState = MutableStateFlow(EventDetailsUIState())
  val uiState: StateFlow<EventDetailsUIState> = _uiState.asStateFlow()

  /** Loads [eventId] and updates the UI state with either the event or a loading error. */
  fun fetchEvent() {
    viewModelScope.launch {
      _uiState.update { EventDetailsUIState() }
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update { it.copy(event = event, isOwner = event.ownerId == currentUserId) }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        _uiState.update { it.copy(errorMsg = R.string.event_details_fetch_error) }
      }
    }
  }
}
