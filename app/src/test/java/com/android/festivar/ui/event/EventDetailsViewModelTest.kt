package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.event.details.EventDetailsViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
    kotlinx.coroutines.Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    kotlinx.coroutines.Dispatchers.resetMain()
  }

  /** Red if the ViewModel does not load the event identified by its constructor argument. */
  @Test
  fun fetchEventLoadsEvent() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
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
    val viewModel = EventDetailsViewModel(dataRepositoryWith(repository), event.eventId)

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
    val viewModel = EventDetailsViewModel(dataRepositoryWith(repository), event.eventId)
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
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.event)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Breaks if asking to delete deletes the event before the user confirmed. */
  @Test
  fun requestDeleteOnlyAsksForConfirmation() = runTest {
    val (viewModel, deleted) = deletableViewModel()

    viewModel.requestDelete()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.deleteRequested)
    assertTrue(deleted.isEmpty())
  }

  /** Breaks if cancelling the confirmation deletes the event or leaves the dialog open. */
  @Test
  fun cancelDeleteClosesTheRequestAndKeepsTheEvent() = runTest {
    val (viewModel, deleted) = deletableViewModel()
    viewModel.requestDelete()

    viewModel.cancelDelete()
    viewModel.confirmDelete {}
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.deleteRequested)
    assertTrue(deleted.isEmpty())
  }

  /** Breaks if the event can be deleted without a prior request, i.e. without a confirmation. */
  @Test
  fun confirmDeleteWithoutRequestDeletesNothing() = runTest {
    val (viewModel, deleted) = deletableViewModel()
    var left = false

    viewModel.confirmDelete { left = true }
    advanceUntilIdle()

    assertTrue(deleted.isEmpty())
    assertFalse(left)
  }

  /** Breaks if a confirmed deletion does not delete the event and then leave the screen. */
  @Test
  fun confirmDeleteDeletesTheEventThenLeaves() = runTest {
    val (viewModel, deleted) = deletableViewModel()
    var left = false
    viewModel.requestDelete()

    viewModel.confirmDelete { left = true }
    advanceUntilIdle()

    assertEquals(listOf("event-1"), deleted)
    assertTrue(left)
    assertFalse(viewModel.uiState.value.deleteRequested)
    assertFalse(viewModel.uiState.value.isDeleting)
    assertNull(viewModel.uiState.value.deleteErrorMsg)
  }

  /** Breaks if confirming twice in a row deletes the event twice. */
  @Test
  fun confirmDeleteTwiceDeletesOnce() = runTest {
    val (viewModel, deleted) = deletableViewModel()
    viewModel.requestDelete()

    viewModel.confirmDelete {}
    viewModel.confirmDelete {}
    advanceUntilIdle()

    assertEquals(1, deleted.size)
  }

  /**
   * Breaks if a failed deletion does not show an error, stays stuck, or still leaves the screen.
   */
  @Test
  fun failedDeleteShowsErrorAndStaysOnTheScreen() = runTest {
    val (viewModel, _) = deletableViewModel(failure = IllegalStateException("delete failed"))
    var left = false
    viewModel.fetchEvent()
    advanceUntilIdle()
    viewModel.requestDelete()

    viewModel.confirmDelete { left = true }
    advanceUntilIdle()

    assertFalse(left)
    assertFalse(viewModel.uiState.value.isDeleting)
    assertEquals(R.string.event_details_delete_error, viewModel.uiState.value.deleteErrorMsg)
    assertEquals("event-1", viewModel.uiState.value.event?.eventId)
  }

  /** Breaks if the deletion error cannot be dismissed or survives a new deletion request. */
  @Test
  fun deleteErrorIsClearedByDismissingOrRequestingAgain() = runTest {
    val (viewModel, _) = deletableViewModel(failure = IllegalStateException("delete failed"))
    viewModel.requestDelete()
    viewModel.confirmDelete {}
    advanceUntilIdle()

    viewModel.clearDeleteError()
    assertNull(viewModel.uiState.value.deleteErrorMsg)

    viewModel.requestDelete()
    viewModel.confirmDelete {}
    advanceUntilIdle()
    viewModel.requestDelete()
    assertNull(viewModel.uiState.value.deleteErrorMsg)
  }

  /** Breaks if a cancelled deletion is reported as an error or still leaves the screen. */
  @Test
  fun cancelledDeleteIsNotReportedAsErrorAndDoesNotLeave() = runTest {
    val (viewModel, _) = deletableViewModel(failure = CancellationException("cancelled"))
    var left = false
    viewModel.requestDelete()

    viewModel.confirmDelete { left = true }
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.deleteErrorMsg)
    assertFalse(viewModel.uiState.value.isDeleting)
    assertFalse(left)
  }

  /** Breaks if asking to delete before the event is loaded opens a confirmation later. */
  @Test
  fun requestDeleteBeforeTheEventIsLoadedIsIgnored() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
        )

    viewModel.requestDelete()
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.deleteRequested)
  }

  /** Breaks if reloading the event (e.g. after a rotation) cancels the pending confirmation. */
  @Test
  fun fetchEventKeepsThePendingConfirmation() = runTest {
    val (viewModel, _) = deletableViewModel()
    viewModel.requestDelete()

    viewModel.fetchEvent()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.deleteRequested)
  }

  /**
   * Breaks if a request made while the deletion runs reopens the confirmation, or if confirming
   * again deletes the event a second time.
   */
  @Test
  fun requestAndConfirmWhileDeletingDeleteOnlyOnce() = runTest {
    val gate = CompletableDeferred<Unit>()
    val (viewModel, deleted) = deletableViewModel(gate = gate)
    viewModel.requestDelete()
    viewModel.confirmDelete {}
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isDeleting)

    viewModel.requestDelete()
    viewModel.confirmDelete {}
    gate.complete(Unit)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.deleteRequested)
    assertEquals(listOf("event-1"), deleted)
  }

  /**
   * Breaks if leaving the screen while the deletion runs interrupts it half-way, or still calls the
   * navigation callback of the screen that is gone.
   */
  @Test
  fun leavingTheScreenDoesNotInterruptTheDeletion() = runTest {
    val gate = CompletableDeferred<Unit>()
    val (viewModel, deleted) = deletableViewModel(gate = gate)
    var left = false
    viewModel.requestDelete()
    viewModel.confirmDelete { left = true }
    advanceUntilIdle()

    viewModel.viewModelScope.cancel()
    gate.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf("event-1"), deleted)
    assertFalse(left)
  }

  /**
   * Breaks if a confirmed deletion bypasses
   * [com.android.festivar.model.datarepository.DataRepository.deleteEvent] and leaves the event's
   * tasks or the member's links behind.
   */
  @Test
  fun confirmDeleteRemovesTheEventItsTasksAndTheMemberLinks() = runTest {
    val event = testEvent("event-1")
    val task = Task(taskId = "task-1", eventId = "event-1", title = "Task", ownerId = "owner")
    val events = EventsRepositoryLocal(listOf(event))
    val tasks = TasksRepositoryLocal().also { it.addTask(task) }
    val users =
        UsersRepositoryLocal(
            listOf(
                User(
                    uid = "owner",
                    joinedEvents = listOf("event-1"),
                    tasksAssigned = listOf("task-1"),
                )
            )
        )
    val viewModel =
        EventDetailsViewModel(
            DataRepositoryLocal(FakeAuthRepository(), tasks, events, users),
            "event-1",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()
    viewModel.requestDelete()

    viewModel.confirmDelete {}
    advanceUntilIdle()

    assertTrue(events.getAllEvents().isEmpty())
    assertTrue(tasks.getAllTasks("event-1").isEmpty())
    assertEquals(User(uid = "owner"), users.getUser("owner"))
  }

  /**
   * The ViewModel of the loaded "event-1" and the ids its repository was asked to delete. The
   * deletion waits for [gate], throws [failure] if given, and only then records the id.
   */
  private fun TestScope.deletableViewModel(
      failure: Exception? = null,
      gate: CompletableDeferred<Unit>? = null,
  ): Pair<EventDetailsViewModel, MutableList<String>> {
    val event = testEvent("event-1")
    val deleted = mutableListOf<String>()
    val repository =
        object : EventsRepository by EventsRepositoryLocal(listOf(event)) {
          override suspend fun deleteEvent(eventId: String) {
            gate?.await()
            if (failure != null) throw failure
            deleted += eventId
          }
        }
    val viewModel = EventDetailsViewModel(dataRepositoryWith(repository), event.eventId)
    viewModel.fetchEvent()
    advanceUntilIdle()
    return viewModel to deleted
  }

  private fun dataRepositoryWith(eventsRepository: EventsRepository) =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
          usersRepository =
              UsersRepositoryLocal(listOf(User(uid = "owner", joinedEvents = listOf("event-1")))),
      )

  private fun testEvent(eventId: String): Event {
    val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    return Event(
        eventId = eventId,
        ownerId = "owner",
        title = "Test event",
        startDate = start,
        endDate = start.plusHours(2),
    )
  }
}
