// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.feed

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementFeedViewModelTest {
  private val dispatcher = UnconfinedTestDispatcher()
  private val zone = ZoneId.of("Europe/Zurich")
  private val clock = Clock.fixed(Instant.parse("2026-10-17T10:00:00Z"), zone)

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun refreshSeparatesPinnedAndGroupsAnnouncementsByDateNewestFirst() = runTest {
    val olderToday = announcement("today-old", at(9))
    val newerToday = announcement("today-new", at(11))
    val yesterday = announcement("yesterday", at(16).minusDays(1))
    val pinned = announcement("pinned", at(8), pinned = true)
    val viewModel =
        AnnouncementFeedViewModel(
            EVENT_ID,
            EventsRepositoryLocal(listOf(event(pinned, olderToday, yesterday, newerToday))),
            clock,
        )

    viewModel.refresh()

    assertEquals(listOf(pinned), viewModel.uiState.value.pinnedAnnouncements)
    assertEquals(
        listOf(today, today.minusDays(1)),
        viewModel.uiState.value.sections.map { it.date },
    )
    assertEquals(
        listOf(newerToday, olderToday),
        viewModel.uiState.value.sections.first().announcements,
    )
    assertEquals(listOf(yesterday), viewModel.uiState.value.sections.last().announcements)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun refreshFailureKeepsLastFeedAndShowsError() = runTest {
    val announcement = announcement("first", at(9))
    var shouldFail = false
    val event = event(announcement)
    val repository =
        object : EventsRepository by EventsRepositoryLocal(listOf(event)) {
          override suspend fun getEvent(eventId: String): Event {
            if (shouldFail) throw IllegalStateException("fetch failed")
            return event
          }
        }
    val viewModel = AnnouncementFeedViewModel(EVENT_ID, repository, clock)

    viewModel.refresh()
    shouldFail = true
    viewModel.refresh()

    assertEquals(listOf(announcement), viewModel.uiState.value.sections.single().announcements)
    assertEquals("fetch failed", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun cancellationDoesNotBecomeFeedError() = runTest {
    val repository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun getEvent(eventId: String): Event {
            throw CancellationException("fetch cancelled")
          }
        }
    val viewModel = AnnouncementFeedViewModel(EVENT_ID, repository, clock)

    viewModel.refresh()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  private fun event(vararg announcements: Announcement): Event =
      Event(
          eventId = EVENT_ID,
          title = "Festival",
          startDate = at(8),
          endDate = at(18),
          announcements = announcements.toList(),
      )

  private fun announcement(
      id: String,
      updatedAt: ZonedDateTime,
      pinned: Boolean = false,
  ) =
      Announcement(
          announcementId = id,
          title = id,
          authorId = "organizer",
          updatedAt = updatedAt,
          pinned = pinned,
      )

  private fun at(hour: Int): ZonedDateTime = today.atTime(hour, 0).atZone(zone)

  private companion object {
    const val EVENT_ID = "event-1"
    val today = java.time.LocalDate.of(2026, 10, 17)
  }
}
