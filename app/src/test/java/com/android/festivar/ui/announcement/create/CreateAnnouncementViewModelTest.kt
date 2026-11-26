// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.create

import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class CreateAnnouncementViewModelTest {
  private val zone = ZoneId.of("Europe/Zurich")
  private val clock = Clock.fixed(Instant.parse("2026-10-30T18:00:00Z"), zone)
  // One member per role around CAN_CREATE_ANNOUNCEMENT: the owner and the team lead have it, the
  // staff member and the visitor do not.
  private val roles =
      mapOf(
          "owner-1" to Role.OWNER,
          "lead-1" to Role.TEAM_LEAD,
          "staff-1" to Role.STAFF,
          "visitor-1" to Role.VISITOR,
      )
  private val event =
      Event(
          eventId = "event-1",
          ownerId = "owner-1",
          title = "Balelec",
          members = roles.keys.map { User(it) },
          roles = roles,
          startDate = ZonedDateTime.of(2026, 11, 6, 16, 0, 0, 0, zone),
          endDate = ZonedDateTime.of(2026, 11, 7, 4, 0, 0, 0, zone),
      )

  // FAKE: a real repository that keeps the event in memory. The tests read the stored
  // announcements back from it; nothing checks that a configured mock was called.
  private val repo = ScriptedRepository(EventsRepositoryLocal(listOf(event)))
  private val dataRepository = DataRepositoryLocal(FakeAuthRepository(), eventsRepository = repo)

  private fun viewModel(
      userId: () -> String = { "owner-1" },
      clock: Clock = this.clock,
      saveTimeoutMs: Long = 15_000L,
  ) = CreateAnnouncementViewModel("event-1", userId, dataRepository, clock, saveTimeoutMs)

  private fun stored(): List<Announcement> = runBlocking {
    repo.local.getEvent("event-1").announcements
  }

  private fun CreateAnnouncementViewModel.fill() {
    updateTitle("Gates open")
    updateContent("The north gate opens at 16:00.")
  }

  private fun idle(seconds: Long = 0) =
      shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

  // A clock the test moves by hand, to tell the time of a first attempt from the time of a retry.
  private class MovingClock(private val zone: ZoneId) : Clock() {
    var now: Instant = Instant.parse("2026-10-30T18:00:00Z")

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
  }

  // Delegates to the in-memory repository. It records the ids it hands out and the ids it is asked
  // to add, counts edits and reads, and runs a hook before and after the real add, so a test can
  // hang or fail a save on either side of the write.
  private class ScriptedRepository(val local: EventsRepositoryLocal) : EventsRepository by local {
    val issuedIds = mutableListOf<String>()
    val addedIds = mutableListOf<String>()
    var edits = 0
    var reads = 0
    var beforeStore: suspend () -> Unit = {}
    var afterStore: suspend () -> Unit = {}
    var beforeRead: suspend () -> Unit = {}

    override fun getNewUid(): String = local.getNewUid().also { issuedIds += it }

    override suspend fun addAnnouncement(eventId: String, announcement: Announcement) {
      addedIds += announcement.announcementId
      beforeStore()
      local.addAnnouncement(eventId, announcement)
      afterStore()
    }

    override suspend fun editAnnouncement(eventId: String, announcement: Announcement) {
      edits++
      local.editAnnouncement(eventId, announcement)
    }

    override suspend fun getEvent(eventId: String): Event {
      reads++
      beforeRead()
      return local.getEvent(eventId)
    }
  }

  /**
   * Breaks if canPublish ignores the form, so an empty form can publish, or if applyEdit does not
   * store the edited form in the state. The field rules are tested in AnnouncementFormTest.
   */
  @Test
  fun typingReachesTheFormAndOnlyAValidFormCanPublish() {
    val vm = viewModel()
    assertFalse(vm.uiState.value.canPublish)

    vm.fill()
    vm.updatePriority(AnnouncementPriority.URGENT)

    val state = vm.uiState.value
    assertEquals("Gates open", state.form.title)
    assertEquals("The north gate opens at 16:00.", state.form.content)
    assertEquals(AnnouncementPriority.URGENT, state.form.priority)
    assertTrue(state.canPublish)
  }

  /**
   * Breaks if hasChanges forgets the title, the content or the priority, or if a published form
   * still asks before leaving.
   */
  @Test
  fun hasChangesAndNeedsDiscardConfirmationFollowTheForm() {
    val vm = viewModel()
    assertFalse(vm.uiState.value.hasChanges)
    assertFalse(vm.uiState.value.needsDiscardConfirmation)

    vm.updateTitle("Gates open")
    assertTrue(vm.uiState.value.hasChanges)
    vm.updateTitle("")
    assertFalse(vm.uiState.value.hasChanges)

    vm.updateContent("The north gate opens at 16:00.")
    assertTrue(vm.uiState.value.hasChanges)
    vm.updateContent("")
    assertFalse(vm.uiState.value.hasChanges)

    vm.updatePriority(AnnouncementPriority.WARNING)
    assertTrue(vm.uiState.value.hasChanges)
    assertTrue(vm.uiState.value.needsDiscardConfirmation)

    vm.fill()
    vm.publish()
    assertTrue(vm.uiState.value.isPublished)
    assertFalse(vm.uiState.value.needsDiscardConfirmation)
  }

  /**
   * Breaks if isEditable ignores isSaving or isPublished, or if a failed save leaves the form
   * locked, so the user cannot fix it and retry.
   */
  @Test
  fun theFormIsEditableExceptDuringASaveAndOncePublished() {
    val gate = CompletableDeferred<Unit>()
    repo.beforeStore = {
      gate.await()
      throw IllegalStateException("offline")
    }
    val vm = viewModel()
    assertTrue(vm.uiState.value.isEditable)

    vm.fill()
    vm.publish()
    assertTrue(vm.uiState.value.isSaving)
    assertFalse(vm.uiState.value.isEditable)

    gate.complete(Unit)
    idle()
    assertEquals(R.string.create_announcement_error_save_failed, vm.uiState.value.errorMsg)
    assertTrue(vm.uiState.value.isEditable)

    repo.beforeStore = {}
    vm.publish()
    assertTrue(vm.uiState.value.isPublished)
    assertFalse(vm.uiState.value.isEditable)
  }

  /**
   * Breaks if the text is stored untrimmed, the author, priority or time is wrong, or the id is not
   * the one the repository handed out.
   */
  @Test
  fun aValidFormPublishesOneTrimmedAnnouncementWithAuthorPriorityAndTime() {
    val vm = viewModel()
    vm.updateTitle("  Gates open ")
    vm.updateContent(" The north gate opens at 16:00. ")
    vm.updatePriority(AnnouncementPriority.WARNING)

    vm.publish()

    val saved = stored().single()
    assertEquals("Gates open", saved.title)
    assertEquals("The north gate opens at 16:00.", saved.content)
    assertEquals("owner-1", saved.authorId)
    assertEquals(AnnouncementPriority.WARNING, saved.priority)
    assertEquals(ZonedDateTime.now(clock), saved.updatedAt)
    assertEquals(repo.issuedIds.single(), saved.announcementId)
    assertTrue(vm.uiState.value.isPublished)
    assertFalse(vm.uiState.value.isSaving)
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if a blank uid is not checked and an authorless announcement is written or attempted.
   */
  @Test
  fun aBlankUserIdPublishesNothingAndShowsTheSaveError() {
    val vm = viewModel(userId = { " " })
    vm.fill()

    vm.publish()

    assertTrue(stored().isEmpty())
    assertTrue(repo.addedIds.isEmpty())
    assertFalse(vm.uiState.value.isPublished)
    assertEquals(R.string.create_announcement_error_save_failed, vm.uiState.value.errorMsg)
  }

  /** Breaks if canPublish ignores isSaving, so a second tap writes a second announcement. */
  @Test
  fun aSecondTapWhileSavingIsIgnored() {
    val gate = CompletableDeferred<Unit>()
    repo.beforeStore = { gate.await() }
    val vm = viewModel()
    vm.fill()

    vm.publish()
    assertTrue(vm.uiState.value.isSaving)
    vm.publish()
    gate.complete(Unit)
    idle()

    assertEquals(1, repo.addedIds.size)
    assertEquals(1, stored().size)
    assertTrue(vm.uiState.value.isPublished)
    assertFalse(vm.uiState.value.isSaving)
  }

  /** Breaks if canPublish ignores isPublished, or if applyEdit lets a published form change. */
  @Test
  fun aTapAfterAPublishDoesNotPublishASecondAnnouncementAndEditsAreIgnored() {
    val vm = viewModel()
    vm.fill()
    vm.publish()
    val published = vm.uiState.value.form

    vm.publish()
    vm.updateTitle("Something else")
    vm.updateContent("Other text")
    vm.updatePriority(AnnouncementPriority.URGENT)

    assertEquals(1, repo.addedIds.size)
    assertEquals(1, stored().size)
    assertEquals(published, vm.uiState.value.form)
  }

  /** Breaks if a failed write is reported as published, clears the form, or skips the read-back. */
  @Test
  fun aFailingRepositoryReportsTheSaveErrorAndKeepsTheForm() {
    repo.beforeStore = { throw IllegalStateException("offline") }
    val vm = viewModel()
    vm.fill()
    val typed = vm.uiState.value.form
    // Only the reads of the publish are counted: the role check before the write, then the
    // read-back.
    repo.reads = 0

    vm.publish()

    val state = vm.uiState.value
    assertEquals(R.string.create_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isPublished)
    assertFalse(state.isSaving)
    assertTrue(state.canPublish)
    assertEquals(typed, state.form)
    assertEquals(2, repo.reads)
    assertTrue(stored().isEmpty())
  }

  /** Breaks if the write has no timeout, so the form stays stuck in the saving state forever. */
  @Test
  fun aSaveThatHangsTimesOutAndKeepsTheForm() {
    val never = CompletableDeferred<Unit>()
    repo.beforeStore = { never.await() }
    val vm = viewModel(saveTimeoutMs = 1_000)
    vm.fill()
    vm.updatePriority(AnnouncementPriority.WARNING)
    val typed = vm.uiState.value.form

    vm.publish()
    assertTrue(vm.uiState.value.isSaving)
    idle(seconds = 5)

    val state = vm.uiState.value
    assertEquals(R.string.create_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertTrue(state.canPublish)
    assertEquals(typed, state.form)
  }

  /**
   * Breaks if a timeout is reported as a failure without reading back the write that landed, or if
   * the read-back edits a stored copy that already matches the form.
   */
  @Test
  fun aTimeoutAfterTheWriteLandedEndsAsPublished() {
    val never = CompletableDeferred<Unit>()
    repo.afterStore = { never.await() }
    val vm = viewModel(saveTimeoutMs = 1_000)
    vm.fill()

    vm.publish()
    idle(seconds = 5)

    assertTrue(vm.uiState.value.isPublished)
    assertNull(vm.uiState.value.errorMsg)
    assertEquals(1, stored().size)
    assertEquals(0, repo.edits)
  }

  /**
   * Breaks if the read-back runs without its own timeout, so a hanging event read keeps the form
   * saving forever.
   */
  @Test
  fun aReadBackThatHangsEndsAsTheSaveError() {
    val never = CompletableDeferred<Unit>()
    var writeTried = false
    repo.beforeStore = {
      writeTried = true
      throw IllegalStateException("offline")
    }
    // Only the read-back hangs: the role is read before the write is tried.
    repo.beforeRead = { if (writeTried) never.await() }
    val vm = viewModel(saveTimeoutMs = 1_000)
    vm.fill()

    vm.publish()
    assertTrue(vm.uiState.value.isSaving)
    idle(seconds = 5)

    val state = vm.uiState.value
    assertFalse(state.isSaving)
    assertEquals(R.string.create_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isPublished)
    assertTrue(stored().isEmpty())
  }

  /** Breaks if a retry draws a new id or a new time instead of the ones kept from the first try. */
  @Test
  fun aRetryReusesTheIdAndTheTimeOfTheFirstAttempt() {
    val moving = MovingClock(zone)
    var failing = true
    repo.beforeStore = { if (failing) throw IllegalStateException("offline") }
    val vm = viewModel(clock = moving)
    vm.fill()
    vm.publish()
    assertEquals(R.string.create_announcement_error_save_failed, vm.uiState.value.errorMsg)

    moving.now = moving.now.plusSeconds(60)
    failing = false
    vm.publish()

    val saved = stored().single()
    assertEquals(1, repo.issuedIds.size)
    assertEquals(repo.issuedIds.single(), saved.announcementId)
    assertEquals(ZonedDateTime.of(2026, 10, 30, 19, 0, 0, 0, zone), saved.updatedAt)
    assertTrue(vm.uiState.value.isPublished)
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if the read-back marks a stored copy that differs from the form published without
   * editing it to the new text, if the edit carries a new time instead of the first attempt's, or
   * if it hands the announcement to the user who retried. Which fields are compared is tested in
   * AnnouncementFormTest (matches).
   */
  @Test
  fun aRetryThatFindsTheStoredCopyUpdatesItWithTheEditedText() {
    var firstTry = true
    repo.afterStore = {
      if (firstTry) {
        firstTry = false
        throw IllegalStateException("lost the answer")
      }
    }
    // The first read-back fails too (offline), so the first attempt ends with the error. The reads
    // of the role, before the writes, succeed.
    var readBackFailed = false
    repo.beforeRead = {
      if (!firstTry && !readBackFailed) {
        readBackFailed = true
        throw IllegalStateException("offline")
      }
    }
    val moving = MovingClock(zone)
    var user = "owner-1"
    val vm = viewModel(userId = { user }, clock = moving)
    vm.fill()
    vm.publish()
    assertEquals(R.string.create_announcement_error_save_failed, vm.uiState.value.errorMsg)

    moving.now = moving.now.plusSeconds(60)
    user = "lead-1"
    vm.updateContent("The north gate opens at 16:30.")
    vm.publish()

    val saved = stored().single()
    assertEquals("The north gate opens at 16:30.", saved.content)
    assertEquals(ZonedDateTime.of(2026, 10, 30, 19, 0, 0, 0, zone), saved.updatedAt)
    assertEquals("owner-1", saved.authorId)
    assertEquals(1, repo.edits)
    assertTrue(vm.uiState.value.isPublished)
  }

  /**
   * Breaks if CancellationException falls into the generic catch, so a cancelled save leaves the
   * form saving or shows an error the user did not cause.
   */
  @Test
  fun aCancelledSaveClearsSavingAndShowsNoError() {
    val never = CompletableDeferred<Unit>()
    repo.beforeStore = { never.await() }
    val vm = viewModel()
    vm.fill()
    vm.publish()
    assertTrue(vm.uiState.value.isSaving)

    vm.viewModelScope.cancel()
    idle()

    val state = vm.uiState.value
    assertFalse(state.isSaving)
    assertNull(state.errorMsg)
    assertFalse(state.isPublished)
  }

  /**
   * Breaks if the announcement is built from the live state inside the coroutine, so typing during
   * the save changes what is stored.
   */
  @Test
  fun typingDuringTheSaveDoesNotChangeWhatIsSaved() {
    val gate = CompletableDeferred<Unit>()
    repo.beforeStore = { gate.await() }
    val vm = viewModel()
    vm.fill()
    vm.publish()

    vm.updateTitle("Doors open")
    gate.complete(Unit)
    idle()

    assertEquals("Gates open", stored().single().title)
    assertEquals("Doors open", vm.uiState.value.form.title)
  }

  /**
   * Breaks if the role is not read when the screen opens, or if a role with CAN_CREATE_ANNOUNCEMENT
   * is refused.
   */
  @Test
  fun aRoleWithTheCreatePermissionCanPublish() {
    val vm = viewModel(userId = { "lead-1" })
    vm.fill()

    assertTrue(vm.uiState.value.canCreateAnnouncement)
    assertTrue(vm.uiState.value.canPublish)
    vm.publish()

    assertEquals("lead-1", stored().single().authorId)
    assertTrue(vm.uiState.value.isPublished)
  }

  /**
   * Breaks if a role without CAN_CREATE_ANNOUNCEMENT is allowed on the form, or if canPublish
   * ignores canCreateAnnouncement, so such a member can still publish.
   */
  @Test
  fun aRoleWithoutTheCreatePermissionCannotPublish() {
    for (userId in listOf("staff-1", "visitor-1")) {
      val vm = viewModel(userId = { userId })
      vm.fill()

      assertFalse(userId, vm.uiState.value.canCreateAnnouncement)
      assertFalse(userId, vm.uiState.value.canPublish)
      vm.publish()

      assertTrue(userId, repo.addedIds.isEmpty())
      assertFalse(userId, vm.uiState.value.isPublished)
    }
  }

  /** Breaks if a user who is not a member makes the role check throw instead of refusing them. */
  @Test
  fun aUserWhoIsNotAMemberCannotPublish() {
    val vm = viewModel(userId = { "stranger" })

    assertFalse(vm.uiState.value.canCreateAnnouncement)
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if a failed load of the event refuses the user, so the screen closes for everyone
   * offline.
   */
  @Test
  fun aFailedLoadOfTheEventKeepsTheFormOpen() {
    var failing = true
    repo.beforeRead = { if (failing) throw IllegalStateException("offline") }
    val vm = viewModel()

    assertTrue(vm.uiState.value.canCreateAnnouncement)

    failing = false
    vm.fill()
    vm.publish()
    assertTrue(vm.uiState.value.isPublished)
  }

  /**
   * Breaks if publish trusts the role read when the screen opened instead of checking it again
   * before the write.
   */
  @Test
  fun aRoleLostAfterTheScreenOpenedPublishesNothing() {
    val vm = viewModel(userId = { "lead-1" })
    vm.fill()
    assertTrue(vm.uiState.value.canPublish)

    runBlocking {
      repo.local.editEvent("event-1", event.copy(roles = roles + ("lead-1" to Role.STAFF)))
    }
    vm.publish()

    val state = vm.uiState.value
    assertTrue(repo.addedIds.isEmpty())
    assertTrue(stored().isEmpty())
    assertFalse(state.isPublished)
    assertFalse(state.isSaving)
    assertFalse(state.canCreateAnnouncement)
    assertEquals(R.string.create_announcement_error_save_failed, state.errorMsg)
  }

  /** Breaks if the init check on eventId is removed, so a blank event fails only at save time. */
  @Test
  fun aBlankEventIdFailsFast() {
    assertThrows(IllegalArgumentException::class.java) {
      CreateAnnouncementViewModel(" ", { "owner-1" }, dataRepository, clock)
    }
  }
}
