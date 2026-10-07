// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.task.Task
import com.android.festivar.model.temporary.User
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsOverviewViewModelTest {

  /**
   * A fake [EventsRepository] serving [eventList]. It throws [error] on every fetch when it is set,
   * and counts the fetches so the tests can check which one the ViewModel uses.
   */
  private class EventsRepositoryImpl(
      var eventList: List<Event> = listOf(),
      val error: Exception? = null,
  ) : EventsRepository {
    var getAllEventsCalls = 0
    var getEventsForUserCalls = 0

    override fun getNewUid(): String {
      return "${eventList.size}"
    }

    override suspend fun getAllEvents(): List<Event> {
      getAllEventsCalls++
      error?.let { throw it }
      return eventList
    }

    override suspend fun getEvent(eventId: String): Event {
      TODO("Not yet implemented")
    }

    override suspend fun getEventsForUser(userId: String): List<Event> {
      getEventsForUserCalls++
      error?.let { throw it }
      return eventList.filter { event -> event.members.any { it.uid == userId } }
    }

    override suspend fun addEvent(event: Event) {
      TODO("Not yet implemented")
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      TODO("Not yet implemented")
    }

    override suspend fun deleteEvent(eventId: String) {
      TODO("Not yet implemented")
    }
  }

  private fun getDate(year: Int, month: Int, day: Int, hour: Int = 0): ZonedDateTime {
    return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  }

  private val testDispatcher = StandardTestDispatcher()

  /** The fixed current time of every test: 17/10/2026 at 18:00. */
  private val now = getDate(2026, 10, 17, hour = 18)

  private val sara = User(uid = "u1", name = "Sara", surname = "Keller")

  private val ongoingEvent =
      Event(
          eventId = "0",
          title = "Fête de l'Asso",
          description = "The yearly party of the association",
          members = listOf(sara),
          startDate = getDate(2026, 10, 17, hour = 16),
          endDate = getDate(2026, 10, 17, hour = 23),
          location = "Esplanade EPFL",
          tasks =
              listOf(
                  Task(taskId = "t1", eventId = "0", title = "Set up the bar"),
                  Task(taskId = "t2", eventId = "0", title = "Sound check", completed = true),
                  Task(taskId = "t3", eventId = "0", title = "Clean up"),
              ),
      )

  private val upcomingEvent =
      Event(
          eventId = "1",
          title = "Soirée Jazz",
          description = "A jazz concert at Satellite",
          members = listOf(sara),
          startDate = getDate(2026, 10, 19, hour = 20),
          endDate = getDate(2026, 10, 20, hour = 1),
          location = "Satellite",
      )

  private val pastEvent =
      Event(
          eventId = "2",
          title = "Balélec",
          description = "The EPFL music festival",
          startDate = getDate(2026, 5, 8, hour = 17),
          endDate = getDate(2026, 5, 9, hour = 5),
          location = "EPFL",
      )

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(
      withRepository: EventsRepository,
      userId: String? = null,
  ): EventsOverviewViewModel {
    return EventsOverviewViewModel(
        eventsRepository = withRepository,
        userId = userId,
        now = { now },
    )
  }

  /** Asserts that [items] show exactly the [expected] events, in the same order. */
  private fun assertShows(expected: List<Event>, items: List<EventItemUIState>) {
    assertEquals(expected, items.map { it.event })
  }

  /** Test that the UI state is loading until the events are fetched, without any error. */
  @Test
  fun isLoadingUntilEventsAreFetched() = runTest {
    val viewModel = createViewModel(EventsRepositoryImpl(listOf(ongoingEvent)))
    assertTrue(viewModel.uiState.value.isLoading)

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Test that each event lands in the category matching its dates: ongoing, upcoming or past. */
  @Test
  fun eventsAreSplitIntoOngoingUpcomingAndPast() = runTest {
    val repository = EventsRepositoryImpl(listOf(pastEvent, upcomingEvent, ongoingEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertShows(listOf(ongoingEvent), state.ongoingEvents)
    assertShows(listOf(upcomingEvent), state.upcomingEvents)
    assertShows(listOf(pastEvent), state.pastEvents)
  }

  /**
   * Test that the events of each category are sorted: ongoing and upcoming ones by the soonest
   * start first, past ones by the most recent start first.
   */
  @Test
  fun eventsAreSortedInEachCategory() = runTest {
    val earlierOngoing =
        ongoingEvent.copy(eventId = "3", startDate = getDate(2026, 10, 16), tasks = listOf())
    val laterUpcoming =
        upcomingEvent.copy(
            eventId = "4",
            startDate = getDate(2026, 12, 1),
            endDate = getDate(2026, 12, 2),
        )
    val olderPast =
        pastEvent.copy(
            eventId = "5",
            startDate = getDate(2025, 5, 8),
            endDate = getDate(2025, 5, 9),
        )
    val repository =
        EventsRepositoryImpl(
            listOf(ongoingEvent, earlierOngoing, laterUpcoming, upcomingEvent, olderPast, pastEvent)
        )
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertShows(listOf(earlierOngoing, ongoingEvent), state.ongoingEvents)
    assertShows(listOf(upcomingEvent, laterUpcoming), state.upcomingEvents)
    assertShows(listOf(pastEvent, olderPast), state.pastEvents)
  }

  /** Test that a closed event is past, even if its end date is not reached yet. */
  @Test
  fun closedEventIsPastEvenIfNotEnded() = runTest {
    val closedOngoing = ongoingEvent.close()
    val closedUpcoming = upcomingEvent.close()
    val viewModel = createViewModel(EventsRepositoryImpl(listOf(closedOngoing, closedUpcoming)))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.ongoingEvents.isEmpty())
    assertTrue(state.upcomingEvents.isEmpty())
    assertShows(listOf(closedUpcoming, closedOngoing), state.pastEvents)
  }

  /**
   * Test the boundaries of the current time: an event ending right now is past, and an event
   * starting right now is ongoing.
   */
  @Test
  fun eventsAtTheCurrentTimeAreSplitCorrectly() = runTest {
    val endingNow = pastEvent.copy(startDate = getDate(2026, 10, 17, hour = 12), endDate = now)
    val startingNow =
        upcomingEvent.copy(startDate = now, endDate = getDate(2026, 10, 17, hour = 22))
    val viewModel = createViewModel(EventsRepositoryImpl(listOf(endingNow, startingNow)))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertShows(listOf(startingNow), state.ongoingEvents)
    assertTrue(state.upcomingEvents.isEmpty())
    assertShows(listOf(endingNow), state.pastEvents)
  }

  /**
   * Test that an event card counts only the tasks that are not completed, and that only ongoing
   * events are flagged as ongoing.
   */
  @Test
  fun eventItemsCountOpenTasksAndFlagOngoing() = runTest {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(
        EventItemUIState(ongoingEvent, openTasksCount = 2, isOngoing = true),
        state.ongoingEvents.single(),
    )
    assertEquals(EventItemUIState(upcomingEvent, openTasksCount = 0), state.upcomingEvents.single())
    assertEquals(EventItemUIState(pastEvent, openTasksCount = 0), state.pastEvents.single())
  }

  /** Test that, without a user, every event of the repository is fetched. */
  @Test
  fun withoutUserIdAllEventsAreFetched() = runTest {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    assertEquals(1, repository.getAllEventsCalls)
    assertEquals(0, repository.getEventsForUserCalls)
    assertShows(listOf(pastEvent), viewModel.uiState.value.pastEvents)
  }

  /** Test that, with a user, only the events the user is a member of are fetched. */
  @Test
  fun withUserIdOnlyTheirEventsAreFetched() = runTest {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    val viewModel = createViewModel(withRepository = repository, userId = sara.uid)
    advanceUntilIdle()

    assertEquals(0, repository.getAllEventsCalls)
    assertEquals(1, repository.getEventsForUserCalls)
    val state = viewModel.uiState.value
    assertShows(listOf(ongoingEvent), state.ongoingEvents)
    assertShows(listOf(upcomingEvent), state.upcomingEvents)
    assertTrue(state.pastEvents.isEmpty())
  }

  /** Test that a failing repository stops the loading and shows an error message. */
  @Test
  fun repositoryFailureShowsErrorMsg() = runTest {
    val repository = EventsRepositoryImpl(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to load events: Network down", state.errorMsg)
    assertTrue(state.hasNoEvents)
  }

  /** Test that the error message is removed once cleared. */
  @Test
  fun clearErrorMsgRemovesTheErrorMsg() = runTest {
    val repository = EventsRepositoryImpl(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Test that the ongoing events are shown by default, and that each filter shows its events. */
  @Test
  fun selectFilterChangesTheShownEvents() = runTest {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent, upcomingEvent, pastEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    assertEquals(EventsFilter.ONGOING, viewModel.uiState.value.selectedFilter)
    assertShows(listOf(ongoingEvent), viewModel.uiState.value.shownEvents)

    viewModel.selectFilter(EventsFilter.UPCOMING)
    assertEquals(EventsFilter.UPCOMING, viewModel.uiState.value.selectedFilter)
    assertShows(listOf(upcomingEvent), viewModel.uiState.value.shownEvents)

    viewModel.selectFilter(EventsFilter.PAST)
    assertEquals(EventsFilter.PAST, viewModel.uiState.value.selectedFilter)
    assertShows(listOf(pastEvent), viewModel.uiState.value.shownEvents)
  }

  /** Test that refreshing fetches the new events of the repository and keeps the filter. */
  @Test
  fun refreshFetchesNewEventsAndKeepsTheFilter() = runTest {
    val repository = EventsRepositoryImpl(listOf(ongoingEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    viewModel.selectFilter(EventsFilter.UPCOMING)
    assertTrue(viewModel.uiState.value.shownEvents.isEmpty())

    repository.eventList = listOf(ongoingEvent, upcomingEvent)
    viewModel.refreshUIState()
    assertTrue(viewModel.uiState.value.isLoading)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(EventsFilter.UPCOMING, state.selectedFilter)
    assertShows(listOf(upcomingEvent), state.shownEvents)
  }

  /** Test that there are no events only when every category is empty. */
  @Test
  fun hasNoEventsOnlyWhenEveryCategoryIsEmpty() = runTest {
    val emptyViewModel = createViewModel(EventsRepositoryImpl())
    advanceUntilIdle()
    assertTrue(emptyViewModel.uiState.value.hasNoEvents)

    val pastOnlyViewModel = createViewModel(EventsRepositoryImpl(listOf(pastEvent)))
    advanceUntilIdle()
    assertFalse(pastOnlyViewModel.uiState.value.hasNoEvents)
  }
}
