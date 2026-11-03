package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.components.startDateError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    get() = startDate?.atTime(startTime ?: START_OF_DAY)?.atZone(zone)

  // Full end timestamp assembled from the selected end date, time, and zone.
  val endDateTime: ZonedDateTime?
    get() = endDate?.atTime(endTime ?: END_OF_DAY)?.atZone(endZone)

  // Validation error for the title field when it has been edited and left blank.
  val nameError: FieldError?
    get() = if (nameEdited && name.isBlank()) FieldError.EMPTY_EVENT_NAME else null

  // Validation error for the start date/time combination before saving.
  val startDateError: FieldError?
    get() = startDateError(startDate, startTime)

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
        event != null &&
            name.isNotBlank() &&
            startDateTime != null &&
            endDateTime != null &&
            nameError == null &&
            startDateError == null &&
            endDateError == null

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

class EditEventViewModel(private val eventsRepository: EventsRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(EditEventUIState())
  val uiState: StateFlow<EditEventUIState> = _uiState.asStateFlow()

  // Applies a state update while clearing any prior form error so the UI reflects the latest edit.
  private fun edit(change: (EditEventUIState) -> EditEventUIState) {
    _uiState.update { change(it).copy(errorMsg = null) }
  }

  // Loads an event by id and populates the form so the user can edit the existing event details.
  fun fetchEvent(eventId: String) {
    _uiState.update { EditEventUIState(isLoading = true) }
    viewModelScope.launch {
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update { it.load(event) }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        _uiState.update { it.copy(isLoading = false, errorFetch = true) }
      }
    }
  }

  // Updates the event title as the user types it and marks the field as edited for validation.
  fun updateName(newName: String) = edit { it.copy(name = newName, nameEdited = true) }

  // Stores the selected start date from the date picker in the current draft event.
  fun updateStartDate(newDate: LocalDate) = edit { it.copy(startDate = newDate) }

  // Stores the selected end date from the date picker in the current draft event.
  fun updateEndDate(newDate: LocalDate) = edit { it.copy(endDate = newDate) }

  // Stores the chosen start time from the time picker in the current draft event.
  fun updateStartTime(time: LocalTime) = edit { it.copy(startTime = time) }

  // Stores the chosen end time from the time picker in the current draft event.
  fun updateEndTime(time: LocalTime) = edit { it.copy(endTime = time) }

  // Updates the venue field as the user edits the event location.
  fun updateVenue(newVenue: String) = edit { it.copy(venue = newVenue) }

  // Updates the notes field so the description remains in sync with the user input.
  fun updateNotes(newNotes: String) = edit { it.copy(notes = newNotes) }

  // Opens or closes a date/time picker for the selected field in the form.
  fun showPicker(newOpenPicker: DateTimeField?) = edit { it.copy(openPicker = newOpenPicker) }

  // Applies the selected date to whichever field is currently being edited.
  fun pickDate(date: LocalDate) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_DATE -> updateStartDate(date)
      DateTimeField.END_DATE -> updateEndDate(date)
      else -> Unit
    }
    showPicker(null)
  }

  // Applies the selected time to whichever field is currently being edited.
  fun pickTime(time: LocalTime) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_TIME -> updateStartTime(time)
      DateTimeField.END_TIME -> updateEndTime(time)
      else -> Unit
    }
    showPicker(null)
  }

  // Starts the discard flow when the user tries to leave the screen with unsaved edits.
  fun requestDiscard() {
    _uiState.update {
      if (it.hasChanges) it.copy(showDiscardConfirmation = true) else it.copy(isDiscarded = true)
    }
  }

  // Closes the discard confirmation dialog without discarding the current draft.
  fun cancelDiscard() {
    _uiState.update { it.copy(showDiscardConfirmation = false) }
  }

  // Confirms discarding the draft and either resets to the original event or clears the form.
  fun confirmDiscard() {
    _uiState.update {
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
    val state = _uiState.value
    if (!state.canSave) return
    val updatedEvent = state.toEvent()
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        eventsRepository.editEvent(updatedEvent.eventId, updatedEvent)
        _uiState.update { it.load(updatedEvent).copy(savedEventId = updatedEvent.eventId) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("EditEventViewModel", "Saving the event failed", e)
        _uiState.update {
          it.copy(isSaving = false, errorMsg = R.string.edit_event_error_save_failed)
        }
      }
    }
  }
}
