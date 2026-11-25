// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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
import com.android.festivar.model.event.role.Role
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

private const val OWNER_ID = "sneaky owner"
private const val OTHER_LEAD_ID = "lead-2"
private const val MANAGER_ID = "manager-1"
private const val STAFF_ID = "staff-1"
private const val VISITOR_ID = "visitor-1"

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
  // One member per role. The author of the announcement is a team lead, and so is OTHER_LEAD_ID,
  // who did not write it.
  private val roles =
      mapOf(
          OWNER_ID to Role.OWNER,
          "author-1" to Role.TEAM_LEAD,
          OTHER_LEAD_ID to Role.TEAM_LEAD,
          MANAGER_ID to Role.STAFF_MANAGER,
          STAFF_ID to Role.STAFF,
          VISITOR_ID to Role.VISITOR,
      )
  private val event =
      Event(
          eventId = "event-1",
          title = "Vidyfest",
          startDate = start.minusHours(1),
          endDate = start.plusHours(2),
          announcements = listOf(announcement),
          ownerId = OWNER_ID,
          members = roles.keys.map { User(uid = it) },
          roles = roles,
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

  /** Builds the ViewModel for the user [userId], the owner by default, who may pin. */
  private fun viewModel(
      userId: () -> String = { OWNER_ID },
      dataRepository: DataRepository = dataRepository(),
  ) = AnnouncementDetailsViewModel("event-1", "announcement-1", userId, dataRepository)

  /** Gives [userId] the role [role] in the stored event, as another member's device would. */
  private fun changeRoleInTheStoredEvent(userId: String, role: Role) = runBlocking {
    val stored = localEvents.getEvent("event-1")
    localEvents.editEvent("event-1", stored.copy(roles = stored.roles + (userId to role)))
  }

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
    val vm = viewModel(dataRepository = dataRepository(repository))

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
    val vm = viewModel(dataRepository = dataRepository(usersRepository = blankSurnameUsers))

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
    val vm = viewModel(dataRepository = dataRepository(usersRepository = deletedAuthorRepository))

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
    // One read for the load, and one for the role check made before saving the pin.
    assertEquals(2, events.getCalls)

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
      AnnouncementDetailsViewModel(" ", "announcement-1", { OWNER_ID }, dataRepository())
    }
    assertThrows(IllegalArgumentException::class.java) {
      AnnouncementDetailsViewModel("event-1", " ", { OWNER_ID }, dataRepository())
    }
  }

  /** Breaks if a role gets a pin, edit or delete right it does not have, or loses one it has. */
  @Test
  fun loadGivesEachRoleItsPermissionsOnTheAnnouncement() {
    // Expected (canPin = canEditAnnouncement, canDeleteAnnouncement) for each member.
    val expected =
        mapOf(
            OWNER_ID to (true to true),
            MANAGER_ID to (true to true),
            "author-1" to (true to true),
            OTHER_LEAD_ID to (false to false),
            STAFF_ID to (false to false),
            VISITOR_ID to (false to false),
        )

    expected.forEach { (userId, rights) ->
      val (canEdit, canDelete) = rights
      val vm = viewModel(userId = { userId }).also { it.load() }

      val permissions = vm.uiState.value.permissions
      assertTrue(userId, permissions.canSeeAnnouncement)
      assertEquals(userId, canEdit, permissions.canPin)
      assertEquals(userId, canEdit, permissions.canEditAnnouncement)
      assertEquals(userId, canDelete, permissions.canDeleteAnnouncement)
      assertEquals(userId, canEdit, vm.uiState.value.canTogglePin)
      assertEquals(userId, announcement, vm.uiState.value.announcement)
    }
  }

  /** Breaks if a user who is not a member of the event is shown the announcement or an error. */
  @Test
  fun loadHidesTheAnnouncementFromANonMember() {
    val vm = viewModel(userId = { "stranger" })

    vm.load()

    val state = vm.uiState.value
    assertFalse(state.permissions.canSeeAnnouncement)
    assertFalse(state.permissions.canPin)
    assertFalse(state.permissions.canEditAnnouncement)
    assertFalse(state.permissions.canDeleteAnnouncement)
    assertNull(state.announcement)
    assertFalse(state.canTogglePin)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  /** Breaks if the uid is read once at creation instead of at each load. */
  @Test
  fun loadReadsTheCurrentUserAgain() {
    var userId = VISITOR_ID
    val vm = viewModel(userId = { userId })
    vm.load()
    assertFalse(vm.uiState.value.permissions.canPin)

    userId = OWNER_ID
    vm.load()

    assertTrue(vm.uiState.value.permissions.canPin)
  }

  /** Breaks if a member whose role may not pin can still save a pin. */
  @Test
  fun togglePinDoesNothingForAMemberWhoCannotPin() {
    val vm = viewModel(userId = { STAFF_ID }).also { it.load() }

    vm.togglePin()

    assertEquals(0, events.editCalls)
    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
    assertFalse(runBlocking { localEvents.getEvent("event-1").announcements.single().pinned })
  }

  /** Breaks if the pin is saved on the role of the last load instead of the current one. */
  @Test
  fun togglePinChecksTheRoleAgainBeforeSaving() {
    val vm = viewModel(userId = { "author-1" }).also { it.load() }
    assertTrue(vm.uiState.value.canTogglePin)

    changeRoleInTheStoredEvent("author-1", Role.STAFF)
    vm.togglePin()

    val state = vm.uiState.value
    assertEquals(0, events.editCalls)
    assertEquals(R.string.announcement_details_save_failed, state.errorMsg)
    assertFalse(state.isUpdating)
    assertFalse(state.permissions.canPin)
    assertFalse(state.canTogglePin)
    assertEquals(announcement, state.announcement)
    assertFalse(runBlocking { localEvents.getEvent("event-1").announcements.single().pinned })
  }

  /** Breaks if a user who left the event keeps the announcement after a refused pin. */
  @Test
  fun refusedPinHidesTheAnnouncementFromAUserWhoIsNoLongerAMember() {
    var userId = OWNER_ID
    val vm = viewModel(userId = { userId }).also { it.load() }

    userId = "stranger"
    vm.togglePin()

    val state = vm.uiState.value
    assertEquals(0, events.editCalls)
    assertFalse(state.permissions.canSeeAnnouncement)
    assertNull(state.announcement)
    assertEquals(R.string.announcement_details_save_failed, state.errorMsg)
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
