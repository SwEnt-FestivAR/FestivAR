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
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.components.endDateError
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
    val event: Event? = null,
    val name: String = "",
    val nameEdited: Boolean = false,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val venue: String = "",
    val notes: String = "",
    val openPicker: DateTimeField? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedEventId: String? = null,
    val showDiscardConfirmation: Boolean = false,
    val isDiscarded: Boolean = false,
    val errorFetch: Boolean = false,
    val zone: ZoneId = ZoneId.systemDefault(),
    @param:StringRes val errorMsg: Int? = null,
) {
  val startDateTime: ZonedDateTime?
    get() = startDate?.atTime(startTime ?: START_OF_DAY)?.atZone(zone)

  val endDateTime: ZonedDateTime?
    get() = endDate?.atTime(endTime ?: END_OF_DAY)?.atZone(zone)

  val nameError: FieldError?
    get() = if (nameEdited && name.isBlank()) FieldError.EMPTY_EVENT_NAME else null

  val startDateError: FieldError?
    get() = startDateError(startDate, startTime)

  val endDateError: FieldError?
    get() = endDateError(DateTimeRange(startDate, startTime, endDate, endTime), zone)

  val isValid: Boolean
    get() =
        event != null &&
            name.isNotBlank() &&
            startDateTime != null &&
            endDateTime != null &&
            nameError == null &&
            startDateError == null &&
            endDateError == null

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
          errorMsg = null,
      )
}

class EditEventViewModel(private val eventsRepository: EventsRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(EditEventUIState())
  val uiState: StateFlow<EditEventUIState> = _uiState.asStateFlow()

  private fun edit(change: (EditEventUIState) -> EditEventUIState) {
    _uiState.update { change(it).copy(errorMsg = null) }
  }

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

  fun updateName(newName: String) = edit { it.copy(name = newName, nameEdited = true) }

  fun updateStartDate(newDate: LocalDate) = edit { it.copy(startDate = newDate) }

  fun updateEndDate(newDate: LocalDate) = edit { it.copy(endDate = newDate) }

  fun updateStartTime(time: LocalTime) = edit { it.copy(startTime = time) }

  fun updateEndTime(time: LocalTime) = edit { it.copy(endTime = time) }

  fun updateVenue(newVenue: String) = edit { it.copy(venue = newVenue) }

  fun updateNotes(newNotes: String) = edit { it.copy(notes = newNotes) }

  fun showPicker(newOpenPicker: DateTimeField?) = edit { it.copy(openPicker = newOpenPicker) }

  fun pickDate(date: LocalDate) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_DATE -> updateStartDate(date)
      DateTimeField.END_DATE -> updateEndDate(date)
      else -> Unit
    }
    showPicker(null)
  }

  fun pickTime(time: LocalTime) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_TIME -> updateStartTime(time)
      DateTimeField.END_TIME -> updateEndTime(time)
      else -> Unit
    }
    showPicker(null)
  }

  fun requestDiscard() {
    _uiState.update {
      if (it.hasChanges) it.copy(showDiscardConfirmation = true) else it.copy(isDiscarded = true)
    }
  }

  fun cancelDiscard() {
    _uiState.update { it.copy(showDiscardConfirmation = false) }
  }

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
