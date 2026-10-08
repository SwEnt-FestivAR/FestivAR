package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateEventUIState(
    val name: String = "",
    val startDate: ZonedDateTime? = null,
    val endDate: ZonedDateTime? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val venue: String = "",
    val notes: String = "",
    val activeDatePicker: DateField? = null,
    val errorName: Boolean = false,
    val errorDate: Boolean = false,
    val errorVenue: Boolean = false,
)

enum class DateField {
  START,
  END,
}

class CreateEventViewModel(
    private val eventsRepository: EventsRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(CreateEventUIState())
  val uiState: StateFlow<CreateEventUIState> = _uiState.asStateFlow()

  fun updateName(newName: String) {
    _uiState.update { it.copy(name = newName) }
  }

  fun updateStartDate(newDate: ZonedDateTime) {
    _uiState.update { it.copy(startDate = newDate) }
  }

  fun updateEndDate(newDate: ZonedDateTime) {
    _uiState.update { it.copy(endDate = newDate) }
  }

  fun updateStartDateMillis(newDateMillis: Long) {
    _uiState.update { it.copy(startDateMillis = newDateMillis) }
  }

  fun updateEndDateMillis(newDateMillis: Long) {
    _uiState.update { it.copy(endDateMillis = newDateMillis) }
  }

  fun updateVenue(newVenue: String) {
    _uiState.update { it.copy(venue = newVenue) }
  }

  fun updateNotes(newNotes: String) {
    _uiState.update { it.copy(notes = newNotes) }
  }

  fun updateActiveDatePicker(newActiveDatePicker: DateField?) {
    _uiState.update { it.copy(activeDatePicker = newActiveDatePicker) }
  }

  fun removeErrorName() {
    _uiState.update { it.copy(errorName = false) }
  }

  fun removeErrorDate() {
    _uiState.update { it.copy(errorDate = false) }
  }

  fun removeErrorVenue() {
    _uiState.update { it.copy(errorVenue = false) }
  }

  fun setErrors() {
    _uiState.update {
      it.copy(
          errorName = _uiState.value.name.isBlank(),
          errorDate = !endDateComesAfterStartDate(),
          errorVenue = _uiState.value.venue.isBlank(),
      )
    }
  }

  fun endDateComesAfterStartDate(): Boolean {
    val state = _uiState.value
    return state.startDate != null &&
        state.endDate != null &&
        state.endDate.isAfter(state.startDate)
  }

  fun formIsComplete(): Boolean {
    return _uiState.value.name.isNotBlank() &&
        _uiState.value.startDate != null &&
        _uiState.value.endDate != null &&
        _uiState.value.venue.isNotBlank()
  }

  fun validEvent(): Boolean {
    return formIsComplete() && endDateComesAfterStartDate()
  }

  fun createEvent() {
    viewModelScope.launch {
      eventsRepository.addEvent(
          Event(
              eventId = eventsRepository.getNewUid(),
              title = _uiState.value.name,
              description = _uiState.value.notes,
              startDate = _uiState.value.startDate!!,
              endDate = _uiState.value.endDate!!,
              location = _uiState.value.venue,
          )
      )
    }
  }
}
