package com.android.festivar.ui.event.details

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

  /** Breaks if the organizer's deletion does not delete that event, then leave once. */
  @Test
  fun ownerDeletesTheEventThenLeaves() = runTest {
    val deleted = mutableListOf<String>()
    var leaves = 0
    val viewModel = loadedViewModel("owner") { deleted += it }

    viewModel.deleteEvent { leaves++ }
    advanceUntilIdle()

    assertEquals(listOf("event-1"), deleted)
    assertEquals(1, leaves)
    assertFalse(viewModel.uiState.value.isDeleting)
  }

  /** Breaks if a volunteer can delete the event of its organizer. */
  @Test
  fun volunteerCannotDeleteTheEvent() = runTest {
    val deleted = mutableListOf<String>()
    var leaves = 0
    val viewModel = loadedViewModel("volunteer") { deleted += it }

    viewModel.deleteEvent { leaves++ }
    advanceUntilIdle()

    assertTrue(deleted.isEmpty())
    assertEquals(0, leaves)
  }

  /** Breaks if a failed deletion leaves the screen, hides the event or shows no error. */
  @Test
  fun failedDeletionKeepsTheEventAndShowsAnError() = runTest {
    var leaves = 0
    val viewModel = loadedViewModel("owner") { throw IllegalStateException("delete failed") }

    viewModel.deleteEvent { leaves++ }
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(0, leaves)
    assertEquals("event-1", state.event?.eventId)
    assertEquals(R.string.event_details_delete_error, state.errorMsg)
    assertFalse(state.isDeleting)
  }

  /**
   * Breaks if a second tap deletes the event twice, or if a reload during the deletion resets the
   * screen, so the organizer could tap Delete again.
   */
  @Test
  fun deletionRunsOnceAndIsNotResetByAReload() = runTest {
    val gate = CompletableDeferred<Unit>()
    var deletions = 0
    var leaves = 0
    val viewModel =
        loadedViewModel("owner") {
          deletions++
          gate.await()
        }

    viewModel.deleteEvent { leaves++ }
    advanceUntilIdle()
    viewModel.deleteEvent { leaves++ }
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isDeleting)
    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(1, deletions)
    assertEquals(1, leaves)
  }

  /** Breaks if a cancelled deletion stays in progress, so the event can never be deleted again. */
  @Test
  fun cancelledDeletionCanBeRetried() = runTest {
    val viewModel = loadedViewModel("owner") { throw CancellationException("cancelled") }

    viewModel.deleteEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isDeleting)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /**
   * A view model of `event-1`, loaded for [currentUserId], whose repository runs [onDelete] instead
   * of deleting the event.
   */
  private fun TestScope.loadedViewModel(
      currentUserId: String,
      onDelete: suspend (String) -> Unit,
  ): EventDetailsViewModel {
    val base = dataRepositoryWith(EventsRepositoryLocal(listOf(testEvent("event-1"))))
    val dataRepository =
        object : DataRepository by base {
          override suspend fun deleteEvent(eventId: String) = onDelete(eventId)
        }
    val viewModel = EventDetailsViewModel(dataRepository, "event-1", currentUserId)
    viewModel.fetchEvent()
    advanceUntilIdle()
    return viewModel
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
