package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import java.time.ZoneId
import java.time.ZonedDateTime
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

  @Test
  fun fetchEventLoadsEventWithRequestedId() = runTest {
    val event = testEvent("event-1")
    val viewModel = EventDetailsViewModel(EventsRepositoryLocal(listOf(event)))

    viewModel.fetchEvent(event.eventId)
    advanceUntilIdle()

    assertEquals(event, viewModel.uiState.value.event)
  }

  @Test
  fun fetchEventSilentlyKeepsEventNullWhenRepositoryFails() = runTest {
    val viewModel =
        EventDetailsViewModel(
            object : EventsRepository by EventsRepositoryLocal() {
              override suspend fun getEvent(eventId: String): Event {
                throw IllegalStateException("fetch failed")
              }
            }
        )

    viewModel.fetchEvent("missing")
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.event)
    assertEquals(true, viewModel.uiState.value.errorFetch)
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
