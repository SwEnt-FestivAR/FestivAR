// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.edit

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
import com.android.festivar.ui.announcement.AnnouncementFormState
import com.android.festivar.ui.components.FieldError
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

// The field rules (errors, trimming, matches) are tested in AnnouncementFormTest; these tests only
// cover what the ViewModel adds. Each one breaks on a change of the ViewModel no other test
// catches.
@RunWith(AndroidJUnit4::class)
class EditAnnouncementViewModelTest {
  private val zone = ZoneId.of("Europe/Zurich")
  private val clock = Clock.fixed(Instant.parse("2026-10-30T18:00:00Z"), zone)
  // Not the default priority of the form, so a priority left out of the form is seen.
  private val seeded =
      Announcement(
          announcementId = "ann-1",
          title = "Gates open",
          authorId = "author-1",
          content = "The north gate opens at 16:00.",
          priority = AnnouncementPriority.WARNING,
          updatedAt = ZonedDateTime.of(2026, 10, 30, 10, 0, 0, 0, zone),
          links = listOf("https://a.example"),
          pinned = false,
      )
  private val event =
      Event(
          eventId = "event-1",
          ownerId = "owner-1",
          title = "Balelec",
          startDate = ZonedDateTime.of(2026, 11, 6, 16, 0, 0, 0, zone),
          endDate = ZonedDateTime.of(2026, 11, 7, 4, 0, 0, 0, zone),
          announcements = listOf(seeded),
      )

  // FAKE: a real repository that keeps the event in memory. The tests read the stored
  // announcement back from it; nothing checks that a configured mock was called.
  private val repo = ScriptedRepository(EventsRepositoryLocal(listOf(event)))
  private val dataRepository = DataRepositoryLocal(FakeAuthRepository(), eventsRepository = repo)
  private val never = CompletableDeferred<Unit>()

  private fun viewModel(
      userId: () -> String = { "owner-1" },
      clock: Clock = this.clock,
      saveTimeoutMs: Long = 1_000L,
  ) = EditAnnouncementViewModel("event-1", "ann-1", userId, dataRepository, clock, saveTimeoutMs)

  private fun editedViewModel(
      userId: () -> String = { "owner-1" },
      clock: Clock = this.clock,
  ) =
      viewModel(userId, clock).apply {
        load()
        updateTitle("Doors open")
      }

  private fun stored(): Announcement = runBlocking {
    repo.local.getEvent("event-1").announcements.single()
  }

  private fun idle(seconds: Long = 0) =
      shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

  private fun assertSaveError(vm: EditAnnouncementViewModel) {
    val state = vm.uiState.value
    assertEquals(R.string.edit_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
  }

  // Delegates to the in-memory repository. It counts reads and edits, and runs a hook before every
  // read, and before and after the real edit, so a test can hang or fail a call on either side of
  // the write.
  private class ScriptedRepository(val local: EventsRepositoryLocal) : EventsRepository by local {
    var edits = 0
    var reads = 0
    var beforeRead: suspend () -> Unit = {}
    var beforeEdit: suspend () -> Unit = {}
    var afterEdit: suspend () -> Unit = {}

    override suspend fun getEvent(eventId: String): Event {
      reads++
      beforeRead()
      return local.getEvent(eventId)
    }

    override suspend fun editAnnouncement(eventId: String, announcement: Announcement) {
      edits++
      beforeEdit()
      local.editAnnouncement(eventId, announcement)
      afterEdit()
    }
  }

  /** Breaks if the init check on eventId or on announcementId is removed. */
  @Test
  fun blankIdsFailFast() {
    assertThrows(IllegalArgumentException::class.java) {
      EditAnnouncementViewModel(" ", "ann-1", { "owner-1" }, dataRepository)
    }
    assertThrows(IllegalArgumentException::class.java) {
      EditAnnouncementViewModel("event-1", " ", { "owner-1" }, dataRepository)
    }
  }

  /**
   * Breaks if a setter edits a form that is not loaded, if save() writes it, or if hasChanges reads
   * the announcement before it is loaded.
   */
  @Test
  fun beforeLoadNothingIsEditableOrSavable() {
    val vm = viewModel()

    vm.updateTitle("Typed too early")
    vm.save()

    assertEquals(AnnouncementFormState(), vm.uiState.value.form)
    assertFalse(vm.uiState.value.needsDiscardConfirmation)
    assertEquals(0, repo.edits)
  }

  /**
   * Breaks if a stored field is not mapped into the form, if the load stays loading, or if an
   * untouched form counts as a change.
   */
  @Test
  fun loadPrefillsTheFormWithoutErrorsOrChanges() {
    val vm = viewModel().apply { load() }

    val state = vm.uiState.value
    assertEquals(seeded, state.announcement)
    assertEquals(
        AnnouncementFormState(seeded.title, seeded.content, seeded.priority),
        state.form,
    )
    assertFalse(state.isLoading)
    assertFalse(state.needsDiscardConfirmation)
    assertFalse(state.canSave)
  }

  /**
   * Breaks if a missing announcement shows no load error or stays loading, or if the error blocks a
   * retry.
   */
  @Test
  fun aMissingAnnouncementShowsTheLoadErrorAndALaterLoadRecovers() {
    runBlocking { repo.local.deleteAnnouncement("event-1", "ann-1") }
    val vm = viewModel()

    vm.load()

    assertEquals(R.string.edit_announcement_error_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)

    runBlocking { repo.local.addAnnouncement("event-1", seeded) }
    vm.load()

    assertEquals(seeded, vm.uiState.value.announcement)
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if the load guard ignores a running load or a loaded announcement, so a second call
   * reads again or wipes the unsaved edit, or if isLoading is not set while the load runs.
   */
  @Test
  fun loadIsNotRepeatedWhileRunningOrOnceLoadedAndKeepsEdits() {
    val gate = CompletableDeferred<Unit>()
    repo.beforeRead = { gate.await() }
    val vm = viewModel()

    vm.load()
    vm.load()

    assertTrue(vm.uiState.value.isLoading)
    assertEquals(1, repo.reads)

    gate.complete(Unit)
    idle()
    vm.updateTitle("Doors open")
    vm.load()

    assertEquals(1, repo.reads)
    assertEquals("Doors open", vm.uiState.value.form.title)
  }

  /** Breaks if a cancelled load falls into the generic catch and shows a load error. */
  @Test
  fun aCancelledLoadShowsNoError() {
    repo.beforeRead = { never.await() }
    val vm = viewModel().apply { load() }

    vm.viewModelScope.cancel()
    idle()

    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if a stored blank content is not flagged from the start, or if canSave ignores an
   * invalid form.
   */
  @Test
  fun blankStoredContentShowsTheErrorFromTheStartAndNeedsContentToSave() {
    runBlocking { repo.local.editAnnouncement("event-1", seeded.copy(content = "")) }
    val vm = viewModel().apply { load() }

    assertEquals(FieldError.EMPTY_ANNOUNCEMENT_CONTENT, vm.uiState.value.form.contentError)

    vm.updatePriority(AnnouncementPriority.URGENT)
    assertFalse(vm.uiState.value.canSave)

    vm.updateContent("The north gate opens at 16:00.")
    assertTrue(vm.uiState.value.canSave)
  }

  /**
   * Breaks if hasChanges does not compare the form with the loaded announcement, or if
   * needsDiscardConfirmation does not follow it. The compared fields and the trimming are tested in
   * AnnouncementFormTest (matches).
   */
  @Test
  fun aFormThatDiffersFromTheLoadedAnnouncementHasChanges() {
    val vm = viewModel().apply { load() }

    vm.updatePriority(AnnouncementPriority.URGENT)
    assertTrue(vm.uiState.value.needsDiscardConfirmation)
  }

  /**
   * Breaks if a text is stored untrimmed or a field is not stored, if the time of the edit is not
   * now, if the save writes the copy loaded at screen open (undoing a pin toggled since), or if a
   * saved form still takes typing.
   */
  @Test
  fun saveStoresTheTrimmedEditsOnTheLatestCopy() {
    val vm = viewModel().apply { load() }
    runBlocking { repo.local.setAnnouncementPinned("event-1", "ann-1", true) }
    vm.updateTitle("  Doors open ")
    vm.updateContent(" The south gate opens at 17:00. ")
    vm.updatePriority(AnnouncementPriority.URGENT)

    vm.save()

    val expected =
        seeded.copy(
            title = "Doors open",
            content = "The south gate opens at 17:00.",
            priority = AnnouncementPriority.URGENT,
            updatedAt = ZonedDateTime.now(clock),
            pinned = true,
        )
    assertEquals(expected, stored())
    assertEquals(expected, vm.uiState.value.announcement)
    assertTrue(vm.uiState.value.isSaved)
    assertFalse(vm.uiState.value.isSaving)

    vm.updateTitle("Later")
    assertEquals("  Doors open ", vm.uiState.value.form.title)
  }

  /** Breaks if a device clock behind the stored time is written as is, which the model refuses. */
  @Test
  fun aClockBehindTheStoredTimeKeepsTheStoredTime() {
    val behind = Clock.fixed(Instant.parse("2026-10-30T05:00:00Z"), zone)
    val vm = editedViewModel(clock = behind)

    vm.save()

    assertEquals("Doors open", stored().title)
    assertEquals(seeded.updatedAt.toInstant(), stored().updatedAt.toInstant())
  }

  /**
   * Breaks if the owner is not checked, or only at load, so a user who no longer owns the event can
   * save, or if the refused save shows no error.
   */
  @Test
  fun aUserWhoNoLongerOwnsTheEventCannotSave() {
    var user = "owner-1"
    val vm = editedViewModel(userId = { user })
    assertTrue(vm.uiState.value.canSave)
    user = "someone-else"

    assertFalse(vm.uiState.value.canSave)
    vm.save()

    assertEquals(0, repo.edits)
    assertSaveError(vm)
  }

  /**
   * Breaks if canSave or isEditable ignores isSaving or isSaved, so a second tap writes twice or
   * the inputs stay open, if typing during the save changes the form, or if a saved form that still
   * differs asks to discard.
   */
  @Test
  fun aSaveInProgressIgnoresASecondTapAndLaterTyping() {
    val gate = CompletableDeferred<Unit>()
    repo.beforeEdit = { gate.await() }
    val vm = editedViewModel()
    assertTrue(vm.uiState.value.isEditable)

    vm.save()
    assertTrue(vm.uiState.value.isSaving)
    assertFalse(vm.uiState.value.isEditable)
    vm.save()
    vm.updateTitle("Later")
    assertEquals("Doors open", vm.uiState.value.form.title)
    gate.complete(Unit)
    idle()

    assertEquals(1, repo.edits)
    assertEquals("Doors open", stored().title)
    val state = vm.uiState.value
    assertTrue(state.isSaved)
    assertFalse(state.canSave)
    assertFalse(state.isEditable)
    assertFalse(state.needsDiscardConfirmation)
  }

  /**
   * Breaks if a failed write is shown as saved, resets the form or leaves it locked, if a retry
   * keeps the old error, or if a write that landed before the failure is not read back as saved.
   */
  @Test
  fun aFailedSaveKeepsTheFormAndARetryThatLandedEndsAsSaved() {
    repo.beforeEdit = { throw IllegalStateException("offline") }
    val vm = editedViewModel()

    vm.save()

    assertSaveError(vm)
    assertTrue(vm.uiState.value.canSave)
    assertTrue(vm.uiState.value.isEditable)
    assertEquals(seeded, stored())

    repo.beforeEdit = {}
    repo.afterEdit = { throw IllegalStateException("ack lost") }
    vm.save()

    assertTrue(vm.uiState.value.isSaved)
    assertNull(vm.uiState.value.errorMsg)
    assertEquals(stored(), vm.uiState.value.announcement)
    assertEquals("Doors open", stored().title)
  }

  /**
   * Breaks if the write has no timeout, or if a timeout is reported as a failure without reading
   * back the write that landed.
   */
  @Test
  fun aTimeoutAfterTheWriteLandedEndsAsSaved() {
    repo.afterEdit = { never.await() }
    val vm = editedViewModel()

    vm.save()
    idle(seconds = 5)

    assertTrue(vm.uiState.value.isSaved)
    assertFalse(vm.uiState.value.isSaving)
    assertEquals("Doors open", vm.uiState.value.announcement?.title)
  }

  /**
   * Breaks if the read-back after a failed write has no timeout of its own, or lets its failure
   * escape, so the form stays saving or crashes.
   */
  @Test
  fun aReadBackThatFailsOrHangsEndsAsTheSaveError() {
    val readBacks: List<suspend () -> Unit> =
        listOf({ throw IllegalStateException("still offline") }, { never.await() })
    repo.beforeEdit = { throw IllegalStateException("offline") }

    readBacks.forEach { readBack ->
      repo.edits = 0
      repo.beforeRead = { if (repo.edits > 0) readBack() }
      val vm = editedViewModel()

      vm.save()
      idle(seconds = 5)

      assertSaveError(vm)
    }
  }

  /** Breaks if the save recreates an announcement deleted since the load, or does not report it. */
  @Test
  fun anAnnouncementDeletedBeforeTheSaveShowsTheSaveErrorAndStoresNothing() {
    val vm = editedViewModel()
    runBlocking { repo.local.deleteAnnouncement("event-1", "ann-1") }

    vm.save()

    assertSaveError(vm)
    assertEquals(0, repo.edits)
    assertTrue(runBlocking { repo.local.getEvent("event-1") }.announcements.isEmpty())
  }

  /**
   * Breaks if a cancellation during the save or its read-back falls into a generic catch, so the
   * form stays saving or shows an error the user did not cause.
   */
  @Test
  fun aSaveCancelledDuringTheReadBackClearsSavingAndShowsNoError() {
    repo.beforeEdit = { throw IllegalStateException("offline") }
    repo.beforeRead = { if (repo.edits > 0) never.await() }
    val vm = editedViewModel()
    vm.save()
    idle()
    assertTrue(vm.uiState.value.isSaving)

    vm.viewModelScope.cancel()
    idle()

    assertFalse(vm.uiState.value.isSaving)
    assertNull(vm.uiState.value.errorMsg)
  }
}
