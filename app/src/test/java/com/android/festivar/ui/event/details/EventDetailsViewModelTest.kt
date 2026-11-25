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
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import com.android.festivar.ui.event.MembersFixture
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

  /** Breaks if the owner of the event cannot edit it, so they never get the menu. */
  @Test
  fun ownerOfTheEventCanEditIt() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            event.ownerId,
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.permissions.canEditEvent)
  }

  /** Breaks if a user who is not a member of the event can edit it and gets the menu. */
  @Test
  fun anotherUserCannotEditIt() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            "volunteer",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.permissions.canEditEvent)
  }

  /** Breaks if nobody signed in can edit the event. */
  @Test
  fun signedOutUserCannotEditIt() = runTest {
    val event = testEvent("event-1")
    val viewModel =
        EventDetailsViewModel(
            dataRepositoryWith(EventsRepositoryLocal(listOf(event))),
            event.eventId,
            "",
        )
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.permissions.canEditEvent)
  }

  /** Breaks if a failed reload keeps the owner's rights over an event that is not shown. */
  @Test
  fun failedRefetchRemovesThePermissions() = runTest {
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

    assertFalse(viewModel.uiState.value.permissions.canEditEvent)
  }

  // ******************************* Roles and members ******************************* //

  private val owner = MembersFixture.organizer
  private val manager = MembersFixture.lea
  private val staff = MembersFixture.tom
  private val visitor = MembersFixture.sara
  private val rolesEvent =
      MembersFixture.eventWithRoles(
          manager to Role.STAFF_MANAGER,
          staff to Role.STAFF,
          visitor to Role.VISITOR,
      )

  /** Breaks if what the screen offers does not follow the role of the current user. */
  @Test
  fun theScreenPermissionsFollowTheRoleOfTheCurrentUser() = runTest {
    val staffView = EventDetailsPermissions(canSeeMembers = true, canSeeTasks = true)
    val managerView = staffView.copy(canEditEvent = true, canManageRoles = true)
    mapOf(
            owner to managerView.copy(canDeleteEvent = true),
            manager to managerView,
            staff to staffView,
            visitor to EventDetailsPermissions(),
        )
        .forEach { (user, expected) ->
          val permissions = loadedViewModel(user.uid).uiState.value.permissions
          assertEquals(user.name, expected, permissions.copy(members = emptyMap()))
        }
  }

  /**
   * Breaks if the roles a member can be given are not listed from the highest, or if someone not
   * strictly above them can manage them, e.g. a staff manager acting on the owner or kicking.
   */
  @Test
  fun aMemberCanOnlyBeManagedByTheRolesAboveTheirsFromTheHighest() = runTest {
    val ofOwner = loadedViewModel(owner.uid).uiState.value.permissions
    val ofManager = loadedViewModel(manager.uid).uiState.value.permissions
    val belowManager = listOf(Role.TEAM_LEAD, Role.STAFF, Role.VISITOR)

    assertEquals(
        MemberPermissions(listOf(Role.ADMIN, Role.STAFF_MANAGER) + belowManager, canKick = true),
        ofOwner.onMember(visitor.uid),
    )
    assertEquals(MemberPermissions(belowManager), ofManager.onMember(visitor.uid))
    assertEquals(MemberPermissions(), ofManager.onMember(owner.uid))
    assertEquals(MemberPermissions(), ofManager.onMember(manager.uid))
  }

  /** Breaks if a new role is not saved and shown, or drops a change saved since the load. */
  @Test
  fun changeRoleSavesTheNewRoleOnTheLatestEvent() = runTest {
    val repository = EventsRepositoryLocal(listOf(rolesEvent))
    val viewModel = loadedViewModel(owner.uid, repository)
    repository.editEvent(rolesEvent.eventId, rolesEvent.copy(title = "Renamed"))

    viewModel.changeRole(visitor.uid, Role.TEAM_LEAD)
    advanceUntilIdle()

    val saved = repository.getEvent(rolesEvent.eventId)
    assertEquals("Renamed", saved.title)
    assertEquals(Role.TEAM_LEAD, saved.roleOf(visitor.uid))
    assertEquals(saved, viewModel.uiState.value.event)
  }

  /** Breaks if a kicked member stays in the event, or the screen still offers actions on them. */
  @Test
  fun kickMemberRemovesThemAndTheActionsOnThem() = runTest {
    val repository = EventsRepositoryLocal(listOf(rolesEvent))
    val viewModel = loadedViewModel(owner.uid, repository)

    viewModel.kickMember(visitor.uid)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(repository.getEvent(rolesEvent.eventId), state.event)
    assertFalse(visitor in state.event!!.members)
    assertFalse(visitor.uid in state.permissions.members)
  }

  /**
   * Breaks if a failed save does not show its own message until it is cleared, changes the event
   * shown, or stays in progress.
   */
  @Test
  fun aFailedChangeOfRoleOrKickShowsItsErrorUntilCleared() = runTest {
    val viewModel = loadedViewModel(owner.uid, MembersFixture.failingToSave(rolesEvent))
    val saves: List<Pair<() -> Unit, Int>> =
        listOf(
            {
              viewModel.changeRole(visitor.uid, Role.STAFF)
            } to R.string.event_details_change_role_failed,
            { viewModel.kickMember(visitor.uid) } to R.string.event_details_kick_failed,
        )

    saves.forEach { (save, message) ->
      save()
      advanceUntilIdle()
      val state = viewModel.uiState.value
      assertEquals(EventDetailsUIState(rolesEvent, state.permissions, errorMsg = message), state)

      viewModel.clearError()
      assertNull(viewModel.uiState.value.errorMsg)
    }
  }

  /**
   * Breaks if a second change, a deletion or a reload runs while a change is saved, e.g. on a
   * double tap.
   */
  @Test
  fun nothingElseRunsWhileAChangeIsBeingSaved() = runTest {
    val stored = EventsRepositoryLocal(listOf(rolesEvent))
    val release = CompletableDeferred<Unit>()
    var saves = 0
    val repository =
        object : EventsRepository by stored {
          override suspend fun editEvent(eventId: String, newValue: Event) {
            saves++
            release.await()
            stored.editEvent(eventId, newValue)
          }
        }
    var leaves = 0
    val viewModel = loadedViewModel(owner.uid, repository)
    viewModel.changeRole(visitor.uid, Role.STAFF)

    viewModel.changeRole(visitor.uid, Role.TEAM_LEAD)
    viewModel.kickMember(visitor.uid)
    viewModel.deleteEvent { leaves++ }
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertEquals(1, saves)
    assertEquals(0, leaves)
    assertTrue(viewModel.uiState.value.isUpdating)
    assertEquals(rolesEvent, viewModel.uiState.value.event)
    release.complete(Unit)
    advanceUntilIdle()
    assertEquals(Role.STAFF, viewModel.uiState.value.event?.roleOf(visitor.uid))
    assertFalse(viewModel.uiState.value.isUpdating)
  }

  /** An [EventDetailsViewModel] of [rolesEvent], stored in [repository], loaded for [userId]. */
  private fun TestScope.loadedViewModel(
      userId: String,
      repository: EventsRepository = EventsRepositoryLocal(listOf(rolesEvent)),
  ): EventDetailsViewModel {
    val viewModel =
        EventDetailsViewModel(
            MembersFixture.dataRepositoryWith(rolesEvent, repository),
            rolesEvent.eventId,
            userId,
        )
    viewModel.fetchEvent()
    advanceUntilIdle()
    return viewModel
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
   * Breaks if a second tap deletes the event twice, if a change of role runs during the deletion,
   * or if a reload during the deletion resets the screen, so the organizer could tap Delete again.
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
    viewModel.changeRole("volunteer", Role.TEAM_LEAD)
    viewModel.fetchEvent()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isDeleting)
    assertEquals(testEvent("event-1"), viewModel.uiState.value.event)
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
