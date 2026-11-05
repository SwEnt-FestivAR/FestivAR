package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    val viewModel = EventDetailsViewModel(EventsRepositoryLocal(listOf(event)), event.eventId)
    viewModel.fetchEvent()

    advanceUntilIdle()

    assertEquals(event, viewModel.uiState.value.event)
  }

  /** Red if a repository failure does not expose the event fetch error resource. */
  @Test
  fun fetchEventReportsErrorWhenRepositoryFails() = runTest {
    val viewModel =
        EventDetailsViewModel(
            object : EventsRepository by EventsRepositoryLocal() {
              override suspend fun getEvent(eventId: String): Event {
                throw IllegalStateException("fetch failed")
              }
            },
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
    val viewModel = EventDetailsViewModel(repository, event.eventId)

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
    val viewModel = EventDetailsViewModel(repository, event.eventId)
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
            object : EventsRepository by EventsRepositoryLocal() {
              override suspend fun getEvent(eventId: String): Event {
                throw CancellationException("fetch cancelled")
              }
            },
            eventId = "event-1",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.event)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  private fun testEvent(eventId: String): Event {
    val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    return Event(
        eventId = eventId,
        title = "Test event",
        startDate = start,
        endDate = start.plusHours(2),
    )
  }
}
