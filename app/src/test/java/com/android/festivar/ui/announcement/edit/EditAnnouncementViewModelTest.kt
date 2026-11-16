// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
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
import com.android.festivar.ui.announcement.create.CreateAnnouncementUiState
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

@RunWith(AndroidJUnit4::class)
class EditAnnouncementViewModelTest {
  private val zone = ZoneId.of("Europe/Zurich")
  private val clock = Clock.fixed(Instant.parse("2026-10-30T18:00:00Z"), zone)
  private val seeded =
      Announcement(
          announcementId = "ann-1",
          title = "Gates open",
          authorId = "author-1",
          content = "The north gate opens at 16:00.",
          priority = AnnouncementPriority.INFO,
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

  private fun viewModel(
      userId: () -> String = { "owner-1" },
      clock: Clock = this.clock,
      saveTimeoutMs: Long = 15_000L,
      announcementId: String = "ann-1",
  ) =
      EditAnnouncementViewModel(
          "event-1",
          announcementId,
          userId,
          dataRepository,
          clock,
          saveTimeoutMs,
      )

  private fun loadedViewModel(
      userId: () -> String = { "owner-1" },
      clock: Clock = this.clock,
      saveTimeoutMs: Long = 15_000L,
  ) = viewModel(userId, clock, saveTimeoutMs).apply { load() }

  private fun stored(): Announcement = runBlocking {
    repo.local.getEvent("event-1").announcements.single { it.announcementId == "ann-1" }
  }

  private fun idle(seconds: Long = 0) =
      shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

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

  // Breaks if the init check on eventId or on announcementId is removed, so a blank id fails only
  // at load or save time.
  @Test
  fun blankIdsFailFast() {
    assertThrows(IllegalArgumentException::class.java) {
      EditAnnouncementViewModel(" ", "ann-1", { "owner-1" }, dataRepository)
    }
    assertThrows(IllegalArgumentException::class.java) {
      EditAnnouncementViewModel("event-1", " ", { "owner-1" }, dataRepository)
    }
  }

  // Breaks if a setter or save() acts on an announcement that is not loaded, or if the ViewModel
  // reads in init.
  @Test
  fun beforeLoadNothingIsEditableOrSavable() {
    val vm = viewModel()

    vm.updateTitle("Typed too early")
    vm.updateContent("Typed too early")
    vm.updatePriority(AnnouncementPriority.URGENT)
    vm.save()

    val state = vm.uiState.value
    assertEquals(CreateAnnouncementUiState(), state.form)
    assertNull(state.announcement)
    assertFalse(state.hasChanges)
    assertFalse(state.canSave)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(0, repo.reads)
    assertEquals(0, repo.edits)
  }

  // Breaks if a stored field is not mapped into the form, or if a loaded untouched form shows an
  // error, a change or an enabled save.
  @Test
  fun loadPrefillsTheFormWithoutErrorsOrChanges() {
    val vm = loadedViewModel()

    val state = vm.uiState.value
    assertEquals(seeded, state.announcement)
    assertEquals("Gates open", state.form.title)
    assertEquals("The north gate opens at 16:00.", state.form.content)
    assertEquals(AnnouncementPriority.INFO, state.form.priority)
    assertNull(state.form.titleError)
    assertNull(state.form.contentError)
    assertFalse(state.hasChanges)
    assertFalse(state.needsDiscardConfirmation)
    assertFalse(state.canSave)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  // Breaks if a failed load shows no error or stays loading, or if the error blocks a retry.
  @Test
  fun aFailedLoadShowsTheLoadErrorAndALaterLoadRecovers() {
    var failing = true
    repo.beforeRead = { if (failing) throw IllegalStateException("offline") }
    val vm = viewModel()

    vm.load()

    assertEquals(R.string.edit_announcement_error_load_failed, vm.uiState.value.errorMsg)
    assertNull(vm.uiState.value.announcement)
    assertFalse(vm.uiState.value.isLoading)

    failing = false
    vm.load()

    assertEquals(seeded, vm.uiState.value.announcement)
    assertNull(vm.uiState.value.errorMsg)
  }

  // Breaks if an announcement missing from the event is not reported as a load failure.
  @Test
  fun aMissingAnnouncementShowsTheLoadError() {
    val vm = viewModel(announcementId = "missing")

    vm.load()

    val state = vm.uiState.value
    assertEquals(R.string.edit_announcement_error_load_failed, state.errorMsg)
    assertNull(state.announcement)
    assertFalse(state.isLoading)
    assertFalse(state.canSave)
  }

  // Breaks if the load guard ignores a running load or a loaded announcement, so a second call
  // reads again or wipes the unsaved edit.
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

  // Breaks if titleError or contentError reads the other field, or if an error does not block save.
  @Test
  fun emptyingAFieldShowsItsErrorAndBlocksSave() {
    val vm = loadedViewModel()

    vm.updateTitle("")
    assertEquals(FieldError.EMPTY_ANNOUNCEMENT_TITLE, vm.uiState.value.form.titleError)
    assertNull(vm.uiState.value.form.contentError)
    assertFalse(vm.uiState.value.canSave)

    vm.updateTitle("Gates open")
    vm.updateContent("  ")
    assertEquals(FieldError.EMPTY_ANNOUNCEMENT_CONTENT, vm.uiState.value.form.contentError)
    assertNull(vm.uiState.value.form.titleError)
    assertFalse(vm.uiState.value.canSave)
  }

  // Breaks if a stored blank content is not flagged from the start, or if a change of priority
  // alone can save a form that is invalid.
  @Test
  fun blankStoredContentShowsTheErrorFromTheStartAndNeedsContentToSave() {
    runBlocking { repo.local.editAnnouncement("event-1", seeded.copy(content = "")) }
    val vm = loadedViewModel()

    assertEquals(FieldError.EMPTY_ANNOUNCEMENT_CONTENT, vm.uiState.value.form.contentError)

    vm.updatePriority(AnnouncementPriority.URGENT)
    assertTrue(vm.uiState.value.hasChanges)
    assertFalse(vm.uiState.value.canSave)

    vm.updateContent("The north gate opens at 16:00.")
    assertNull(vm.uiState.value.form.contentError)
    assertTrue(vm.uiState.value.canSave)
  }

  // Breaks if hasChanges ignores a field, compares untrimmed texts, or keeps a stale flag after a
  // revert, or if needsDiscardConfirmation does not follow it.
  @Test
  fun hasChangesComparesTrimmedTitleContentAndPriority() {
    val vm = loadedViewModel()
    fun changed() = vm.uiState.value.hasChanges

    vm.updateTitle("  Gates open ")
    vm.updateContent(" The north gate opens at 16:00.  ")
    assertFalse(changed())
    assertFalse(vm.uiState.value.needsDiscardConfirmation)

    vm.updateTitle("Doors open")
    assertTrue(changed())
    assertTrue(vm.uiState.value.needsDiscardConfirmation)
    vm.updateTitle("Gates open")
    assertFalse(changed())

    vm.updateContent("The south gate opens at 16:00.")
    assertTrue(changed())
    vm.updateContent("The north gate opens at 16:00.")
    assertFalse(changed())

    vm.updatePriority(AnnouncementPriority.WARNING)
    assertTrue(changed())
    vm.updatePriority(AnnouncementPriority.INFO)
    assertFalse(changed())
  }

  // Breaks if the text is stored untrimmed, or if the id, author, links, pin or the time of the
  // edit are wrong, or if the saved state still asks to discard or lets typing through.
  @Test
  fun saveStoresTheTrimmedEditsKeepingIdAuthorLinksAndPin() {
    val vm = loadedViewModel()
    vm.updateTitle("  Doors open ")
    vm.updateContent(" The south gate opens at 17:00. ")
    vm.updatePriority(AnnouncementPriority.WARNING)

    vm.save()

    val saved = stored()
    assertEquals("Doors open", saved.title)
    assertEquals("The south gate opens at 17:00.", saved.content)
    assertEquals(AnnouncementPriority.WARNING, saved.priority)
    assertEquals("ann-1", saved.announcementId)
    assertEquals("author-1", saved.authorId)
    assertEquals(seeded.links, saved.links)
    assertEquals(seeded.pinned, saved.pinned)
    assertEquals(ZonedDateTime.now(clock), saved.updatedAt)
    val state = vm.uiState.value
    assertTrue(state.isSaved)
    assertFalse(state.isSaving)
    assertNull(state.errorMsg)
    assertFalse(state.canSave)
    assertFalse(state.needsDiscardConfirmation)
    assertEquals(saved, state.announcement)

    vm.updateTitle("Later")
    assertEquals("  Doors open ", vm.uiState.value.form.title)
  }

  // Breaks if the save writes the announcement loaded at screen open, which undoes a pin toggled
  // since then.
  @Test
  fun savePreservesAPinToggledAfterTheLoad() {
    val vm = loadedViewModel()
    runBlocking { repo.local.setAnnouncementPinned("event-1", "ann-1", true) }
    vm.updateTitle("Doors open")

    vm.save()

    assertTrue(stored().pinned)
    assertEquals("Doors open", stored().title)
  }

  // Breaks if a device clock behind the stored time is written as is, which the model refuses, so
  // the save fails.
  @Test
  fun aClockBehindTheStoredTimeKeepsTheStoredTime() {
    val behind = Clock.fixed(Instant.parse("2026-10-30T05:00:00Z"), zone)
    val vm = loadedViewModel(clock = behind)
    vm.updateTitle("Doors open")

    vm.save()

    assertTrue(vm.uiState.value.isSaved)
    assertEquals("Doors open", stored().title)
    assertEquals(seeded.updatedAt.toInstant(), stored().updatedAt.toInstant())
  }

  // Breaks if a user who does not own the event can save.
  @Test
  fun aUserWhoDoesNotOwnTheEventCannotSave() {
    val vm = loadedViewModel(userId = { "someone-else" })
    vm.updateTitle("Hijacked")

    assertTrue(vm.uiState.value.hasChanges)
    assertFalse(vm.uiState.value.canSave)
    vm.save()

    assertEquals(0, repo.edits)
    assertEquals(seeded, stored())
    assertEquals(R.string.edit_announcement_error_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSaved)
  }

  // Breaks if the owner is only checked at load, so a user who signed out (a blank uid) can still
  // save.
  @Test
  fun anOwnerWhoSignedOutAfterTheLoadCannotSave() {
    var user = "owner-1"
    val vm = loadedViewModel(userId = { user })
    vm.updateTitle("Doors open")
    assertTrue(vm.uiState.value.canSave)
    user = ""

    assertFalse(vm.uiState.value.canSave)
    vm.save()

    assertEquals(0, repo.edits)
    assertEquals(seeded, stored())
    assertEquals(R.string.edit_announcement_error_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSaved)
  }

  // Breaks if canSave ignores isSaving, so a second tap writes twice, if typing during the save
  // changes what is stored, or if a saved form that still differs is savable or asks to discard.
  @Test
  fun aSaveInProgressIgnoresASecondTapAndLaterTyping() {
    val gate = CompletableDeferred<Unit>()
    repo.beforeEdit = { gate.await() }
    val vm = loadedViewModel()
    vm.updateTitle("Doors open")

    vm.save()
    assertTrue(vm.uiState.value.isSaving)
    assertFalse(vm.uiState.value.canSave)
    vm.save()
    vm.updateTitle("Later")
    gate.complete(Unit)
    idle()

    assertEquals(1, repo.edits)
    assertEquals("Doors open", stored().title)
    val state = vm.uiState.value
    assertTrue(state.isSaved)
    assertTrue(state.hasChanges)
    assertFalse(state.canSave)
    assertFalse(state.needsDiscardConfirmation)
  }

  // Breaks if the read-back after a failed write is skipped, or if it compares the stored text with
  // the untrimmed form.
  @Test
  fun aFailureAfterTheWriteLandedEndsAsSaved() {
    repo.afterEdit = { throw IllegalStateException("ack lost") }
    val vm = loadedViewModel()
    vm.updateTitle("  Doors open ")

    vm.save()
    idle()

    val state = vm.uiState.value
    assertTrue(state.isSaved)
    assertFalse(state.isSaving)
    assertNull(state.errorMsg)
    assertEquals("Doors open", stored().title)
    assertEquals(stored(), state.announcement)
  }

  // Breaks if a failed write is shown as saved, the error is missing, the form is reset, or a
  // retry after the failure is blocked.
  @Test
  fun aFailingRepositoryShowsTheSaveErrorKeepsTheFormAndARetrySucceeds() {
    var failing = true
    repo.beforeEdit = { if (failing) throw IllegalStateException("offline") }
    val vm = loadedViewModel()
    vm.updateTitle("Doors open")

    vm.save()

    val state = vm.uiState.value
    assertEquals(R.string.edit_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertEquals("Doors open", state.form.title)
    assertTrue(state.canSave)
    assertEquals(seeded, stored())

    failing = false
    vm.save()

    assertTrue(vm.uiState.value.isSaved)
    assertNull(vm.uiState.value.errorMsg)
    assertEquals("Doors open", stored().title)
  }

  // Breaks if matches ignores the title, the content or the priority, so a write that did not land
  // is counted as saved because the other two fields are unchanged.
  @Test
  fun aFailedSaveIsNotCountedAsStoredWhicheverSingleFieldDiffers() {
    repo.beforeEdit = { throw IllegalStateException("offline") }
    val edits: List<(EditAnnouncementViewModel) -> Unit> =
        listOf(
            { it.updateTitle("Doors open") },
            { it.updateContent("The south gate opens at 17:00.") },
            { it.updatePriority(AnnouncementPriority.URGENT) },
        )

    edits.forEach { edit ->
      val vm = loadedViewModel()
      edit(vm)

      vm.save()

      assertFalse(vm.uiState.value.isSaved)
      assertEquals(R.string.edit_announcement_error_save_failed, vm.uiState.value.errorMsg)
    }
    assertEquals(seeded, stored())
  }

  // Breaks if the write has no timeout, so the form stays stuck in the saving state forever.
  @Test
  fun aSaveThatHangsTimesOutAndShowsTheSaveError() {
    val never = CompletableDeferred<Unit>()
    repo.beforeEdit = { never.await() }
    val vm = loadedViewModel(saveTimeoutMs = 1_000)
    vm.updateTitle("Doors open")

    vm.save()
    assertTrue(vm.uiState.value.isSaving)
    idle(seconds = 5)

    val state = vm.uiState.value
    assertEquals(R.string.edit_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertEquals(seeded, stored())
  }

  // Breaks if a timeout is reported as a failure without reading back the write that landed.
  @Test
  fun aTimeoutAfterTheWriteLandedEndsAsSaved() {
    val never = CompletableDeferred<Unit>()
    repo.afterEdit = { never.await() }
    val vm = loadedViewModel(saveTimeoutMs = 1_000)
    vm.updateTitle("Doors open")

    vm.save()
    idle(seconds = 5)

    val state = vm.uiState.value
    assertTrue(state.isSaved)
    assertFalse(state.isSaving)
    assertNull(state.errorMsg)
    assertEquals(stored(), state.announcement)
    assertEquals("Doors open", state.announcement?.title)
  }

  // Breaks if the read-back runs without its own timeout, so a hanging read keeps the form saving
  // forever.
  @Test
  fun aReadBackThatHangsEndsAsTheSaveError() {
    val never = CompletableDeferred<Unit>()
    repo.beforeEdit = { throw IllegalStateException("offline") }
    repo.beforeRead = { if (repo.edits > 0) never.await() }
    val vm = loadedViewModel(saveTimeoutMs = 1_000)
    vm.updateTitle("Doors open")

    vm.save()
    assertTrue(vm.uiState.value.isSaving)
    idle(seconds = 5)

    val state = vm.uiState.value
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertEquals(R.string.edit_announcement_error_save_failed, state.errorMsg)
  }

  // Breaks if a failing read-back escapes as a crash or is counted as saved.
  @Test
  fun aReadBackThatFailsEndsAsTheSaveError() {
    repo.beforeEdit = { throw IllegalStateException("offline") }
    repo.beforeRead = { if (repo.edits > 0) throw IllegalStateException("still offline") }
    val vm = loadedViewModel()
    vm.updateTitle("Doors open")

    vm.save()

    val state = vm.uiState.value
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertEquals(R.string.edit_announcement_error_save_failed, state.errorMsg)
    assertTrue(state.canSave)
  }

  // Breaks if the save recreates an announcement deleted since the load, or does not report it.
  @Test
  fun anAnnouncementDeletedBeforeTheSaveShowsTheSaveErrorAndStoresNothing() {
    val vm = loadedViewModel()
    runBlocking { repo.local.deleteAnnouncement("event-1", "ann-1") }
    vm.updateTitle("Doors open")

    vm.save()

    val state = vm.uiState.value
    assertEquals(R.string.edit_announcement_error_save_failed, state.errorMsg)
    assertFalse(state.isSaved)
    assertFalse(state.isSaving)
    assertEquals(0, repo.edits)
    assertTrue(runBlocking { repo.local.getEvent("event-1") }.announcements.isEmpty())
  }

  // Breaks if CancellationException falls into the generic catch, so a cancelled save leaves the
  // form saving or shows an error the user did not cause.
  @Test
  fun aCancelledSaveClearsSavingAndShowsNoError() {
    val never = CompletableDeferred<Unit>()
    repo.beforeEdit = { never.await() }
    val vm = loadedViewModel()
    vm.updateTitle("Doors open")
    vm.save()
    assertTrue(vm.uiState.value.isSaving)

    vm.viewModelScope.cancel()
    idle()

    val state = vm.uiState.value
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertNull(state.errorMsg)
  }

  // Breaks if CancellationException falls into the generic catch of load, so a cancelled load shows
  // a load error the user did not cause.
  @Test
  fun aCancelledLoadShowsNoError() {
    val never = CompletableDeferred<Unit>()
    repo.beforeRead = { never.await() }
    val vm = viewModel()
    vm.load()

    vm.viewModelScope.cancel()
    idle()

    assertNull(vm.uiState.value.errorMsg)
    assertNull(vm.uiState.value.announcement)
  }
}
