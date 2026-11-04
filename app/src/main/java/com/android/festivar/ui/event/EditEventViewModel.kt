package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.FieldError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val SAVE_TIMEOUT_MS = 15_000L

data class EditEventUIState(
    // The event currently being edited, or null before a record has been loaded.
    val event: Event? = null,
    // The user-entered event title shown in the form.
    val name: String = "",
    // Tracks whether the title field has been touched so empty-name validation can trigger
    // correctly.
    val nameEdited: Boolean = false,
    // Selected start date for the event, used to build the full start timestamp.
    val startDate: LocalDate? = null,
    // Selected end date for the event, used to build the full end timestamp.
    val endDate: LocalDate? = null,
    // Selected start time for the event, used together with the start date.
    val startTime: LocalTime? = null,
    // Selected end time for the event, used together with the end date.
    val endTime: LocalTime? = null,
    // Venue text entered by the user for the event location.
    val venue: String = "",
    // Notes or description entered by the user for the event.
    val notes: String = "",
    // Which date/time picker is currently open, or null when no picker is active.
    val openPicker: DateTimeField? = null,
    // True while the existing event is being fetched from the repository.
    val isLoading: Boolean = false,
    // True while the updated event is being saved.
    val isSaving: Boolean = false,
    // Event id returned after a successful save, used to prevent duplicate saves.
    val savedEventId: String? = null,
    // True when the discard confirmation dialog should be shown.
    val showDiscardConfirmation: Boolean = false,
    // True when the user has confirmed the form should be discarded.
    val isDiscarded: Boolean = false,
    // True when loading the event failed and the UI should show a fetch error.
    val errorFetch: Boolean = false,
    // Time zone used for the start date/time when building the event timestamp.
    val zone: ZoneId = ZoneId.systemDefault(),
    // Time zone used for the end date/time when building the event timestamp.
    val endZone: ZoneId = ZoneId.systemDefault(),
    // Resource id for the latest form error message displayed to the user.
    @param:StringRes val errorMsg: Int? = null,
) {
  // Full start timestamp assembled from the selected start date, time, and zone.
  val startDateTime: ZonedDateTime?
    get() = computeStartDateTime(startDate, startTime, zone)

  // Full end timestamp assembled from the selected end date, time, and zone.
  val endDateTime: ZonedDateTime?
    get() = computeEndDateTime(endDate, endTime, endZone)

  // Validation error for the title field when it has been edited and left blank.
  val nameError: FieldError?
    get() = validateEventName(name, nameEdited)

  // Validation error for the start date/time combination before saving.
  val startDateError: FieldError?
    get() = validateStartDate(startDate, startTime)

  // Validation error for the end date/time combination before saving.
  val endDateError: FieldError?
    get() {
      if (endTime != null && endDate == null) return FieldError.MISSING_DATE
      val end = endDateTime ?: return null
      return when {
        startDateTime != null && !end.isAfter(startDateTime) -> FieldError.END_BEFORE_START
        end.isBefore(ZonedDateTime.now()) -> FieldError.OUTDATED
        else -> null
      }
    }

  // True when the loaded event data is complete and satisfies the form validation rules.
  val isValid: Boolean
    get() =
        isEventFormValid(
            event != null,
            name,
            startDateTime,
            endDateTime,
            nameError,
            startDateError,
            endDateError,
        )

  // True when the current draft differs from the original event and therefore needs saving.
  val hasChanges: Boolean
    get() =
        event != null &&
            (name != event.title ||
                startDate != event.startDate.toLocalDate() ||
                startTime != event.startDate.toLocalTime() ||
                endDate != event.endDate.toLocalDate() ||
                endTime != event.endDate.toLocalTime() ||
                venue != event.location ||
                notes != event.description)

  // True when the form is valid, changed, not busy, and not already saved.
  val canSave: Boolean
    get() = isValid && hasChanges && !isLoading && !isSaving && savedEventId == null

  fun toEvent(): Event {
    val original = checkNotNull(event) { "No event is loaded." }
    require(isValid) { "The form is not valid." }
    return original.copy(
        title = name,
        description = notes,
        startDate = startDateTime!!,
        endDate = endDateTime!!,
        location = venue,
    )
  }

  internal fun load(event: Event): EditEventUIState =
      copy(
          event = event,
          name = event.title,
          nameEdited = false,
          startDate = event.startDate.toLocalDate(),
          endDate = event.endDate.toLocalDate(),
          startTime = event.startDate.toLocalTime(),
          endTime = event.endDate.toLocalTime(),
          venue = event.location,
          notes = event.description,
          openPicker = null,
          isLoading = false,
          isSaving = false,
          savedEventId = null,
          showDiscardConfirmation = false,
          isDiscarded = false,
          errorFetch = false,
          zone = event.startDate.zone,
          endZone = event.endDate.zone,
          errorMsg = null,
      )
}

class EditEventViewModel(
    private val eventsRepository: EventsRepository,
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
) : EventFormViewModel<EditEventUIState>(EditEventUIState()) {
  val uiState: StateFlow<EditEventUIState> = formState
  private var loadJob: Job? = null

  init {
    require(saveTimeoutMs > 0) { "saveTimeoutMs must be positive" }
  }

  override fun clearError(state: EditEventUIState) = state.copy(errorMsg = null)

  override fun updateNameState(state: EditEventUIState, newName: String) =
      state.copy(name = newName, nameEdited = true)

  override fun updateStartDateState(state: EditEventUIState, newDate: LocalDate) =
      state.copy(startDate = newDate)

  override fun updateEndDateState(state: EditEventUIState, newDate: LocalDate) =
      state.copy(endDate = newDate)

  override fun updateStartTimeState(state: EditEventUIState, time: LocalTime) =
      state.copy(startTime = time)

  override fun updateEndTimeState(state: EditEventUIState, time: LocalTime) =
      state.copy(endTime = time)

  override fun updateVenueState(state: EditEventUIState, newVenue: String) =
      state.copy(venue = newVenue)

  override fun updateNotesState(state: EditEventUIState, newNotes: String) =
      state.copy(notes = newNotes)

  override fun updatePickerState(state: EditEventUIState, picker: DateTimeField?) =
      state.copy(openPicker = picker)

  override fun currentPicker(state: EditEventUIState) = state.openPicker

  // Loads an event by id and populates the form so the user can edit the existing event details.
  fun fetchEvent(eventId: String) {
    if (eventId.isBlank()) {
      Log.e("EditEventViewModel", "Cannot load an event with a blank id")
      updateState { if (it.event == null) it.copy(isLoading = false, errorFetch = true) else it }
      return
    }
    if (currentState.event != null || currentState.isSaving || loadJob?.isActive == true) return

    updateState { it.copy(isLoading = true, errorFetch = false) }
    loadJob = viewModelScope.launch {
      try {
        val event = eventsRepository.getEvent(eventId)
        updateState { it.load(event) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("EditEventViewModel", "Loading event $eventId failed", e)
        updateState { it.copy(isLoading = false, errorFetch = true) }
      }
    }
  }

  // Starts the discard flow when the user tries to leave the screen with unsaved edits.
  fun requestDiscard() {
    updateState {
      if (it.hasChanges) it.copy(showDiscardConfirmation = true) else it.copy(isDiscarded = true)
    }
  }

  // Closes the discard confirmation dialog without discarding the current draft.
  fun cancelDiscard() {
    updateState { it.copy(showDiscardConfirmation = false) }
  }

  // Confirms discarding the draft and either resets to the original event or clears the form.
  fun confirmDiscard() {
    updateState {
      val original = it.event
      if (original == null) {
        it.copy(showDiscardConfirmation = false, isDiscarded = true)
      } else {
        it.load(original).copy(isDiscarded = true)
      }
    }
  }

  // Saves the edited event to the repository after validating that the form is ready to persist.
  fun saveChanges() {
    val state = currentState
    if (!state.canSave) return
    val updatedEvent = state.toEvent()
    updateState { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        val latestEvent =
            withTimeout(saveTimeoutMs) { eventsRepository.getEvent(updatedEvent.eventId) }
        val eventToSave =
            latestEvent.copy(
                title = updatedEvent.title,
                description = updatedEvent.description,
                startDate = updatedEvent.startDate,
                endDate = updatedEvent.endDate,
                location = updatedEvent.location,
            )
        withTimeout(saveTimeoutMs) { eventsRepository.editEvent(eventToSave.eventId, eventToSave) }
        updateState { it.load(eventToSave).copy(savedEventId = eventToSave.eventId) }
      } catch (e: TimeoutCancellationException) {
        onSaveFailed(updatedEvent, e)
      } catch (e: CancellationException) {
        updateState { it.copy(isSaving = false) }
        throw e
      } catch (e: Exception) {
        onSaveFailed(updatedEvent, e)
      }
    }
  }

  private suspend fun onSaveFailed(updatedEvent: Event, error: Exception) {
    Log.w("EditEventViewModel", "Saving event failed; checking whether it was stored", error)
    val storedEvent =
        try {
          withTimeout(saveTimeoutMs) { eventsRepository.getEvent(updatedEvent.eventId) }
        } catch (e: TimeoutCancellationException) {
          Log.e("EditEventViewModel", "Could not verify event save before timeout", e)
          null
        } catch (e: CancellationException) {
          updateState { it.copy(isSaving = false) }
          throw e
        } catch (e: Exception) {
          Log.e("EditEventViewModel", "Could not verify event save", e)
          null
        }

    if (storedEvent != null && storedEvent.hasSameFormValuesAs(updatedEvent)) {
      updateState { it.load(storedEvent).copy(savedEventId = storedEvent.eventId) }
    } else {
      updateState { it.copy(isSaving = false, errorMsg = R.string.edit_event_error_save_failed) }
    }
  }

  private fun Event.hasSameFormValuesAs(other: Event): Boolean =
      title == other.title &&
          description == other.description &&
          startDate == other.startDate &&
          endDate == other.endDate &&
          location == other.location
}
