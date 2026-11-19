package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
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
class EventDetailsViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** Red if the ViewModel does not load the event identified by its constructor argument. */
  @Test
  fun fetchEventLoadsEvent() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            "owner",
        )
    viewModel.fetchEvent()

    advanceUntilIdle()

    assertEquals(event, viewModel.uiState.value.event)
  }

  /** Red if a repository failure does not expose the event fetch error resource. */
  @Test
  fun fetchEventReportsErrorWhenRepositoryFails() = runTest {
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(
                object : EventsRepository by EventsRepositoryLocal() {
                  override suspend fun getEvent(eventId: String): Event {
                    throw IllegalStateException("fetch failed")
                  }
                }
            ),
            eventId = "missing",
            currentUserId = "owner",
        )

    viewModel.fetchEvent()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.event)
    assertEquals(R.string.event_details_fetch_error, viewModel.uiState.value.errorMsg)
  }

  /** Red if a failed reload leaves stale event data visible. */
  @Test
  fun failedRefetchClearsPreviouslyLoadedEvent() = runTest {
    var shouldFail = false
    val event = testEvent("event-1")
    val repository =
        object : EventsRepository by EventsRepositoryLocal(listOf(event)) {
          override suspend fun getEvent(eventId: String): Event {
            if (shouldFail) throw IllegalStateException("fetch failed")
            return event
          }
        }
    val viewModel = EventDetailsViewModel(dataRepositoryWith(repository), event.eventId, "owner")

    viewModel.fetchEvent()
    advanceUntilIdle()
    shouldFail = true
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.event)
    assertEquals(R.string.event_details_fetch_error, viewModel.uiState.value.errorMsg)
  }

  /** Red if a successful reload does not clear the previous fetch error. */
  @Test
  fun successfulRefetchClearsPreviousError() = runTest {
    var shouldFail = true
    val event = testEvent("event-1")
    val repository =
        object : EventsRepository by EventsRepositoryLocal(listOf(event)) {
          override suspend fun getEvent(eventId: String): Event {
            if (shouldFail) throw IllegalStateException("fetch failed")
            return event
          }
        }
    val viewModel = EventDetailsViewModel(dataRepositoryWith(repository), event.eventId, "owner")
    viewModel.fetchEvent()

    advanceUntilIdle()
    assertEquals(R.string.event_details_fetch_error, viewModel.uiState.value.errorMsg)

    shouldFail = false
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertEquals(event, viewModel.uiState.value.event)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Red if cancellation is incorrectly converted into a user-facing fetch error. */
  @Test
  fun fetchEventDoesNotConvertCancellationExceptionIntoFetchError() = runTest {
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(
                object : EventsRepository by EventsRepositoryLocal() {
                  override suspend fun getEvent(eventId: String): Event {
                    throw CancellationException("fetch cancelled")
                  }
                }
            ),
            eventId = "event-1",
            currentUserId = "owner",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.event)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Breaks if the organizer of the event is not recognised, so they never get the menu. */
  @Test
  fun ownerOfTheEventIsTheOwner() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            event.ownerId,
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isOwner)
  }

  /** Breaks if a volunteer is treated as the organizer and gets the menu. */
  @Test
  fun anotherUserIsNotTheOwner() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            "volunteer",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isOwner)
  }

  /** Breaks if nobody signed in is treated as the organizer. */
  @Test
  fun signedOutUserIsNotTheOwner() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            "",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isOwner)
  }

  /** Breaks if a failed reload keeps the organizer's rights over an event that is not shown. */
  @Test
  fun failedRefetchRemovesOwnership() = runTest {
    var shouldFail = false
    val event = testEvent("event-1")
    val repository =
        object : EventsRepository by EventsRepositoryLocal(listOf(event)) {
          override suspend fun getEvent(eventId: String): Event {
            if (shouldFail) throw IllegalStateException("fetch failed")
            return event
          }
        }
    val viewModel = EventDetailsViewModel(dataRepositoryWith(repository), event.eventId, "owner")
    viewModel.fetchEvent()
    advanceUntilIdle()
    shouldFail = true
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isOwner)
  }

  private fun dataRepositoryWith(eventsRepository: EventsRepository) =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
      )

  private fun testEvent(eventId: String): Event {
    val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    return Event(
        eventId = eventId,
        ownerId = "owner",
        title = "Test event",
        startDate = start,
        endDate = start.plusHours(2),
        members = listOf(User(uid = "owner"), User(uid = "volunteer")),
    )
  }
}
