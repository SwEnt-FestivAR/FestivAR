package com.android.festivar.ui.event.create

// Co-authored-by: Copilot App

import androidx.lifecycle.ViewModel
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.components.startDateError
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class EventFormViewModel<S>(initialState: S) : ViewModel() {
  private val _uiState = MutableStateFlow(initialState)
  protected val formState: StateFlow<S> = _uiState.asStateFlow()
  protected val currentState: S
    get() = _uiState.value

  protected abstract fun clearError(state: S): S

  protected abstract fun updateNameState(state: S, newName: String): S

  protected abstract fun updateStartDateState(state: S, newDate: LocalDate): S

  protected abstract fun updateEndDateState(state: S, newDate: LocalDate): S

  protected abstract fun updateStartTimeState(state: S, time: LocalTime): S

  protected abstract fun updateEndTimeState(state: S, time: LocalTime): S

  protected abstract fun updateVenueState(state: S, newVenue: String): S

  protected abstract fun updateNotesState(state: S, newNotes: String): S

  protected abstract fun updatePickerState(state: S, picker: DateTimeField?): S

  protected abstract fun currentPicker(state: S): DateTimeField?

  protected fun updateState(change: (S) -> S) {
    _uiState.update(change)
  }

  private fun edit(change: (S) -> S) {
    _uiState.update { clearError(change(it)) }
  }

  fun updateName(newName: String) = edit { updateNameState(it, newName) }

  fun updateStartDate(newDate: LocalDate) = edit { updateStartDateState(it, newDate) }

  fun updateEndDate(newDate: LocalDate) = edit { updateEndDateState(it, newDate) }

  fun updateStartTime(time: LocalTime) = edit { updateStartTimeState(it, time) }

  fun updateEndTime(time: LocalTime) = edit { updateEndTimeState(it, time) }

  fun updateVenue(newVenue: String) = edit { updateVenueState(it, newVenue) }

  fun updateNotes(newNotes: String) = edit { updateNotesState(it, newNotes) }

  fun showPicker(newOpenPicker: DateTimeField?) = edit { updatePickerState(it, newOpenPicker) }

  /**
   * Sets [date] in the selected date field, if any, then closes the picker.
   *
   * @param date The selected date.
   */
  fun pickDate(date: LocalDate) {
    when (currentPicker(currentState)) {
      DateTimeField.START_DATE -> updateStartDate(date)
      DateTimeField.END_DATE -> updateEndDate(date)
      else -> Unit
    }
    showPicker(null)
  }

  /**
   * Sets [time] in the selected time field, if any, then closes the picker.
   *
   * @param time The selected time.
   */
  fun pickTime(time: LocalTime) {
    when (currentPicker(currentState)) {
      DateTimeField.START_TIME -> updateStartTime(time)
      DateTimeField.END_TIME -> updateEndTime(time)
      else -> Unit
    }
    showPicker(null)
  }
}

internal fun computeStartDateTime(
    date: LocalDate?,
    time: LocalTime?,
    zone: ZoneId,
): ZonedDateTime? = date?.atTime(time ?: START_OF_DAY)?.atZone(zone)

internal fun computeEndDateTime(
    date: LocalDate?,
    time: LocalTime?,
    zone: ZoneId,
): ZonedDateTime? = date?.atTime(time ?: END_OF_DAY)?.atZone(zone)

internal fun validateEventName(name: String, edited: Boolean): FieldError? =
    if (edited && name.isBlank()) FieldError.EMPTY_EVENT_NAME else null

internal fun validateStartDate(date: LocalDate?, time: LocalTime?): FieldError? =
    startDateError(date, time)

internal fun isEventFormValid(
    isLoaded: Boolean,
    name: String,
    startDateTime: ZonedDateTime?,
    endDateTime: ZonedDateTime?,
    nameError: FieldError?,
    startDateError: FieldError?,
    endDateError: FieldError?,
): Boolean =
    isLoaded &&
        name.isNotBlank() &&
        startDateTime != null &&
        endDateTime != null &&
        nameError == null &&
        startDateError == null &&
        endDateError == null
