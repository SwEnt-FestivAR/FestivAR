package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventDetailsViewModel(
    private val eventsRepository: EventsRepository,
) : ViewModel() {

  private val _event = MutableStateFlow<Event?>(null)
  val event: StateFlow<Event?> = _event.asStateFlow()

  fun fetchEvent(eventId: String) {
    viewModelScope.launch {
      try {
        _event.value = eventsRepository.getEvent(eventId)
      } catch (_: Exception) {
        // Event loading errors are intentionally ignored for now.
      }
    }
  }
}
