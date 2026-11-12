// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.announcement.details

import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import java.io.IOException
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementDetailsViewModelTest {
  private val start = ZonedDateTime.parse("2026-10-17T14:02:00+02:00")
  private val announcement =
      Announcement(
          announcementId = "announcement-1",
          title = "Stage B is closed",
          authorId = "author-1",
          content = "Strong wind on the hill.",
          priority = AnnouncementPriority.URGENT,
          updatedAt = start,
          links = listOf("https://vidyfest.ch/map", "https://vidyfest.ch/programme"),
      )
  private val event =
      Event(
          eventId = "event-1",
          title = "Vidyfest",
          startDate = start.minusHours(1),
          endDate = start.plusHours(2),
          announcements = listOf(announcement),
          ownerId = "sneaky owner",
      )
  private val localEvents = EventsRepositoryLocal(listOf(event))
  private val events = ScriptedEventsRepository(localEvents)
  private val users =
      UsersRepositoryLocal(listOf(User(uid = "author-1", name = "Marie", surname = "Dupont")))

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun dataRepository(
      eventsRepository: EventsRepository = events,
      usersRepository: UsersRepository = users,
  ): DataRepository =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
          usersRepository = usersRepository,
      )

  private fun viewModel() =
      AnnouncementDetailsViewModel("event-1", "announcement-1", dataRepository())

  @Test
  fun loadProvidesAnnouncementAuthorAndEventEndTime() {
    val vm = viewModel()

    assertEquals(0, events.getCalls)
    assertTrue(vm.uiState.value.isLoading)
    assertFalse(vm.uiState.value.canTogglePin)

    vm.load()

    val state = vm.uiState.value
    assertEquals(announcement, state.announcement)
    assertEquals("Marie Dupont", state.authorName)
    assertEquals(event.endDate, state.eventEndTime)
    assertEquals(AnnouncementPriority.URGENT, state.announcement?.priority)
    assertEquals("Strong wind on the hill.", state.announcement?.content)
    assertEquals(announcement.links, state.announcement?.links)
    assertFalse(state.isLoading)
    assertTrue(state.canTogglePin)
    assertNull(state.errorMsg)
  }

  @Test
  fun loadFailureCanBeRetried() {
    events.getFailure = IOException("offline")
    val vm = viewModel()

    vm.load()

    assertNull(vm.uiState.value.announcement)
    assertEquals(R.string.announcement_details_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)

    events.getFailure = null
    vm.load()

    assertEquals(announcement, vm.uiState.value.announcement)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun loadIgnoresRepeatedCallsAndToggleBeforeAnnouncementLoads() {
    val gate = CompletableDeferred<Unit>()
    events.beforeGet = { gate.await() }
    val vm = viewModel()

    vm.togglePin()
    vm.load()
    vm.load()

    assertEquals(1, events.getCalls)
    assertEquals(0, events.editCalls)
    assertTrue(vm.uiState.value.isLoading)

    gate.complete(Unit)

    assertEquals(announcement, vm.uiState.value.announcement)
  }

  @Test
  fun loadReportsMissingAnnouncement() {
    val repository =
        object : EventsRepository by localEvents {
          override suspend fun getEvent(eventId: String) = event.copy(announcements = emptyList())
        }
    val vm = AnnouncementDetailsViewModel("event-1", "announcement-1", dataRepository(repository))

    vm.load()

    assertNull(vm.uiState.value.announcement)
    assertEquals(R.string.announcement_details_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun loadDoesNotConvertCancellationIntoAnErrorAndCanRetry() {
    events.getFailure = CancellationException("fetch cancelled")
    val vm = viewModel()

    vm.load()

    assertNull(vm.uiState.value.errorMsg)

    events.getFailure = null
    vm.load()

    assertEquals(announcement, vm.uiState.value.announcement)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun loadJoinsOnlyNonBlankAuthorNameParts() {
    val blankSurnameUsers = UsersRepositoryLocal(listOf(User(uid = "author-1", name = "Marie")))
    val vm =
        AnnouncementDetailsViewModel(
            "event-1",
            "announcement-1",
            dataRepository(usersRepository = blankSurnameUsers),
        )

    vm.load()

    assertEquals("Marie", vm.uiState.value.authorName)
  }

  @Test
  fun loadShowsAnnouncementWhenAuthorNoLongerExists() {
    val deletedAuthorRepository =
        object : UsersRepository by users {
          override suspend fun getUser(uId: String): User {
            throw NoSuchElementException("User $uId not found")
          }
        }
    val vm =
        AnnouncementDetailsViewModel(
            "event-1",
            "announcement-1",
            dataRepository(usersRepository = deletedAuthorRepository),
        )

    vm.load()

    assertEquals(announcement, vm.uiState.value.announcement)
    assertNull(vm.uiState.value.authorName)
    assertEquals(
        R.string.announcement_details_user_fetch_failed,
        vm.uiState.value.authorNameRes,
    )
    assertEquals(event.endDate, vm.uiState.value.eventEndTime)
    assertFalse(vm.uiState.value.isLoading)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun togglePinPersistsAndBlocksConcurrentTaps() {
    val gate = CompletableDeferred<Unit>()
    events.beforeEdit = { gate.await() }
    val vm = viewModel().also { it.load() }

    vm.togglePin()

    assertTrue(vm.uiState.value.isUpdating)
    assertFalse(vm.uiState.value.canTogglePin)
    vm.load()
    vm.togglePin()
    assertEquals(1, events.editCalls)
    assertEquals(1, events.getCalls)

    gate.complete(Unit)

    assertTrue(vm.uiState.value.announcement!!.pinned)
    assertTrue(runBlocking { localEvents.getEvent("event-1").announcements.single().pinned })
    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun togglePinPreservesConcurrentAnnouncementEdits() {
    val vm = viewModel().also { it.load() }
    val concurrentEdit = announcement.copy(title = "Updated elsewhere", content = "New details")
    events.beforeEdit = { localEvents.editAnnouncement("event-1", concurrentEdit) }

    vm.togglePin()

    val stored = runBlocking { localEvents.getEvent("event-1").announcements.single() }
    assertEquals(concurrentEdit.copy(pinned = true), stored)
    assertEquals(stored, vm.uiState.value.announcement)
  }

  @Test
  fun failedPinUpdateKeepsTheAnnouncementAndShowsError() {
    events.editFailure = IOException("offline")
    val vm = viewModel().also { it.load() }

    vm.togglePin()

    assertEquals(announcement, vm.uiState.value.announcement)
    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(R.string.announcement_details_save_failed, vm.uiState.value.errorMsg)

    events.editFailure = null
    vm.togglePin()

    assertTrue(vm.uiState.value.announcement!!.pinned)
    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun cancelledPinUpdateClearsUpdatingWithoutShowingAnError() {
    events.editFailure = CancellationException("update cancelled")
    val vm = viewModel().also { it.load() }

    vm.togglePin()

    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun blankIdsFailFast() {
    assertThrows(IllegalArgumentException::class.java) {
      AnnouncementDetailsViewModel(" ", "announcement-1", dataRepository())
    }
    assertThrows(IllegalArgumentException::class.java) {
      AnnouncementDetailsViewModel("event-1", " ", dataRepository())
    }
  }

  private class ScriptedEventsRepository(private val local: EventsRepositoryLocal) :
      EventsRepository by local {
    var getFailure: Exception? = null
    var editFailure: Exception? = null
    var beforeGet: suspend () -> Unit = {}
    var beforeEdit: suspend () -> Unit = {}
    var getCalls = 0
    var editCalls = 0

    override suspend fun getEvent(eventId: String): Event {
      getCalls++
      beforeGet()
      getFailure?.let { throw it }
      return local.getEvent(eventId)
    }

    override suspend fun setAnnouncementPinned(
        eventId: String,
        announcementId: String,
        pinned: Boolean,
    ): Announcement {
      editCalls++
      beforeEdit()
      editFailure?.let { throw it }
      return local.setAnnouncementPinned(eventId, announcementId, pinned)
    }
  }
}
