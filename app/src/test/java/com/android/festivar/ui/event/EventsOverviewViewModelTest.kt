// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

  /** A [UsersRepositoryLocal] serving [users], which records for which uids they are fetched. */
  private class RecordingUsersRepository(
      users: List<User> = listOf(),
      private val local: UsersRepository = UsersRepositoryLocal(users),
  ) : UsersRepository by local {
    val requestedUids = mutableListOf<String>()

    override suspend fun getUser(uId: String): User {
      requestedUids.add(uId)
      return local.getUser(uId)
    }
  }

  private fun getDate(year: Int, month: Int, day: Int, hour: Int = 0): ZonedDateTime {
    return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  }

  private val testDispatcher = StandardTestDispatcher()

  /** The fixed current time of every test: 17/10/2026 at 18:00. */
  private val now = getDate(2026, 10, 17, hour = 18)

  /** The signed-in user, as stored in the users repository. */
  private val sara = User(uid = "u1", name = "Sara", surname = "Keller")

  /** What the avatar shows until the stored [sara] is fetched. */
  private val saraWithoutProfile = User(uid = sara.uid)

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

  /**
   * Creates the ViewModel and, unless [shown] is `false`, refreshes it once, as the screen does
   * when it is shown.
   */
  private fun createViewModel(
      withRepository: EventsRepository,
      usersRepository: UsersRepository = RecordingUsersRepository(listOf(sara)),
      shown: Boolean = true,
  ): EventsOverviewViewModel {
    val viewModel =
        EventsOverviewViewModel(
            eventsRepository = withRepository,
            usersRepository = usersRepository,
            uId = sara.uid,
            now = { now },
        )
    if (shown) viewModel.refreshUIState()
    return viewModel
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

  /**
   * Test that nothing is fetched until the screen refreshes the ViewModel, and that the UI state is
   * already loading until then, so that the screen never shows its empty state too early.
   */
  @Test
  fun nothingIsFetchedUntilRefreshed() = runTest {
    val repository = RecordingEventsRepository(listOf(ongoingEvent))
    val usersRepository = RecordingUsersRepository(listOf(sara))
    val viewModel =
        createViewModel(
            withRepository = repository,
            usersRepository = usersRepository,
            shown = false,
        )
    advanceUntilIdle()

    assertTrue(repository.requestedUserIds.isEmpty())
    assertTrue(usersRepository.requestedUids.isEmpty())
    val state = viewModel.uiState.value
    assertTrue(state.isLoading)
    assertTrue(state.hasNoEvents)
    assertFalse(state.hasError)
    assertEquals(saraWithoutProfile, state.user)

    viewModel.refreshUIState()
    advanceUntilIdle()

    assertEquals(listOf(sara.uid), repository.requestedUserIds)
    assertEquals(listOf(sara.uid), usersRepository.requestedUids)
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(listOf(ongoingEvent), viewModel.uiState.value.ongoingEvents)
  }

  /** Test that the UI state is loading until the events are fetched, without any error. */
  @Test
  fun isLoadingUntilEventsAreFetched() = runTest {
    val viewModel = createViewModel(RecordingEventsRepository(listOf(ongoingEvent)))
    assertTrue(viewModel.uiState.value.isLoading)

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.hasError)
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
    val viewModel = createViewModel(withRepository = repository)
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
    val viewModel = createViewModel(withRepository = repository)
    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(saraWithoutProfile, viewModel.uiState.value.user)

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(sara, viewModel.uiState.value.user)

    repository.error = RuntimeException("Network down")
    viewModel.refreshUIState()
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertTrue(state.hasError)
    assertEquals(sara, state.user)
  }

  /**
   * Test that the user only has the uid of the signed-in user until its stored profile is fetched,
   * and that the profile and the events are both fetched with that uid.
   */
  @Test
  fun storedUserIsShownOnceFetched() = runTest {
    val repository = RecordingEventsRepository()
    val usersRepository = RecordingUsersRepository(listOf(sara))
    val viewModel = createViewModel(withRepository = repository, usersRepository = usersRepository)
    assertEquals(saraWithoutProfile, viewModel.uiState.value.user)

    advanceUntilIdle()

    assertEquals(sara, viewModel.uiState.value.user)
    assertEquals(listOf(sara.uid), usersRepository.requestedUids)
    assertEquals(listOf(sara.uid), repository.requestedUserIds)
  }

  /**
   * Test that, if the stored profile cannot be fetched, the user keeps only its uid and the events
   * are still shown, without any error.
   */
  @Test
  fun missingStoredUserKeepsTheUidAndShowsTheEvents() = runTest {
    val viewModel =
        createViewModel(
            withRepository = RecordingEventsRepository(listOf(ongoingEvent)),
            usersRepository = RecordingUsersRepository(),
        )
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(saraWithoutProfile, state.user)
    assertEquals(listOf(ongoingEvent), state.ongoingEvents)
    assertFalse(state.isLoading)
    assertFalse(state.hasError)
    assertNull(state.errorMsg)
  }

  /** Test that a refresh fetches the stored profile again, e.g. after it was edited. */
  @Test
  fun refreshFetchesTheStoredUserAgain() = runTest {
    val usersRepository = RecordingUsersRepository(listOf(sara))
    val viewModel =
        createViewModel(
            withRepository = RecordingEventsRepository(),
            usersRepository = usersRepository,
        )
    advanceUntilIdle()

    val renamed = sara.copy(name = "Sarah")
    usersRepository.editUser(sara.uid, renamed)
    viewModel.refreshUIState()
    advanceUntilIdle()

    assertEquals(renamed, viewModel.uiState.value.user)
    assertEquals(listOf(sara.uid, sara.uid), usersRepository.requestedUids)
  }

  /**
   * Test that a failing repository stops the loading and sets the error, with the cause given by
   * the repository as is: the screen words the error itself.
   */
  @Test
  fun repositoryFailureSetsTheErrorAndItsCause() = runTest {
    val repository = RecordingEventsRepository(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.hasError)
    assertEquals("Network down", state.errorMsg)
    assertTrue(state.hasNoEvents)
  }

  /** Test that a failure whose exception has no message still sets the error, without a cause. */
  @Test
  fun failureWithoutMessageSetsTheErrorWithoutCause() = runTest {
    val repository = RecordingEventsRepository(error = NoSuchElementException())
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.hasError)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /**
   * Test that a failed refresh keeps the events already shown, and their filter, with the error.
   */
  @Test
  fun failedRefreshKeepsTheEventsShown() = runTest {
    val repository = RecordingEventsRepository(listOf(ongoingEvent, upcomingEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    viewModel.selectFilter(EventsFilter.UPCOMING)

    repository.error = RuntimeException("Network down")
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.hasError)
    assertEquals("Network down", state.errorMsg)
    assertEquals(listOf(ongoingEvent), state.ongoingEvents)
    assertEquals(EventsFilter.UPCOMING, state.selectedFilter)
    assertEquals(listOf(upcomingEvent), state.shownEvents)
  }

  /**
   * Test that the error stays while a retry loads, so that the screen keeps showing it, and is
   * still there if the retry fails too.
   */
  @Test
  fun errorStaysWhileRetrying() = runTest {
    val repository = RecordingEventsRepository(error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    viewModel.refreshUIState()
    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.hasError)
    assertEquals("Network down", viewModel.uiState.value.errorMsg)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.hasError)
    assertEquals("Network down", state.errorMsg)
    assertEquals(listOf(sara.uid, sara.uid), repository.requestedUserIds)
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
   * Test that refreshing after the repository recovers shows the events and removes the error and
   * its cause.
   */
  @Test
  fun refreshAfterFailureShowsTheEventsWithoutError() = runTest {
    val repository =
        RecordingEventsRepository(listOf(ongoingEvent), error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.hasError)

    repository.error = null
    viewModel.refreshUIState()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.hasError)
    assertNull(state.errorMsg)
    assertEquals(listOf(ongoingEvent), state.ongoingEvents)
  }

  /**
   * Test that the first load selects the first category with events, among ongoing, upcoming and
   * past, and the ongoing one if there is no event at all.
   */
  @Test
  fun firstLoadSelectsTheFirstCategoryWithEvents() = runTest {
    mapOf(
            listOf(ongoingEvent, upcomingEvent, pastEvent) to EventsFilter.ONGOING,
            listOf(upcomingEvent, pastEvent) to EventsFilter.UPCOMING,
            listOf(pastEvent) to EventsFilter.PAST,
            listOf<Event>() to EventsFilter.ONGOING,
        )
        .forEach { (events, filter) ->
          val viewModel = createViewModel(RecordingEventsRepository(events))
          advanceUntilIdle()
          assertEquals(filter, viewModel.uiState.value.selectedFilter)
        }
  }

  /**
   * Test that the filter selected by the first load is kept by a refresh, even if a category before
   * it has events since.
   */
  @Test
  fun refreshKeepsTheFilterOfTheFirstLoad() = runTest {
    val repository = RecordingEventsRepository(listOf(pastEvent))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    assertEquals(EventsFilter.PAST, viewModel.uiState.value.selectedFilter)

    repository.addEvent(ongoingEvent)
    viewModel.refreshUIState()
    advanceUntilIdle()

    assertEquals(listOf(ongoingEvent), viewModel.uiState.value.ongoingEvents)
    assertEquals(EventsFilter.PAST, viewModel.uiState.value.selectedFilter)
  }

  /** Test that a filter selected by the user before the first load ends is not overridden by it. */
  @Test
  fun filterSelectedBeforeTheFirstLoadIsKept() = runTest {
    val repository = GatedEventsRepository()
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()

    viewModel.selectFilter(EventsFilter.PAST)
    repository.fetches.single().complete(listOf(ongoingEvent))
    advanceUntilIdle()

    assertEquals(listOf(ongoingEvent), viewModel.uiState.value.ongoingEvents)
    assertEquals(EventsFilter.PAST, viewModel.uiState.value.selectedFilter)
  }

  /** Test that a failed first load leaves the choice of the filter to the next successful one. */
  @Test
  fun failedFirstLoadLeavesTheFilterToTheNextOne() = runTest {
    val repository =
        RecordingEventsRepository(listOf(upcomingEvent), error = RuntimeException("Network down"))
    val viewModel = createViewModel(withRepository = repository)
    advanceUntilIdle()
    assertEquals(EventsFilter.ONGOING, viewModel.uiState.value.selectedFilter)

    repository.error = null
    viewModel.refreshUIState()
    advanceUntilIdle()

    assertEquals(EventsFilter.UPCOMING, viewModel.uiState.value.selectedFilter)
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
    assertFalse(viewModel.uiState.value.hasError)
    assertTrue(viewModel.uiState.value.isLoading)

    repository.fetches.last().complete(listOf(ongoingEvent))
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.hasError)
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
    assertFalse(viewModel.uiState.value.hasError)

    newerFetch.complete(listOf(upcomingEvent))
    advanceUntilIdle()
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.hasError)
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
    val viewModel =
        EventsOverviewViewModel(
            eventsRepository = repository,
            usersRepository = RecordingUsersRepository(listOf(sara)),
            uId = sara.uid,
        )
    viewModel.refreshUIState()
    advanceUntilIdle()

    assertEquals(listOf(sara.uid), repository.requestedUserIds)
    val state = viewModel.uiState.value
    assertEquals(sara, state.user)
    assertTrue(state.ongoingEvents.isEmpty())
    assertEquals(listOf(nextYear), state.upcomingEvents)
    assertEquals(listOf(lastYear), state.pastEvents)
  }
}
