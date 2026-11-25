// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
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
  private val clock = Clock.fixed(at(12).toInstant(), zone)

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

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // Breaks if the first state can flash the empty screen, has an error, or has already fetched.
  @Test
  fun initialStateIsLoadingWithoutErrorOrData() {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)

    val state = viewModel.uiState.value
    assertTrue(state.isLoading)
    assertNull(state.errorMsg)
    assertTrue(state.hasNoAnnouncements)
    assertEquals(counts(urgent = 0, warning = 0, info = 0, all = 0), state.counts)
    assertEquals(0, repository.getEventCalls)
  }

  // Breaks if the pinned items, sections, their order, the counts or the end of loading are off.
  @Test
  fun refreshSeparatesPinnedAndGroupsAnnouncementsByDateNewestFirst() = runTest {
    val viewModel = feedViewModel()

    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.ALL, state.selectedFilter)
    assertEquals(listOf(pinnedInfo), state.pinnedAnnouncements)
    assertEquals(figmaSections, state.sections)
    assertEquals(listOf(pinnedInfo), state.shownPinnedAnnouncements)
    assertEquals(figmaSections, state.shownSections)
    assertEquals(figmaCounts, state.counts)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)

    val olderPinned = announcement("pinned-old", at(8), pinned = true)
    val newerPinned = announcement("pinned-new", at(10), pinned = true)
    assertEquals(
        listOf(newerPinned, olderPinned),
        loadedState(olderPinned, newerPinned).pinnedAnnouncements,
    )
  }

  // Breaks if a failed refresh loses the feed, filter or counts, or a new filter clears the error.
  @Test
  fun failedRefreshKeepsTheLastFeedAndTheErrorStaysAcrossFilters() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    viewModel.selectFilter(AnnouncementFilter.WARNING)

    repository.failure = IllegalStateException("fetch failed")
    viewModel.refreshUIState()

    val failed = viewModel.uiState.value
    assertEquals(AnnouncementFilter.WARNING, failed.selectedFilter)
    assertTrue(failed.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(warningToday))), failed.shownSections)
    assertEquals(figmaCounts, failed.counts)
    assertEquals(R.string.announcement_feed_load_failed, failed.errorMsg)
    assertFalse(failed.isLoading)

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    val refiltered = viewModel.uiState.value
    assertEquals(
        listOf(AnnouncementFeedSection(today, listOf(urgentToday))),
        refiltered.shownSections,
    )
    assertEquals(R.string.announcement_feed_load_failed, refiltered.errorMsg)
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
    val viewModel = feedViewModel(repository)

    viewModel.refreshUIState()
    assertEquals(R.string.announcement_feed_load_failed, viewModel.uiState.value.errorMsg)

    shouldWait = true
    viewModel.refreshUIState()

    assertTrue(viewModel.uiState.value.isLoading)
    assertEquals(R.string.announcement_feed_load_failed, viewModel.uiState.value.errorMsg)
    pendingEvent.complete(event())
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
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
    assertEquals(figmaSections, state.shownSections)
  }

  // Breaks if updatedAt keeps the repository's zone, or if the conversion moves the instant.
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

  // Breaks if a filter misses a match, keeps an empty section, or changes counts or stored feed.
  @Test
  fun selectingAFilterShowsOnlyMatchesAndKeepsCountsAndTheFullFeed() = runTest {
    val viewModel = feedViewModel()
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.INFO)

    val info = viewModel.uiState.value
    assertEquals(AnnouncementFilter.INFO, info.selectedFilter)
    assertEquals(listOf(pinnedInfo), info.shownPinnedAnnouncements)
    assertEquals(
        listOf(
            AnnouncementFeedSection(today, listOf(infoToday)),
            AnnouncementFeedSection(
                today.minusDays(1),
                listOf(infoYesterdayLate, infoYesterdayEarly),
            ),
        ),
        info.shownSections,
    )
    assertEquals(figmaCounts, info.counts)

    viewModel.selectFilter(AnnouncementFilter.URGENT)

    val urgent = viewModel.uiState.value
    assertEquals(AnnouncementFilter.URGENT, urgent.selectedFilter)
    assertTrue(urgent.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(urgentToday))), urgent.shownSections)
    assertEquals(figmaCounts, urgent.counts)
    assertEquals(listOf(pinnedInfo), urgent.pinnedAnnouncements)
    assertEquals(figmaSections, urgent.sections)
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

  // Breaks if counts skip a priority, or an empty filter falls back to all or looks like no data.
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

  // Breaks if selecting a filter fetches, ends the loading, or is lost when the data arrives.
  @Test
  fun filterSelectedDuringRefreshAppliesWhenDataArrives() = runTest {
    val repository = GatedEventsRepository()
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()

    viewModel.selectFilter(AnnouncementFilter.WARNING)

    val loading = viewModel.uiState.value
    assertEquals(AnnouncementFilter.WARNING, loading.selectedFilter)
    assertTrue(loading.isLoading)
    assertTrue(loading.shownPinnedAnnouncements.isEmpty())
    assertTrue(loading.shownSections.isEmpty())

    repository.fetches.single().complete(figmaEvent)

    val state = viewModel.uiState.value
    assertEquals(AnnouncementFilter.WARNING, state.selectedFilter)
    assertFalse(state.isLoading)
    assertTrue(state.shownPinnedAnnouncements.isEmpty())
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(warningToday))), state.shownSections)
    assertEquals(figmaCounts, state.counts)
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
    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.hasNoAnnouncements)

    val newInfo = announcement("new-info", at(12))
    newerFetch.complete(event(newInfo))

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(newInfo))), state.sections)
  }

  // Breaks if a cancelled refresh failing first stops the loading or shows its error.
  @Test
  fun aCancelledFetchThatFailsDoesNotTouchTheState() = runTest {
    val repository = GatedEventsRepository(ignoresCancellation = true)
    val viewModel = feedViewModel(repository)
    viewModel.refreshUIState()
    viewModel.refreshUIState()
    val (olderFetch, newerFetch) = repository.fetches

    olderFetch.completeExceptionally(IllegalStateException("stale failure"))

    assertTrue(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)

    newerFetch.complete(event(infoToday))

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(listOf(AnnouncementFeedSection(today, listOf(infoToday))), state.sections)
  }

  // Breaks if a CancellationException of the repository shows an error or blocks the next refresh.
  @Test
  fun aCancellationExceptionFromTheRepositoryIsNotAFeedError() = runTest {
    val repository = ControllableEventsRepository(figmaEvent)
    repository.failure = CancellationException("fetch cancelled")
    val viewModel = feedViewModel(repository)

    viewModel.refreshUIState()

    assertNull(viewModel.uiState.value.errorMsg)

    repository.failure = null
    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(figmaSections, state.sections)
  }

  // Breaks if the empty state only looks at pinned announcements or only at the dated ones.
  @Test
  fun hasNoAnnouncementsOnlyWhenPinnedAndSectionsAreBothEmpty() = runTest {
    assertTrue(loadedState().hasNoAnnouncements)
    assertFalse(loadedState(pinnedInfo).hasNoAnnouncements)
    assertFalse(loadedState(infoToday).hasNoAnnouncements)
  }

  /** Breaks if, before any fetch, the user may not see the feed or already has the add button. */
  @Test
  fun initialPermissionsLetTheUserStayButHideTheAddButton() {
    val state = feedViewModel(ControllableEventsRepository(figmaEvent)).uiState.value

    assertTrue(state.permissions.canSeeAnnouncements)
    assertFalse(state.permissions.canCreateAnnouncement)
  }

  /** Breaks if a role gets the add button without CAN_CREATE_ANNOUNCEMENT, or loses the feed. */
  @Test
  fun eachRoleSeesTheFeedAndOnlyRolesAllowedToCreateGetTheAddButton() = runTest {
    val expectedCanCreate =
        mapOf(
            Role.VISITOR to false,
            Role.STAFF to false,
            Role.TEAM_LEAD to true,
            Role.STAFF_MANAGER to true,
            Role.ADMIN to true,
            Role.OWNER to true,
        )

    expectedCanCreate.forEach { (role, canCreate) ->
      val userId = if (role == Role.OWNER) OWNER_ID else MEMBER_ID
      val event = if (role == Role.OWNER) event(infoToday) else eventWithMember(role, infoToday)
      val viewModel = feedViewModel(EventsRepositoryLocal(listOf(event)), userId = { userId })

      viewModel.refreshUIState()

      val state = viewModel.uiState.value
      assertEquals(
          AnnouncementFeedPermissions(
              canSeeAnnouncements = true,
              canCreateAnnouncement = canCreate,
          ),
          state.permissions,
      )
      assertEquals(listOf(AnnouncementFeedSection(today, listOf(infoToday))), state.sections)
    }
  }

  /** Breaks if a non-member keeps any permission, or if being one is shown as a load error. */
  @Test
  fun aUserWhoIsNotAMemberHasNoPermissionAndNoError() = runTest {
    val viewModel = feedViewModel(userId = { "stranger" })

    viewModel.refreshUIState()

    val state = viewModel.uiState.value
    assertEquals(
        AnnouncementFeedPermissions(canSeeAnnouncements = false, canCreateAnnouncement = false),
        state.permissions,
    )
    assertNull(state.errorMsg)
    assertFalse(state.isLoading)
  }

  /** Breaks if a failed fetch resets the permissions, which would make a member leave the feed. */
  @Test
  fun aFailedFetchKeepsThePreviousPermissions() = runTest {
    val repository = ControllableEventsRepository(eventWithMember(Role.TEAM_LEAD))
    repository.failure = IllegalStateException("fetch failed")
    val viewModel = feedViewModel(repository, userId = { MEMBER_ID })

    viewModel.refreshUIState()

    assertEquals(AnnouncementFeedPermissions(), viewModel.uiState.value.permissions)

    repository.failure = null
    viewModel.refreshUIState()
    repository.failure = IllegalStateException("fetch failed")
    viewModel.refreshUIState()

    assertEquals(
        AnnouncementFeedPermissions(canSeeAnnouncements = true, canCreateAnnouncement = true),
        viewModel.uiState.value.permissions,
    )
    assertEquals(R.string.announcement_feed_load_failed, viewModel.uiState.value.errorMsg)
  }

  /** Breaks if the permissions keep the role of an older fetch, e.g. after a kick or promotion. */
  @Test
  fun eachFetchRecomputesThePermissionsFromTheLatestRoles() = runTest {
    val repository = ControllableEventsRepository(eventWithMember(Role.STAFF))
    val viewModel = feedViewModel(repository, userId = { MEMBER_ID })
    viewModel.refreshUIState()
    assertFalse(viewModel.uiState.value.permissions.canCreateAnnouncement)

    repository.event = eventWithMember(Role.TEAM_LEAD)
    viewModel.refreshUIState()
    assertTrue(viewModel.uiState.value.permissions.canCreateAnnouncement)

    repository.event = event()
    viewModel.refreshUIState()
    assertEquals(
        AnnouncementFeedPermissions(canSeeAnnouncements = false, canCreateAnnouncement = false),
        viewModel.uiState.value.permissions,
    )
  }

  /** Breaks if the uid is read once when the ViewModel is built instead of at each fetch. */
  @Test
  fun theCurrentUserIsReadAtEachFetch() = runTest {
    var currentUser = OWNER_ID
    val viewModel = feedViewModel(userId = { currentUser })
    viewModel.refreshUIState()
    assertTrue(viewModel.uiState.value.permissions.canCreateAnnouncement)

    currentUser = "stranger"
    viewModel.refreshUIState()

    assertFalse(viewModel.uiState.value.permissions.canSeeAnnouncements)
  }

  /** Breaks if a cancelled fetch ending first sets its stale permissions, e.g. sending back. */
  @Test
  fun aCancelledFetchDoesNotChangeThePermissions() = runTest {
    val repository = GatedEventsRepository(ignoresCancellation = true)
    val viewModel = feedViewModel(repository, userId = { MEMBER_ID })
    viewModel.refreshUIState()
    viewModel.refreshUIState()
    val (olderFetch, newerFetch) = repository.fetches

    olderFetch.complete(event())

    assertEquals(AnnouncementFeedPermissions(), viewModel.uiState.value.permissions)

    newerFetch.complete(eventWithMember(Role.TEAM_LEAD))

    assertEquals(
        AnnouncementFeedPermissions(canSeeAnnouncements = true, canCreateAnnouncement = true),
        viewModel.uiState.value.permissions,
    )
  }

  private fun feedViewModel(
      repository: EventsRepository = EventsRepositoryLocal(listOf(figmaEvent)),
      userId: () -> String = { OWNER_ID },
  ) =
      AnnouncementFeedViewModel(
          EVENT_ID,
          DataRepositoryLocal(FakeAuthRepository(), eventsRepository = repository),
          currentUserId = userId,
          clock = clock,
      )

  /** An event of [announcements] whose members are the owner and [MEMBER_ID], who has [role]. */
  private fun eventWithMember(role: Role, vararg announcements: Announcement): Event =
      event(*announcements)
          .copy(
              members = listOf(User(OWNER_ID), User(MEMBER_ID)),
              roles = mapOf(OWNER_ID to Role.OWNER, MEMBER_ID to role),
          )

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
          ownerId = OWNER_ID,
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
    const val OWNER_ID = "owner"
    const val MEMBER_ID = "member"
    val today = java.time.LocalDate.of(2026, 10, 17)
  }
}
