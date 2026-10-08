// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.temporary.User
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
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
   * An [EventsRepositoryLocal] serving [events]. It throws [error] on every fetch of the events of
   * a user while it is set, and records for which users they are fetched.
   */
  private class RecordingEventsRepository(
      events: List<Event> = listOf(),
      var error: Exception? = null,
      private val local: EventsRepository = EventsRepositoryLocal(events),
  ) : EventsRepository by local {
    val requestedUserIds = mutableListOf<String>()

    override suspend fun getEventsForUser(userId: String): List<Event> {
      requestedUserIds.add(userId)
      error?.let { throw it }
      return local.getEventsForUser(userId)
    }
  }

  /**
   * A repository whose fetches of the [Event] items of a user wait until the test completes them
   * with the [Event] items to return, so that the tests control the order in which concurrent
   * fetches end. If [ignoresCancellation], a fetch still returns its [Event] items once cancelled,
   * as a repository that does not check for cancellation would.
   */
  private class GatedEventsRepository(
      private val ignoresCancellation: Boolean = false,
      private val local: EventsRepository = EventsRepositoryLocal(),
  ) : EventsRepository by local {
    /** Every fetch started so far, the oldest first. */
    val fetches = mutableListOf<CompletableDeferred<List<Event>>>()

    override suspend fun getEventsForUser(userId: String): List<Event> {
      val fetch = CompletableDeferred<List<Event>>()
      fetches.add(fetch)
      return if (ignoresCancellation) withContext(NonCancellable) { fetch.await() }
      else fetch.await()
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
          members = listOf(sara),
          startDate = getDate(2026, 5, 8, hour = 17),
          endDate = getDate(2026, 5, 9, hour = 5),
          location = "EPFL",
      )

  /** An event Sara is not a member of, so it is never shown to her. */
  private val eventWithoutSara =
      pastEvent.copy(eventId = "6", title = "Satellite Quiz", members = listOf())

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
      user: User = sara,
  ): EventsOverviewViewModel {
    return EventsOverviewViewModel(
        eventsRepository = withRepository,
        user = user,
        now = { now },
    )
  }

  /**
   * Creates a ViewModel on [repository] and refreshes it while its first fetch is still pending.
   */
  private fun TestScope.refreshDuringTheFirstFetch(
      repository: GatedEventsRepository
  ): EventsOverviewViewModel {
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    viewModel.refreshUIState()
    advanceUntilIdle()
    assertEquals(2, repository.fetches.size)
    return viewModel
  }

  /** Test that the UI state is loading until the events are fetched, without any error. */
  @Test
  fun isLoadingUntilEventsAreFetched() = runTest {
    val viewModel = createViewModel(RecordingEventsRepository(listOf(ongoingEvent)))
    assertTrue(viewModel.uiState.value.isLoading)

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Test that each event lands in the category matching its dates: ongoing, upcoming or past. */
  @Test
  fun eventsAreSplitIntoOngoingUpcomingAndPast() = runTest {
    val repository = RecordingEventsRepository(listOf(pastEvent, upcomingEvent, ongoingEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(ongoingEvent), state.ongoingEvents)
    assertEquals(listOf(upcomingEvent), state.upcomingEvents)
    assertEquals(listOf(pastEvent), state.pastEvents)
  }

  /**
   * Test that the events of each category are sorted: ongoing and upcoming ones by the soonest
   * start first, past ones by the most recently ended first.
   */
  @Test
  fun eventsAreSortedInEachCategory() = runTest {
    val earlierOngoing = ongoingEvent.copy(eventId = "3", startDate = getDate(2026, 10, 16))
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
        RecordingEventsRepository(
            listOf(ongoingEvent, earlierOngoing, laterUpcoming, upcomingEvent, olderPast, pastEvent)
        )
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(earlierOngoing, ongoingEvent), state.ongoingEvents)
    assertEquals(listOf(upcomingEvent, laterUpcoming), state.upcomingEvents)
    assertEquals(listOf(pastEvent, olderPast), state.pastEvents)
  }

  /**
   * Test that past events are sorted by their end, not their start: an event that started earlier
   * but ended later comes first.
   */
  @Test
  fun pastEventsAreSortedByTheirEnd() = runTest {
    val longPast =
        pastEvent.copy(
            eventId = "7",
            startDate = getDate(2026, 1, 1),
            endDate = getDate(2026, 9, 1),
        )
    val shortPast =
        pastEvent.copy(
            eventId = "8",
            startDate = getDate(2026, 5, 1),
            endDate = getDate(2026, 5, 2),
        )
    val viewModel = createViewModel(RecordingEventsRepository(listOf(shortPast, longPast)))
    advanceUntilIdle()

    assertEquals(listOf(longPast, shortPast), viewModel.uiState.value.pastEvents)
  }

  /**
   * Test that a closed event is past, even if its end date is not reached yet, and is sorted with
   * the other past events by its end date.
   */
  @Test
  fun closedEventIsPastEvenIfNotEnded() = runTest {
    val closedOngoing = ongoingEvent.close()
    val closedUpcoming = upcomingEvent.close()
    val viewModel =
        createViewModel(RecordingEventsRepository(listOf(closedOngoing, closedUpcoming)))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.ongoingEvents.isEmpty())
    assertTrue(state.upcomingEvents.isEmpty())
    assertEquals(listOf(closedUpcoming, closedOngoing), state.pastEvents)
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
    val viewModel = createViewModel(RecordingEventsRepository(listOf(endingNow, startingNow)))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(listOf(startingNow), state.ongoingEvents)
    assertTrue(state.upcomingEvents.isEmpty())
    assertEquals(listOf(endingNow), state.pastEvents)
  }

  /** Test that only the events the user is a member of are fetched, by its uid. */
  @Test
  fun onlyTheEventsOfTheUserAreFetched() = runTest {
    val repository =
        RecordingEventsRepository(listOf(ongoingEvent, upcomingEvent, pastEvent, eventWithoutSara))
    val viewModel = createViewModel(withRepository = repository, user = sara)
    advanceUntilIdle()

    assertEquals(listOf(sara.uid), repository.requestedUserIds)
    val state = viewModel.uiState.value
    assertEquals(listOf(ongoingEvent), state.ongoingEvents)
    assertEquals(listOf(upcomingEvent), state.upcomingEvents)
    assertEquals(listOf(pastEvent), state.pastEvents)
  }

  /**
   * Test that the signed-in user is in the UI state from the start, while the events load, and
   * stays there once they are fetched and after a refresh fails.
   */
  @Test
  fun userStaysInTheUIState() = runTest {
    val repository = RecordingEventsRepository(listOf(ongoingEvent))
    val viewModel = createViewModel(withRepository = repository, user = sara)
    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(sara, viewModel.uiState.value.user)

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(sara, viewModel.uiState.value.user)

    repository.error = RuntimeException("Network down")
    viewModel.refreshUIState()
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertEquals("Failed to load events: Network down", state.errorMsg)
    assertEquals(sara, state.user)
  }

  /** Test that a failing repository stops the loading and shows an error message. */
  @Test
  fun repositoryFailureShowsErrorMsg() = runTest {
    val repository = RecordingEventsRepository(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to load events: Network down", state.errorMsg)
    assertTrue(state.hasNoEvents)
  }

  /** Test that a failure whose exception has no message still shows an error message. */
  @Test
  fun failureWithoutMessageShowsUnknownError() = runTest {
    val repository = RecordingEventsRepository(error = NoSuchElementException())
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    assertEquals("Failed to load events: unknown error", viewModel.uiState.value.errorMsg)
  }

  /** Test that the error message is removed once cleared. */
  @Test
  fun clearErrorMsgRemovesTheErrorMsg() = runTest {
    val repository = RecordingEventsRepository(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Test that the ongoing events are shown by default, and that each filter shows its events. */
  @Test
  fun selectFilterChangesTheShownEvents() = runTest {
    val repository = RecordingEventsRepository(listOf(ongoingEvent, upcomingEvent, pastEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    assertEquals(EventsFilter.ONGOING, viewModel.uiState.value.selectedFilter)
    assertEquals(listOf(ongoingEvent), viewModel.uiState.value.shownEvents)

    viewModel.selectFilter(EventsFilter.UPCOMING)
    assertEquals(EventsFilter.UPCOMING, viewModel.uiState.value.selectedFilter)
    assertEquals(listOf(upcomingEvent), viewModel.uiState.value.shownEvents)

    viewModel.selectFilter(EventsFilter.PAST)
    assertEquals(EventsFilter.PAST, viewModel.uiState.value.selectedFilter)
    assertEquals(listOf(pastEvent), viewModel.uiState.value.shownEvents)
  }

  /**
   * Test that refreshing fetches the events of the user again, including the ones it joined since
   * but not the ones of others, and keeps the filter.
   */
  @Test
  fun refreshFetchesNewEventsAndKeepsTheFilter() = runTest {
    val repository = RecordingEventsRepository(listOf(ongoingEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    viewModel.selectFilter(EventsFilter.UPCOMING)
    assertTrue(viewModel.uiState.value.shownEvents.isEmpty())

    repository.addEvent(upcomingEvent)
    repository.addEvent(eventWithoutSara)
    viewModel.refreshUIState()
    assertTrue(viewModel.uiState.value.isLoading)
    advanceUntilIdle()

    assertEquals(listOf(sara.uid, sara.uid), repository.requestedUserIds)
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(EventsFilter.UPCOMING, state.selectedFilter)
    assertEquals(listOf(upcomingEvent), state.shownEvents)
    assertTrue(state.pastEvents.isEmpty())
  }

  /**
   * Test that refreshing after the repository recovers shows the events and removes the error
   * message, without clearing it first.
   */
  @Test
  fun refreshAfterFailureShowsTheEventsWithoutError() = runTest {
    val repository =
        RecordingEventsRepository(listOf(ongoingEvent), error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    assertEquals("Failed to load events: Network down", viewModel.uiState.value.errorMsg)

    repository.error = null
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(listOf(ongoingEvent), state.ongoingEvents)
  }

  /**
   * Test that two identical failures in a row both surface: a refresh clears the error message
   * while it loads, so a screen keyed on the error message sees the second failure as a new one.
   */
  @Test
  fun identicalFailuresInARowBothSurface() = runTest {
    val repository = RecordingEventsRepository(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    // Every change of the error message, as a screen keyed on it would see them
    val errorMsgs = mutableListOf<String?>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.map { it.errorMsg }.distinctUntilChanged().toList(errorMsgs)
    }
    advanceUntilIdle()

    viewModel.refreshUIState()
    assertNull(viewModel.uiState.value.errorMsg)
    advanceUntilIdle()

    val failure = "Failed to load events: Network down"
    assertEquals(listOf(null, failure, null, failure), errorMsgs)
  }

  /**
   * Test that, when a refresh starts before the previous fetch ends, the older fetch never
   * overwrites the newer one, even if it ends after it.
   */
  @Test
  fun olderFetchDoesNotOverwriteANewerOne() = runTest {
    val repository = GatedEventsRepository()
    val viewModel = refreshDuringTheFirstFetch(repository)
    val (olderFetch, newerFetch) = repository.fetches

    newerFetch.complete(listOf(upcomingEvent))
    advanceUntilIdle()
    olderFetch.complete(listOf(pastEvent))
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(listOf(upcomingEvent), state.upcomingEvents)
    assertTrue(state.pastEvents.isEmpty())
  }

  /**
   * Test that the UI state stays loading until the latest fetch ends, even if an older fetch ends
   * before it.
   */
  @Test
  fun isLoadingUntilTheLatestFetchEnds() = runTest {
    val repository = GatedEventsRepository()
    val viewModel = refreshDuringTheFirstFetch(repository)
    val (olderFetch, newerFetch) = repository.fetches

    olderFetch.complete(listOf(pastEvent))
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.hasNoEvents)

    newerFetch.complete(listOf(upcomingEvent))
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(listOf(upcomingEvent), state.upcomingEvents)
    assertTrue(state.pastEvents.isEmpty())
  }

  /** Test that a fetch cancelled by a newer refresh does not show an error message. */
  @Test
  fun cancelledFetchDoesNotShowAnError() = runTest {
    val repository = GatedEventsRepository()
    val viewModel = refreshDuringTheFirstFetch(repository)
    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.isLoading)

    repository.fetches.last().complete(listOf(ongoingEvent))
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    assertEquals(listOf(ongoingEvent), viewModel.uiState.value.ongoingEvents)
  }

  /**
   * Test that a cancelled fetch that still returns, because the repository ignores cancellation,
   * neither shows its stale events nor an error message, and leaves the newer fetch to end the
   * loading.
   */
  @Test
  fun staleResultOfACancelledFetchIsDropped() = runTest {
    val repository = GatedEventsRepository(ignoresCancellation = true)
    val viewModel = refreshDuringTheFirstFetch(repository)
    val (olderFetch, newerFetch) = repository.fetches

    olderFetch.complete(listOf(pastEvent))
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.hasNoEvents)
    assertNull(viewModel.uiState.value.errorMsg)

    newerFetch.complete(listOf(upcomingEvent))
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(listOf(upcomingEvent), state.upcomingEvents)
    assertTrue(state.pastEvents.isEmpty())
  }

  /** Test that there are no events only when every category is empty. */
  @Test
  fun hasNoEventsOnlyWhenEveryCategoryIsEmpty() = runTest {
    val emptyViewModel = createViewModel(RecordingEventsRepository())
    advanceUntilIdle()
    assertTrue(emptyViewModel.uiState.value.hasNoEvents)

    // One event in a single category is enough, whichever the category
    listOf(ongoingEvent, upcomingEvent, pastEvent).forEach { event ->
      val viewModel = createViewModel(RecordingEventsRepository(listOf(event)))
      advanceUntilIdle()
      assertFalse(viewModel.uiState.value.hasNoEvents)
    }
  }

  /** Test that, with the default clock, the events of the user are split with the real time. */
  @Test
  fun defaultClockSplitsEventsWithTheRealTime() = runTest {
    val realNow = ZonedDateTime.now()
    val lastYear =
        pastEvent.copy(
            startDate = realNow.minusYears(1),
            endDate = realNow.minusYears(1).plusHours(5),
        )
    val nextYear =
        upcomingEvent.copy(
            startDate = realNow.plusYears(1),
            endDate = realNow.plusYears(1).plusHours(5),
        )
    val repository = RecordingEventsRepository(listOf(lastYear, nextYear))
    val viewModel = EventsOverviewViewModel(eventsRepository = repository, user = sara)
    advanceUntilIdle()

    assertEquals(listOf(sara.uid), repository.requestedUserIds)
    val state = viewModel.uiState.value
    assertEquals(sara, state.user)
    assertTrue(state.ongoingEvents.isEmpty())
    assertEquals(listOf(nextYear), state.upcomingEvents)
    assertEquals(listOf(lastYear), state.pastEvents)
  }
}
