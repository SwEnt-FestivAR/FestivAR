package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI state of the Event Details screen.
 *
 * @property event the loaded event, or null until loading succeeds.
 * @property errorMsg the string resource of the latest loading failure, or null.
 * @property deleteRequested whether the deletion of the event waits for the user's confirmation.
 * @property isDeleting whether the confirmed deletion is running.
 * @property deleteErrorMsg the string resource of the latest deletion failure, or null.
 */
data class EventDetailsUIState(
    val event: Event? = null,
    @StringRes val errorMsg: Int? = null,
    val deleteRequested: Boolean = false,
    val isDeleting: Boolean = false,
    @StringRes val deleteErrorMsg: Int? = null,
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
 */
class EventDetailsViewModel(
    private val dataRepository: DataRepository,
    private val eventId: String,
) : ViewModel() {

  private val eventsRepository: EventsRepository = dataRepository.eventsRepository

  private val _uiState = MutableStateFlow(EventDetailsUIState())
  val uiState: StateFlow<EventDetailsUIState> = _uiState.asStateFlow()

  /** Loads [eventId] and updates the UI state with either the event or a loading error. */
  fun fetchEvent() {
    viewModelScope.launch {
      _uiState.update { it.copy(event = null, errorMsg = null) }
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update { it.copy(event = event) }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        _uiState.update { it.copy(errorMsg = R.string.event_details_fetch_error) }
      }
    }
  }

  /**
   * Asks to delete the event. Nothing is deleted until [confirmDelete] is called. Ignored until the
   * event is loaded and while a deletion is running.
   */
  fun requestDelete() {
    _uiState.update {
      if (it.event != null && !it.isDeleting) {
        it.copy(deleteRequested = true, deleteErrorMsg = null)
      } else {
        it
      }
    }
  }

  /** Closes the confirmation: the event is kept and nothing changes. */
  fun cancelDelete() {
    _uiState.update { it.copy(deleteRequested = false) }
  }

  /** Hides the latest deletion failure. */
  fun clearDeleteError() {
    _uiState.update { it.copy(deleteErrorMsg = null) }
  }

  /**
   * Deletes the event and calls [onDeleted] once it is gone. Does nothing unless the deletion was
   * requested with [requestDelete] and is not already running, so the event is never deleted
   * without a confirmation. On failure the event stays and [EventDetailsUIState.deleteErrorMsg] is
   * set. The deletion writes in several places, so it finishes even if the screen is left
   * meanwhile; [onDeleted] is then not called.
   */
  fun confirmDelete(onDeleted: () -> Unit) {
    val before = _uiState.getAndUpdate {
      if (it.deleteRequested && !it.isDeleting) {
        it.copy(deleteRequested = false, isDeleting = true, deleteErrorMsg = null)
      } else {
        it
      }
    }
    if (!before.deleteRequested || before.isDeleting) return

    viewModelScope.launch {
      try {
        withContext(NonCancellable) { dataRepository.deleteEvent(eventId) }
        ensureActive()
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isDeleting = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error deleting event $eventId", e)
        _uiState.update {
          it.copy(isDeleting = false, deleteErrorMsg = R.string.event_details_delete_error)
        }
        return@launch
      }
      _uiState.update { it.copy(isDeleting = false) }
      onDeleted()
    }
  }

  private companion object {
    const val LOG_TAG = "EventDetailsViewModel"
  }
}
