// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class AnnouncementFeedViewModelTest {
  private val dispatcher = UnconfinedTestDispatcher()
  private val zone = ZoneId.of("Europe/Zurich")
  private val now = at(12)

  // The feed of the Figma frames 2979:6369 and 3237:7284.
  private val pinnedInfo = announcement("pinned-info", at(9).minusDays(1), pinned = true)
  private val urgentToday =
      announcement("urgent-today", at(14, 2), priority = AnnouncementPriority.URGENT)
  private val warningToday =
      announcement("warning-today", at(11, 30), priority = AnnouncementPriority.WARNING)
  private val infoToday = announcement("info-today", at(9, 15))
  private val infoYesterdayLate = announcement("info-yesterday-late", at(16, 5).minusDays(1))
  private val infoYesterdayEarly = announcement("info-yesterday-early", at(10).minusDays(1))
  private val figmaEvent =
      event(
          infoYesterdayEarly,
          infoToday,
          pinnedInfo,
          urgentToday,
          infoYesterdayLate,
          warningToday,
      )
  private val figmaSections =
      listOf(
          AnnouncementFeedSection(today, listOf(urgentToday, warningToday, infoToday)),
          AnnouncementFeedSection(
              today.minusDays(1),
              listOf(infoYesterdayLate, infoYesterdayEarly),
          ),
      )
  private val figmaCounts = counts(urgent = 1, warning = 1, info = 4, all = 6)
  private val zeroCounts = counts(urgent = 0, warning = 0, info = 0, all = 0)

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
            now = { now },
        )

    viewModel.refreshUIState()

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
    val viewModel = AnnouncementFeedViewModel(EVENT_ID, repository, now = { now })

    viewModel.refreshUIState()
    shouldFail = true
    viewModel.refreshUIState()

    assertEquals(listOf(announcement), viewModel.uiState.value.sections.single().announcements)
    assertTrue(viewModel.uiState.value.hasError)
    assertEquals("fetch failed", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  // Breaks if a retry clears the error when it starts, so the screen stops showing it.
  @Test
  fun errorStaysWhileRetrying() = runTest {
    val pendingEvent = CompletableDeferred<Event>()
    var shouldWait = false
    val repository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun getEvent(eventId: String): Event {
            if (shouldWait) return pendingEvent.await()
            throw IllegalStateException("fetch failed")
          }
        }
    val viewModel = AnnouncementFeedViewModel(EVENT_ID, repository, now = { now })

    viewModel.refreshUIState()
    assertEquals("fetch failed", viewModel.uiState.value.errorMsg)

    shouldWait = true
    viewModel.refreshUIState()

    assertTrue(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.hasError)
    assertEquals("fetch failed", viewModel.uiState.value.errorMsg)
    pendingEvent.complete(event())
  }

  // Breaks if the cancellation of a refresh by a newer one is treated as a failed fetch.
  @Test
  fun cancelledFetchDoesNotShowAnError() = runTest {
    val repository = GatedEventsRepository()
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    viewModel.refreshUIState()

    assertFalse(viewModel.uiState.value.hasError)
    assertTrue(viewModel.uiState.value.isLoading)

    repository.fetches.last().complete(figmaEvent)

    assertFalse(viewModel.uiState.value.hasError)
    assertEquals(figmaSections, viewModel.uiState.value.sections)
  }

  // Breaks if the feed keeps the zone the repository sent, so a screen formatting updatedAt shows
  // the wrong time, or if the conversion moves the instant.
  @Test
  fun announcementsAreConvertedToTheZoneOfNow() = runTest {
    val updatedAtInUtc = today.minusDays(1).atTime(23, 30).atZone(ZoneOffset.UTC)

    val state =
        loadedState(
            announcement("dated", updatedAtInUtc),
            announcement("pinned", updatedAtInUtc, pinned = true),
        )

    val section = state.sections.single()
    assertEquals(today, section.date)
    listOf(section.announcements.single(), state.pinnedAnnouncements.single()).forEach {
      assertEquals(zone, it.updatedAt.zone)
      assertEquals(LocalTime.of(1, 30), it.updatedAt.toLocalTime())
      assertEquals(updatedAtInUtc.toInstant(), it.updatedAt.toInstant())
    }
  }

  // Breaks if the default time is not the system's, so the feed shows dates in another zone.
  @Test
  fun defaultNowUsesTheSystemZone() = runTest {
    val updatedAt = at(12)
    val viewModel =
        AnnouncementFeedViewModel(
            EVENT_ID,
            EventsRepositoryLocal(listOf(event(announcement("dated", updatedAt)))),
        )

    viewModel.refreshUIState()

    val shown = viewModel.uiState.value.sections.single().announcements.single()
    assertEquals(ZoneId.systemDefault(), shown.updatedAt.zone)
    assertEquals(updatedAt.toInstant(), shown.updatedAt.toInstant())
  }

  // Breaks if the default filter hides anything, or if pinned announcements are left out of counts.
  @Test
  fun refreshWithoutFilterShowsEverythingAndCountsEachFilter() = runTest {
    val viewModel = feedViewModel()

    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.ALL, state.selectedFilter)
    assertEquals(listOf(pinnedInfo), state.shownPinnedAnnouncements)
    assertEquals(figmaSections, state.shownSections)
    assertEquals(figmaCounts, state.counts)
  }

  // Breaks if the filter skips pinned announcements or a section, loses the newest-first order, or
  // if the counts only cover what the filter shows.
  @Test
  fun selectingAFilterShowsOnlyMatchingAnnouncementsAndKeepsCounts() = runTest {
    val viewModel = feedViewModel()
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.INFO)

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.INFO, state.selectedFilter)
    assertEquals(listOf(pinnedInfo), state.shownPinnedAnnouncements)
    assertEquals(
        listOf(
            AnnouncementFeedSection(today, listOf(infoToday)),
            AnnouncementFeedSection(
                today.minusDays(1),
                listOf(infoYesterdayLate, infoYesterdayEarly),
            ),
        ),
        state.shownSections,
    )
    assertEquals(figmaCounts, state.counts)
  }

  // Breaks if a section stays visible with no announcements once the filter empties it.
  @Test
  fun selectingAFilterHidesPinnedAndSectionsWithoutAMatch() = runTest {
    val viewModel = feedViewModel()
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    val state = viewModel.uiState.value
    assertTrue(state.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(urgentToday))), state.shownSections)
  }

  // Breaks if the filter is applied to the stored feed instead of only to what is shown.
  @Test
  fun selectingAFilterLeavesPinnedAnnouncementsAndSectionsComplete() = runTest {
    val viewModel = feedViewModel()
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    val state = viewModel.uiState.value
    assertEquals(listOf(pinnedInfo), state.pinnedAnnouncements)
    assertEquals(figmaSections, state.sections)
  }

  // Breaks if going back to ALL keeps the old filter, or if changing the filter fetches again.
  @Test
  fun selectingAllRestoresTheFeedWithoutFetchingAgain() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.WARNING)
    viewModel.selectFilter(AnnouncementFilter.ALL)

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.ALL, state.selectedFilter)
    assertEquals(listOf(pinnedInfo), state.shownPinnedAnnouncements)
    assertEquals(figmaSections, state.shownSections)
    assertEquals(1, repository.getEventCalls)
  }

  // Breaks if a refresh resets the filter, or if the feed or the counts keep the old data.
  @Test
  fun refreshWhileFilteredKeepsFilterAndUsesNewData() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    viewModel.selectFilter(AnnouncementFilter.INFO)

    val newInfo = announcement("new-info", at(12))
    val newUrgent = announcement("new-urgent", at(13), priority = AnnouncementPriority.URGENT)
    repository.event = event(newInfo, newUrgent)
    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.INFO, state.selectedFilter)
    assertTrue(state.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(newInfo))), state.shownSections)
    assertEquals(counts(urgent = 1, warning = 0, info = 1, all = 2), state.counts)
  }

  // Breaks if selecting a filter stops the running refresh, or if its data shows unfiltered.
  @Test
  fun filterSelectedDuringRefreshAppliesWhenDataArrives() = runTest {
    val pendingEvent = CompletableDeferred<Event>()
    val repository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun getEvent(eventId: String): Event = pendingEvent.await()
        }
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.WARNING)

    assertEquals(AnnouncementFilter.WARNING, viewModel.uiState.value.selectedFilter)
    assertTrue(viewModel.uiState.value.isLoading)

    pendingEvent.complete(figmaEvent)

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.WARNING, state.selectedFilter)
    assertFalse(state.isLoading)
    assertTrue(state.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(warningToday))), state.shownSections)
    assertEquals(figmaCounts, state.counts)
  }

  // Breaks if a failed refresh clears the filter or the counts, or shows the unfiltered feed.
  @Test
  fun failedRefreshWhileFilteredKeepsFilteredFeedAndCounts() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    viewModel.selectFilter(AnnouncementFilter.WARNING)

    repository.failure = IllegalStateException("fetch failed")
    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.WARNING, state.selectedFilter)
    assertTrue(state.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(warningToday))), state.shownSections)
    assertEquals(figmaCounts, state.counts)
    assertEquals("fetch failed", state.errorMsg)
    assertFalse(state.isLoading)
  }

  // Breaks if the counts start empty instead of listing every filter at 0.
  @Test
  fun everyFilterCountsZeroBeforeAnyRefresh() {
    val viewModel = feedViewModel()

    assertEquals(zeroCounts, viewModel.uiState.value.counts)
  }

  // Breaks if counts only list priorities that occur, if an unmatched filter falls back to all, or
  // if the screen cannot tell an empty filter from an event without announcements.
  @Test
  fun filterWithoutAnnouncementsCountsZeroAndShowsAnEmptyFeed() = runTest {
    val viewModel = feedViewModel(EventsRepositoryLocal(listOf(event(pinnedInfo, infoToday))))
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    val state = viewModel.uiState.value
    assertEquals(counts(urgent = 0, warning = 0, info = 2, all = 2), state.counts)
    assertTrue(state.shownPinnedAnnouncements.isEmpty())
    assertTrue(state.shownSections.isEmpty())
    assertFalse(state.hasNoAnnouncements)
  }

  // Breaks if selecting a filter fetches or ends the first loading, or if a filter picked with no
  // data yet is ignored by the first refresh.
  @Test
  fun filterSelectedBeforeAnyRefreshStaysEmptyThenAppliesToFirstRefresh() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    val before = viewModel.uiState.value
    assertTrue(before.shownPinnedAnnouncements.isEmpty())
    assertTrue(before.shownSections.isEmpty())
    assertTrue(before.isLoading)
    assertEquals(0, repository.getEventCalls)

    viewModel.refreshUIState()

    val after = viewModel.uiState.value
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(urgentToday))), after.shownSections)
    assertEquals(figmaCounts, after.counts)
  }

  // Breaks if changing the filter clears the error of the last refresh.
  @Test
  fun selectingAFilterAfterAFailedRefreshKeepsTheError() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    repository.failure = IllegalStateException("fetch failed")
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    assertEquals(AnnouncementFilter.URGENT, viewModel.uiState.value.selectedFilter)
    assertEquals("fetch failed", viewModel.uiState.value.errorMsg)
  }

  // Breaks if a cancelled refresh whose repository ignored cancellation overwrites newer data.
  @Test
  fun cancelledRefreshReturningLateDoesNotReplaceNewerData() = runTest {
    val staleResult = CompletableDeferred<Event>()
    val newInfo = announcement("new-info", at(12))
    var fetches = 0
    var staleFetchReturned = false
    val repository =
        object : EventsRepository by EventsRepositoryLocal() {
          override suspend fun getEvent(eventId: String): Event {
            if (++fetches > 1) return event(newInfo)
            val stale = withContext(NonCancellable) { staleResult.await() }
            staleFetchReturned = true
            return stale
          }
        }
    val viewModel = feedViewModel(repository)

    viewModel.refreshUIState()
    viewModel.refreshUIState()
    staleResult.complete(event(infoToday))

    assertTrue(staleFetchReturned)
    assertEquals(
        listOf(AnnouncementFeedSection(today, listOf(newInfo))),
        viewModel.uiState.value.sections,
    )
  }

  // Breaks if the first fetch can flash the empty state, or if the ViewModel fetches by itself.
  @Test
  fun isLoadingWithoutErrorBeforeAnyRefresh() {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)

    val state = viewModel.uiState.value
    assertTrue(state.isLoading)
    assertFalse(state.hasError)
    assertNull(state.errorMsg)
    assertEquals(0, repository.getEventCalls)
  }

  // Breaks if an exception without a message hides the failure, or gets a made-up message.
  @Test
  fun failureWithoutAMessageSetsHasErrorAndNoErrorMsg() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    repository.failure = IllegalStateException()
    val viewModel = feedViewModel(repository)

    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertTrue(state.hasError)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  // Breaks if a successful refresh leaves the error of the failed one, or keeps loading.
  @Test
  fun refreshAfterFailureShowsTheNewFeedWithoutError() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    repository.failure = IllegalStateException("fetch failed")
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()

    repository.failure = null
    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertFalse(state.hasError)
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    assertEquals(figmaSections, state.shownSections)
  }

  // Breaks if a cancelled refresh ending first stops the loading or shows its data.
  @Test
  fun isLoadingUntilTheLatestRefreshEnds() = runTest {
    val repository = GatedEventsRepository(ignoresCancellation = true)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    viewModel.refreshUIState()
    val (olderFetch, newerFetch) = repository.fetches

    olderFetch.complete(event(infoToday))

    assertTrue(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.hasError)
    assertTrue(viewModel.uiState.value.hasNoAnnouncements)

    val newInfo = announcement("new-info", at(12))
    newerFetch.complete(event(newInfo))

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertFalse(state.hasError)
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(newInfo))), state.sections)
  }

  // Breaks if the empty state only looks at pinned announcements or only at the dated ones.
  @Test
  fun hasNoAnnouncementsOnlyWhenPinnedAndSectionsAreBothEmpty() = runTest {
    assertTrue(feedViewModel().uiState.value.hasNoAnnouncements)
    assertTrue(loadedState().hasNoAnnouncements)
    assertFalse(loadedState(pinnedInfo).hasNoAnnouncements)
    assertFalse(loadedState(infoToday).hasNoAnnouncements)
  }

  private fun feedViewModel(
      repository: EventsRepository = EventsRepositoryLocal(listOf(figmaEvent))
  ) = AnnouncementFeedViewModel(EVENT_ID, repository, now = { now })

  private fun loadedState(vararg announcements: Announcement): AnnouncementFeedUIState {
    val viewModel = feedViewModel(EventsRepositoryLocal(listOf(event(*announcements))))
    viewModel.refreshUIState()
    return viewModel.uiState.value
  }

  private fun counts(urgent: Int, warning: Int, info: Int, all: Int) =
      mapOf(
          AnnouncementFilter.URGENT to urgent,
          AnnouncementFilter.WARNING to warning,
          AnnouncementFilter.INFO to info,
          AnnouncementFilter.ALL to all,
      )

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
      priority: AnnouncementPriority = AnnouncementPriority.INFO,
  ) =
      Announcement(
          announcementId = id,
          title = id,
          authorId = "organizer",
          priority = priority,
          updatedAt = updatedAt,
          pinned = pinned,
      )

  private fun at(hour: Int, minute: Int = 0): ZonedDateTime =
      today.atTime(hour, minute).atZone(zone)

  /** Serves [event] through the local repository, or throws [failure], and counts the fetches. */
  private class ControllableEventsRepository(var event: Event) :
      EventsRepository by EventsRepositoryLocal(listOf(event)) {
    var getEventCalls = 0
    var failure: Exception? = null

    override suspend fun getEvent(eventId: String): Event {
      getEventCalls++
      failure?.let { throw it }
      return event
    }
  }

  /** Each fetch waits for its entry in [fetches], and may still end if it was cancelled. */
  private class GatedEventsRepository(private val ignoresCancellation: Boolean = false) :
      EventsRepository by EventsRepositoryLocal() {
    val fetches = mutableListOf<CompletableDeferred<Event>>()

    override suspend fun getEvent(eventId: String): Event {
      val fetch = CompletableDeferred<Event>().also { fetches += it }
      return if (ignoresCancellation) withContext(NonCancellable) { fetch.await() }
      else fetch.await()
    }
  }

  private companion object {
    const val EVENT_ID = "event-1"
    val today = java.time.LocalDate.of(2026, 10, 17)
  }
}
