// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.temporary.User
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which [Event] items the overview shows, selected with the chips at the top of the screen. */
enum class EventsFilter {
  ONGOING,
  UPCOMING,
  PAST,
}

/**
 * Represents an [Event] card of the Events Overview screen.
 *
 * @property event The [Event] shown by the card.
 */
data class EventItemUIState(
    val event: Event,
)

/**
 * Represents the UI state of the Events Overview screen.
 *
 * @property ongoingEvents The [Event] items taking place right now, the one that started first
 *   first.
 * @property upcomingEvents The [Event] items that have not started yet, the soonest first.
 * @property pastEvents The [Event] items that are closed or ended, the most recent first.
 * @property selectedFilter Which [Event] items are shown: [ongoingEvents], [upcomingEvents] or
 *   [pastEvents].
 * @property user The signed-in [User], whose avatar is shown in the top bar. `null` if unknown.
 * @property isLoading Whether the [Event] items are currently being fetched.
 * @property errorMsg An error message to show when fetching the [Event] items fails. `null` if
 *   there is no error.
 */
data class EventsOverviewUIState(
    val ongoingEvents: List<EventItemUIState> = emptyList(),
    val upcomingEvents: List<EventItemUIState> = emptyList(),
    val pastEvents: List<EventItemUIState> = emptyList(),
    val selectedFilter: EventsFilter = EventsFilter.ONGOING,
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
) {
  /** The [Event] items selected by [selectedFilter]. */
  val shownEvents: List<EventItemUIState>
    get() =
        when (selectedFilter) {
          EventsFilter.ONGOING -> ongoingEvents
          EventsFilter.UPCOMING -> upcomingEvents
          EventsFilter.PAST -> pastEvents
        }

  /** Whether there is no [Event] at all, neither ongoing, upcoming nor past. */
  val hasNoEvents: Boolean
    get() = ongoingEvents.isEmpty() && upcomingEvents.isEmpty() && pastEvents.isEmpty()
}

/**
 * ViewModel of the Events Overview screen.
 *
 * Manages the UI state by fetching the [Event] items through the [EventsRepository].
 *
 * @property eventsRepository The repository used to fetch the [Event] items.
 * @property user The signed-in [User], whose [Event] items are shown. If `null`, every [Event] of
 *   the repository is shown.
 * @property now Gives the current time, used to split ongoing, upcoming and past [Event] items.
 */
class EventsOverviewViewModel(
    private val eventsRepository: EventsRepository,
    private val user: User? = null,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : ViewModel() {

  private val _uiState = MutableStateFlow(EventsOverviewUIState(user = user))
  val uiState: StateFlow<EventsOverviewUIState> = _uiState.asStateFlow()

  init {
    getEvents()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /** Selects which [Event] items are shown. */
  fun selectFilter(filter: EventsFilter) {
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  /** Refreshes the UI state by fetching the [Event] items from the repository again. */
  fun refreshUIState() {
    getEvents()
  }

  /**
   * Fetches the [Event] items from the repository, the ones of [user] if it is set, and updates the
   * UI state.
   */
  private fun getEvents() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      try {
        val events =
            if (user == null) eventsRepository.getAllEvents()
            else eventsRepository.getEventsForUser(user.uid)
        val currentTime = now()
        val (past, notPast) = events.partition { it.isPast(currentTime) }
        val (upcoming, ongoing) = notPast.partition { it.startDate.isAfter(currentTime) }
        _uiState.update {
          it.copy(
              ongoingEvents = ongoing.sortedBy { event -> event.startDate }.map(::EventItemUIState),
              upcomingEvents =
                  upcoming.sortedBy { event -> event.startDate }.map(::EventItemUIState),
              pastEvents =
                  past.sortedByDescending { event -> event.startDate }.map(::EventItemUIState),
              isLoading = false,
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isLoading = false, errorMsg = "Failed to load events: ${e.message}")
        }
      }
    }
  }

  /** An [Event] is past once it is closed or once its [Event.endDate] is not after [time]. */
  private fun Event.isPast(time: ZonedDateTime): Boolean = closed || !endDate.isAfter(time)
}
