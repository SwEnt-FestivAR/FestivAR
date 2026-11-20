package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import android.util.Log
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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the Event Details screen.
 *
 * @property event the loaded event, or null until loading succeeds.
 * @property errorMsg the string resource of the latest loading or deletion failure, or null.
 * @property isOwner whether the current user organizes the loaded event; only then the screen
 *   offers to edit or delete it.
 * @property isDeleting true while the event is being deleted, so a second tap does nothing.
 */
data class EventDetailsUIState(
    val event: Event? = null,
    @StringRes val errorMsg: Int? = null,
    val isOwner: Boolean = false,
    val isDeleting: Boolean = false,
) {
  /** Whether the event can be deleted now: only by its organizer, and once at a time. */
  val canDelete: Boolean
    get() = isOwner && !isDeleting
}

private const val LOG_TAG = "EventDetailsViewModel"

/**
 * Loads and exposes the event shown by the Event Details screen.
 *
 * Loading starts when the screen calls [fetchEvent]. A failed reload clears the currently displayed
 * event and exposes [EventDetailsUIState.errorMsg] so the screen can show an error. Its organizer
 * can also delete the event with [deleteEvent].
 *
 * @param dataRepository repository giving access to the events repository used to retrieve the
 *   event, and deleting the event with its tasks and the links of its members.
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

  /**
   * Loads [eventId] and updates the UI state with either the event or a loading error. Does nothing
   * while the event is being deleted.
   */
  fun fetchEvent() {
    if (_uiState.value.isDeleting) return
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

  /**
   * Deletes the event, its tasks and the links of its members, then calls [onDeleted]. A failure
   * keeps the event shown and sets [EventDetailsUIState.errorMsg]. Does nothing unless
   * [EventDetailsUIState.canDelete].
   */
  fun deleteEvent(onDeleted: () -> Unit = {}) {
    val before = _uiState.getAndUpdate {
      if (it.canDelete) it.copy(isDeleting = true, errorMsg = null) else it
    }
    if (!before.canDelete) return

    viewModelScope.launch {
      try {
        dataRepository.deleteEvent(eventId)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isDeleting = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error deleting event $eventId", e)
        _uiState.update {
          it.copy(isDeleting = false, errorMsg = R.string.event_details_delete_error)
        }
        return@launch
      }
      _uiState.update { it.copy(isDeleting = false) }
      onDeleted()
    }
  }
}
