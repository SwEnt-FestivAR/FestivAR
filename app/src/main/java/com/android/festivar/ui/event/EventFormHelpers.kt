package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import androidx.lifecycle.ViewModel
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.components.datetime.END_OF_DAY
import com.android.festivar.ui.components.datetime.START_OF_DAY
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

  protected abstract fun updateRangeState(state: S, range: DateTimeRange): S

  protected abstract fun updateVenueState(state: S, newVenue: String): S

  protected abstract fun updateNotesState(state: S, newNotes: String): S

  protected fun updateState(change: (S) -> S) {
    _uiState.update(change)
  }

  private fun edit(change: (S) -> S) {
    _uiState.update { clearError(change(it)) }
  }

  fun updateName(newName: String) = edit { updateNameState(it, newName) }

  fun updateRange(range: DateTimeRange) = edit { updateRangeState(it, range) }

  fun updateVenue(newVenue: String) = edit { updateVenueState(it, newVenue) }

  fun updateNotes(newNotes: String) = edit { updateNotesState(it, newNotes) }
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

internal fun isEventFormValid(
    isLoaded: Boolean,
    name: String,
    startDateTime: ZonedDateTime?,
    endDateTime: ZonedDateTime?,
    nameError: FieldError?,
    endDateError: FieldError?,
): Boolean =
    isLoaded &&
        name.isNotBlank() &&
        startDateTime != null &&
        endDateTime != null &&
        nameError == null &&
        endDateError == null
