// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
 * Represents the UI state of the Events Overview screen.
 *
 * @property ongoingEvents The [Event] items taking place right now, the earliest started first.
 * @property upcomingEvents The [Event] items that have not started yet, the soonest first.
 * @property pastEvents The [Event] items that are closed or ended, the most recently ended first.
 * @property selectedFilter Which [Event] items are shown: [ongoingEvents], [upcomingEvents] or
 *   [pastEvents].
 * @property user The signed-in [User], whose avatar is shown in the top bar. `null` only until a
 *   ViewModel provides it, and only the identifier until the saved [User] is fetched.
 * @property isLoading Whether the [Event] items are currently being fetched.
 * @property hasError Whether the last fetch of the [Event] items failed. It stays `true` until a
 *   fetch succeeds, so the screen can keep showing the error with a retry.
 * @property errorMsg The cause of the failed fetch, as given by the repository. `null` if there is
 *   no error, or if the repository gave no cause. The screen words the error itself.
 */
data class EventsOverviewUIState(
    val ongoingEvents: List<Event> = emptyList(),
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val selectedFilter: EventsFilter = EventsFilter.ONGOING,
    val user: User? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val errorMsg: String? = null,
) {
  /** The [Event] items selected by [selectedFilter]. */
  val shownEvents: List<Event>
    get() =
        when (selectedFilter) {
          EventsFilter.ONGOING -> ongoingEvents
          EventsFilter.UPCOMING -> upcomingEvents
          EventsFilter.PAST -> pastEvents
        }

  /**
   * Whether there is no [Event] at all, neither ongoing, upcoming nor past. Also `true` during the
   * first load, so the screen checks [isLoading] first to not flash its empty state.
   */
  val hasNoEvents: Boolean
    get() = ongoingEvents.isEmpty() && upcomingEvents.isEmpty() && pastEvents.isEmpty()
}

/**
 * ViewModel of the Events Overview screen.
 *
 * Manages the UI state by fetching the signed-in [User] through the [UsersRepository] and its
 * [Event] items through the [EventsRepository]. Nothing is fetched until [refreshUIState] is
 * called: the screen calls it each time it is shown. Until the first fetch, the UI state is already
 * loading.
 *
 * @property eventsRepository The repository used to fetch the [Event] items.
 * @property usersRepository The repository holding the [User] saved by the sign in or the sign up,
 *   with the name and surname shown in the avatar.
 * @property uId The signed-in [User] unique identifier.
 * @property now Gives the current time, used to split ongoing, upcoming and past [Event] items.
 */
class EventsOverviewViewModel(
    private val eventsRepository: EventsRepository,
    private val usersRepository: UsersRepository,
    private val uId: String,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : ViewModel() {

  // Until the saved User is fetched, the avatar shows a person icon. Loading from the start:
  // nothing is fetched until the screen calls refreshUIState on resume, and until then the screen
  // must show its progress indicator, not its empty state.
  private val _uiState =
      MutableStateFlow(EventsOverviewUIState(user = User(uid = uId), isLoading = true))
  val uiState: StateFlow<EventsOverviewUIState> = _uiState.asStateFlow()

  /** The fetch of the [User] in progress, cancelled when a newer one starts. */
  private var getUserJob: Job? = null

  /** The fetch of the [Event] items in progress, cancelled when a newer one starts. */
  private var getEventsJob: Job? = null

  /**
   * Whether the filter is already chosen, by the user or by the first successful fetch of the
   * [Event] items. Until then, a successful fetch picks the default filter; afterwards a refresh
   * never overrides it.
   */
  private var isFilterChosen = false

  /** Selects which [Event] items are shown. */
  fun selectFilter(filter: EventsFilter) {
    isFilterChosen = true
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  /**
   * Refreshes the UI state by fetching the [User] and the [Event] items from the repositories, e.g.
   * when the screen is shown again after the profile was edited, or when the user retries.
   */
  fun refreshUIState() {
    getUser()
    getEvents()
  }

  /**
   * Fetches the signed-in [User] from the repository and shows it in the avatar. Cancels the
   * previous fetch, so that an older result never overwrites a newer one. If it fails, the avatar
   * keeps what it shows.
   */
  private fun getUser() {
    getUserJob?.cancel()
    getUserJob = viewModelScope.launch {
      try {
        val user = usersRepository.getUser(uId)
        ensureActive()
        _uiState.update { it.copy(user = user) }
      } catch (e: CancellationException) {
        // Cancelled by a newer fetch or because the ViewModel is cleared: not an error
        throw e
      } catch (e: Exception) {
        // The events can still be shown: the avatar only misses the name and surname
      }
    }
  }

  /**
   * Fetches the [Event] items of the signed-in [User] from the repository and updates the UI state.
   * Cancels the previous fetch, so that an older result never overwrites a newer one. A failure
   * keeps the [Event] items already shown, and the error stays until a fetch succeeds.
   */
  private fun getEvents() {
    getEventsJob?.cancel()
    _uiState.update { it.copy(isLoading = true) }
    getEventsJob = viewModelScope.launch {
      try {
        val events = eventsRepository.getEventsForUser(uId)
        // A newer fetch may have cancelled this one while the repository was fetching. A
        // repository that ignores cancellation still returns its events, so stop here before
        // these stale events reach the UI state.
        ensureActive()
        val currentTime = now()
        val (past, notPast) = events.partition { it.isPast(currentTime) }
        val (upcoming, ongoing) = notPast.partition { it.startDate.isAfter(currentTime) }
        val choosesFilter = !isFilterChosen
        isFilterChosen = true
        _uiState.update {
          it.copy(
              ongoingEvents = ongoing.sortedBy { event -> event.startDate },
              upcomingEvents = upcoming.sortedBy { event -> event.startDate },
              pastEvents = past.sortedByDescending { event -> event.endDate },
              selectedFilter =
                  if (choosesFilter) defaultFilter(ongoing, upcoming, past) else it.selectedFilter,
              isLoading = false,
              hasError = false,
              errorMsg = null,
          )
        }
      } catch (e: CancellationException) {
        // Cancelled by a newer fetch or because the ViewModel is cleared: not an error
        throw e
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, hasError = true, errorMsg = e.message) }
      }
    }
  }

  /**
   * The filter shown after the first load: the first one with [Event] items among ongoing, upcoming
   * and past, or [EventsFilter.ONGOING] if there is none.
   */
  private fun defaultFilter(
      ongoing: List<Event>,
      upcoming: List<Event>,
      past: List<Event>,
  ): EventsFilter =
      when {
        ongoing.isNotEmpty() -> EventsFilter.ONGOING
        upcoming.isNotEmpty() -> EventsFilter.UPCOMING
        past.isNotEmpty() -> EventsFilter.PAST
        else -> EventsFilter.ONGOING
      }
}
