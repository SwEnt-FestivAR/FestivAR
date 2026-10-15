package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailsUIState(
    val event: Event? = null,
    val errorFetch: Boolean = false,
)

class EventDetailsViewModel(
    private val eventsRepository: EventsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(EventDetailsUIState())
  val uiState: StateFlow<EventDetailsUIState> = _uiState.asStateFlow()

  fun fetchEvent(eventId: String) {
    viewModelScope.launch {
      _uiState.update { EventDetailsUIState() }
      try {
        val event = eventsRepository.getEvent(eventId)
        _uiState.update { it.copy(event = event) }
      } catch (_: Exception) {
        _uiState.update { it.copy(errorFetch = true) }
      }
    }
  }
}
