// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.edit

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.voicenote.VoiceNote
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.task.create.CreateTaskUiState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * FAKE over [TasksRepositoryLocal]: it stores like the local one, counts the edits and reads, and
 * can make the edit fail or wait on a gate, after storing or before. A hand-written fake, not a
 * mock: the tests check the state and what was stored.
 */
private class ScriptedRepository(val local: TasksRepositoryLocal) : TasksRepository by local {
  var edits = 0
  var reads = 0

  /** Thrown by [editTask] when not null, after storing only if [landsFirst]. */
  var editFailure: Exception? = null

  /** While not null, [editTask] waits on it. */
  var editGate: CompletableDeferred<Unit>? = null

  /** When true, [editTask] stores the task before failing or waiting. */
  var landsFirst = false

  /** Thrown by [getTask] when not null. */
  var readFailure: Exception? = null

  /** While not null, [getTask] waits on it. */
  var readGate: CompletableDeferred<Unit>? = null

  override suspend fun getTask(taskId: String): Task {
    reads++
    readGate?.await()
    readFailure?.let { throw it }
    return local.getTask(taskId)
  }

  override suspend fun editTask(taskId: String, newValue: Task) {
    edits++
    if (landsFirst) local.editTask(taskId, newValue)
    editFailure?.let { throw it }
    editGate?.await()
    if (!landsFirst) local.editTask(taskId, newValue)
  }
}

/** Tests of [EditTaskViewModel] against a fake repository holding one task. */
@RunWith(AndroidJUnit4::class)
class EditTaskViewModelTest {
  private val zone = ZoneId.of("Europe/Zurich")
  // Tomorrow, like the picker tests of #371: the form refuses an end in the past.
  private val day = LocalDate.now(zone).plusDays(1)
  private val voiceNote =
      VoiceNote(
          id = "note-1",
          audioUrl = "https://example.com/note-1.m4a",
          duration = Duration.ofSeconds(12),
          createdAt = ZonedDateTime.of(day, LocalTime.of(8, 0), zone),
      )
  private val seeded =
      Task(
          taskId = "task-1",
          eventId = "event-1",
          title = "Run power to stage",
          description = "Blue crate",
          startTime = ZonedDateTime.of(day, LocalTime.of(9, 0), zone),
          endTime = ZonedDateTime.of(day, LocalTime.of(10, 30), zone),
          estimatedTime = Duration.ofMinutes(45),
          location = "Stage",
          priority = Priority.HIGH,
          maxAssign = 3,
          assignees = listOf(User(uid = "me"), User(uid = "other")),
          voiceNote = voiceNote,
          ownerId = "owner-1",
      )
  private val local = TasksRepositoryLocal().also { runBlocking { it.addTask(seeded) } }
  private val repository = ScriptedRepository(local)
  private val viewModel = EditTaskViewModel("task-1", { "owner-1" }, repository, zone = zone)

  private fun stored(): Task = runBlocking { local.getTask("task-1") }

  private fun idle() = shadowOf(Looper.getMainLooper()).idle()

  private fun loadedViewModel(saveTimeoutMs: Long = 15_000L): EditTaskViewModel =
      EditTaskViewModel("task-1", { "owner-1" }, repository, saveTimeoutMs, zone).apply { load() }

  // Breaks if the ViewModel loads in init or reports loading before being asked.
  @Test
  fun aFreshViewModelHasNoTaskAndIsNotLoadingUntilLoadIsCalled() {
    viewModel.updateTitle("Typed too early")

    val state = viewModel.uiState.value
    assertEquals("", state.form.title)
    assertFalse(state.isLoading)
    assertNull(state.task)
    assertFalse(state.hasChanges)
    assertFalse(state.canSave)
    assertEquals(0, repository.reads)

    repository.readGate = CompletableDeferred()
    viewModel.load()

    assertTrue(viewModel.uiState.value.isLoading)
  }

  // Breaks if a task field is not mapped into the form, the times are not converted to the zone,
  // or isOwner does not compare the owner with the signed-in user.
  @Test
  fun loadFillsTheFormFromTheTaskAndKnowsTheOwner() {
    viewModel.load()

    val state = viewModel.uiState.value
    val form = state.form
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals(seeded, state.task)
    assertTrue(state.isOwner())
    assertEquals("Run power to stage", form.title)
    assertEquals("Blue crate", form.description)
    assertEquals("Stage", form.location)
    assertEquals(day, form.startDate)
    assertEquals(LocalTime.of(9, 0), form.startTime)
    assertEquals(day, form.endDate)
    assertEquals(LocalTime.of(10, 30), form.endTime)
    assertEquals(Priority.HIGH, form.priority)
    assertEquals(3, form.maxAssign)
    assertEquals("45", form.estimatedMinutes)
    assertEquals(zone, form.zone)

    val elsewhere =
        EditTaskViewModel("task-1", { "someone-else" }, repository, zone = ZoneId.of("UTC"))
    elsewhere.load()

    val other = elsewhere.uiState.value
    assertFalse(other.isOwner())
    // Zurich is one or two hours ahead of UTC depending on the season of tomorrow.
    val utc = ZoneId.of("UTC")
    assertEquals(seeded.startTime?.withZoneSameInstant(utc)?.toLocalTime(), other.form.startTime)
    assertEquals(seeded.endTime?.withZoneSameInstant(utc)?.toLocalTime(), other.form.endTime)
  }

  // Breaks if load reloads an already loaded task.
  @Test
  fun loadingAgainKeepsTheEditsAndMakesNoRead() {
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")
    val readsAfterLoad = repository.reads

    viewModel.load()

    assertEquals("Run power to the main stage", viewModel.uiState.value.form.title)
    assertTrue(viewModel.uiState.value.hasChanges)
    assertEquals(readsAfterLoad, repository.reads)
  }

  // Breaks if the load guard only checks the task, so a second call reads again.
  @Test
  fun aSecondLoadWhileTheFirstRunsStartsNoSecondRead() {
    val gate = CompletableDeferred<Unit>()
    repository.readGate = gate

    viewModel.load()
    viewModel.load()
    gate.complete(Unit)
    idle()

    assertEquals(1, repository.reads)
    assertEquals(seeded, viewModel.uiState.value.task)
  }

  // Breaks if the guard or the error reset blocks a retry after a failed load.
  @Test
  fun aFailedLoadCanBeRetried() {
    val vm = EditTaskViewModel("task-2", { "owner-1" }, repository, zone = zone)
    vm.load()
    assertEquals(R.string.edit_task_error_load_failed, vm.uiState.value.errorMsg)

    runBlocking { local.addTask(seeded.copy(taskId = "task-2")) }
    val gate = CompletableDeferred<Unit>()
    repository.readGate = gate
    vm.load()
    assertTrue(vm.uiState.value.isLoading)
    assertNull(vm.uiState.value.errorMsg)
    gate.complete(Unit)
    idle()

    val state = vm.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
    assertEquals("task-2", state.task?.taskId)
  }

  // Breaks if the owner is only checked at load, so a user who signed out and in as someone else
  // can still save.
  @Test
  fun aUserWhoIsNoLongerTheOwnerAtSaveTimeCannotSave() {
    var user = "owner-1"
    val vm = EditTaskViewModel("task-1", { user }, repository, zone = zone)
    vm.load()
    vm.updateTitle("Run power to the main stage")
    assertTrue(vm.uiState.value.isOwner())
    user = "someone-else"

    assertFalse(vm.uiState.value.isOwner())
    assertFalse(vm.uiState.value.canSave)
    vm.save()

    assertEquals(0, repository.edits)
    assertEquals(seeded, stored())
    assertEquals(R.string.edit_task_error_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSaved)
  }

  // Breaks if the read-back result is ignored, so a write that landed is reported as failed.
  @Test
  fun aFailedWriteWhoseReadBackMatchesEndsAsSaved() {
    repository.editFailure = IllegalStateException("lost the answer")
    repository.landsFirst = true
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")

    viewModel.save()

    val state = viewModel.uiState.value
    assertTrue(state.isSaved)
    assertNull(state.errorMsg)
    assertEquals("Run power to the main stage", stored().title)
  }

  // Breaks if the read-back result is ignored, so a write that did not land is reported as saved.
  @Test
  fun aFailedWriteWhoseReadBackDiffersShowsTheError() {
    repository.editFailure = IllegalStateException("offline")
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")

    viewModel.save()

    val state = viewModel.uiState.value
    assertFalse(state.isSaved)
    assertEquals(R.string.edit_task_error_save_failed, state.errorMsg)
    assertEquals("Run power to the main stage", state.form.title)
    assertEquals(seeded, stored())
  }

  // Breaks if boundary times are dropped as "no time", so the untouched form differs from the task.
  @Test
  fun timesOnTheDayBoundariesRoundTrip() {
    val wholeDay =
        seeded.copy(
            taskId = "task-3",
            startTime = ZonedDateTime.of(day, START_OF_DAY, zone),
            endTime = ZonedDateTime.of(day, END_OF_DAY, zone),
        )
    runBlocking { local.addTask(wholeDay) }
    val vm = EditTaskViewModel("task-3", { "owner-1" }, repository, zone = zone)

    vm.load()

    val form = vm.uiState.value.form
    assertEquals(START_OF_DAY, form.startTime)
    assertEquals(END_OF_DAY, form.endTime)
    assertFalse(vm.uiState.value.hasChanges)
  }

  // Breaks if toForm stops normalising a time in the repeated hour of the daylight saving change to
  // the earlier offset as documented, or if that lossy mapping alone counts as a change.
  @Test
  fun aTimeInTheSecondRepeatedHourMapsToTheEarlierOffset() {
    // The one fixed date: Zurich leaves summer time on 25 October 2037, so 02:00 to 03:00 happens
    // twice. Far ahead, since the form refuses an end in the past.
    val repeated = LocalDateTime.of(2037, 10, 25, 2, 30)
    val secondOccurrence = ZonedDateTime.ofLocal(repeated, zone, ZoneOffset.ofHours(1))
    val firstOccurrence = ZonedDateTime.ofLocal(repeated, zone, ZoneOffset.ofHours(2))
    assertEquals(ZoneOffset.ofHours(1), secondOccurrence.offset)
    runBlocking {
      local.addTask(
          seeded.copy(
              taskId = "task-4",
              startTime = secondOccurrence,
              endTime = secondOccurrence.plusHours(1),
          )
      )
    }
    val vm = EditTaskViewModel("task-4", { "owner-1" }, repository, zone = zone)

    vm.load()

    val form = vm.uiState.value.form
    assertEquals(repeated.toLocalDate(), form.startDate)
    assertEquals(repeated.toLocalTime(), form.startTime)
    assertEquals(firstOccurrence, form.startDateTime)
    assertEquals(secondOccurrence.minusHours(1).toInstant(), form.startDateTime?.toInstant())
    assertFalse(vm.uiState.value.hasChanges)

    vm.updateTitle("Run power to the main stage")
    vm.save()

    assertEquals(
        firstOccurrence.toInstant(),
        runBlocking { local.getTask("task-4") }.startTime?.toInstant(),
    )
  }

  // Breaks if hasChanges does not trim the texts, so a trailing space counts as a change.
  @Test
  fun aTrailingSpaceIsNotAChange() {
    viewModel.load()

    viewModel.updateTitle("Run power to stage ")

    assertFalse(viewModel.uiState.value.hasChanges)
  }

  // Breaks if a failed load leaves the screen loading or shows no error.
  @Test
  fun aMissingTaskReportsTheLoadError() {
    val vm = EditTaskViewModel("missing", { "owner-1" }, repository, zone = zone)

    vm.load()

    val state = vm.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.task)
    assertEquals(R.string.edit_task_error_load_failed, state.errorMsg)
    assertFalse(state.canSave)
  }

  // Breaks if an open picker counts as a change.
  @Test
  fun anUntouchedLoadedFormHasNoChangesAndCannotSave() {
    viewModel.load()
    viewModel.showPicker(DateTimeField.START_DATE)

    val state = viewModel.uiState.value
    assertFalse(state.hasChanges)
    assertFalse(state.needsDiscardConfirmation)
    assertFalse(state.canSave)
  }

  // Breaks if a setter writes the wrong field, or hasChanges ignores a field or keeps a stale flag.
  @Test
  fun editingAFieldThenRevertingItLeavesNoChanges() {
    viewModel.load()
    fun changed() = viewModel.uiState.value.hasChanges

    viewModel.updateTitle("Something else")
    assertTrue(changed())
    assertTrue(viewModel.uiState.value.needsDiscardConfirmation)
    viewModel.updateTitle("Run power to stage")
    assertFalse(changed())

    viewModel.updateDescription("Red crate")
    assertTrue(changed())
    viewModel.updateDescription("Blue crate")
    assertFalse(changed())

    viewModel.updateLocation("Bar")
    assertTrue(changed())
    viewModel.updateLocation("Stage")
    assertFalse(changed())

    viewModel.updatePriority(Priority.LOW)
    assertTrue(changed())
    viewModel.updatePriority(Priority.HIGH)
    assertFalse(changed())

    viewModel.incrementMaxAssign()
    assertTrue(changed())
    viewModel.decrementMaxAssign()
    assertFalse(changed())

    viewModel.updateEstimatedMinutes("6x0")
    assertEquals("60", viewModel.uiState.value.form.estimatedMinutes)
    assertTrue(changed())
    viewModel.updateEstimatedMinutes("45")
    assertFalse(changed())

    viewModel.showPicker(DateTimeField.START_DATE)
    viewModel.pickDate(day.minusDays(1))
    assertEquals(day.minusDays(1), viewModel.uiState.value.form.startDate)
    assertNull(viewModel.uiState.value.form.openPicker)
    assertTrue(changed())
    viewModel.updateStartDate(day)
    assertFalse(changed())

    viewModel.showPicker(DateTimeField.START_TIME)
    viewModel.pickTime(LocalTime.of(8, 0))
    assertTrue(changed())
    viewModel.updateStartTime(LocalTime.of(9, 0))
    assertFalse(changed())

    viewModel.showPicker(DateTimeField.END_DATE)
    viewModel.pickDate(day.plusDays(1))
    assertTrue(changed())
    viewModel.updateEndDate(day)
    assertFalse(changed())

    viewModel.showPicker(DateTimeField.END_TIME)
    viewModel.pickTime(LocalTime.of(11, 0))
    assertTrue(changed())
    viewModel.updateEndTime(LocalTime.of(10, 30))
    assertFalse(changed())

    // A date picked with a time picker open, and the reverse, only close the picker.
    viewModel.showPicker(DateTimeField.START_TIME)
    viewModel.pickDate(day.plusDays(3))
    viewModel.showPicker(DateTimeField.START_DATE)
    viewModel.pickTime(LocalTime.NOON)
    assertFalse(changed())
    assertNull(viewModel.uiState.value.form.openPicker)
  }

  // Breaks if maxAssignError is missing, uses the wrong bound, or canSave ignores it.
  @Test
  fun maxAssignBelowTheAssigneesShowsTooFewSlotsAndBlocksSave() {
    viewModel.load()

    viewModel.decrementMaxAssign()
    assertEquals(2, viewModel.uiState.value.form.maxAssign)
    assertNull(viewModel.uiState.value.maxAssignError)
    assertTrue(viewModel.uiState.value.canSave)

    viewModel.decrementMaxAssign()
    assertEquals(1, viewModel.uiState.value.form.maxAssign)
    assertEquals(FieldError.TOO_FEW_SLOTS, viewModel.uiState.value.maxAssignError)
    assertFalse(viewModel.uiState.value.canSave)

    viewModel.save()
    assertEquals(0, repository.edits)
    assertEquals(3, stored().maxAssign)
  }

  // Breaks if canSave or save() does not check that the signed-in user owns the task.
  @Test
  fun aNonOwnerCannotSave() {
    val vm = EditTaskViewModel("task-1", { "someone-else" }, repository, zone = zone)
    vm.load()
    vm.updateTitle("Hijacked")

    assertTrue(vm.uiState.value.hasChanges)
    assertFalse(vm.uiState.value.isOwner())
    assertFalse(vm.uiState.value.canSave)

    vm.save()
    assertEquals(0, repository.edits)
    assertEquals(seeded, stored())
  }

  // Breaks if toTask drops a field the form does not own (assignees, completion, voice note) or
  // writes a field the user edited with its old value.
  @Test
  fun savingWritesTheEditedTaskAndKeepsAssigneesCompletionAndVoiceNote() {
    runBlocking { local.editTask("task-1", seeded.complete()) }
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")
    viewModel.updateLocation("Main stage")
    viewModel.updatePriority(Priority.URGENT)
    viewModel.updateEstimatedMinutes("90")
    viewModel.updateEndTime(LocalTime.of(11, 0))

    viewModel.save()

    val saved = stored()
    assertEquals("Run power to the main stage", saved.title)
    assertEquals("Main stage", saved.location)
    assertEquals(Priority.URGENT, saved.priority)
    assertEquals(Duration.ofMinutes(90), saved.estimatedTime)
    assertEquals(ZonedDateTime.of(day, LocalTime.of(11, 0), zone), saved.endTime)
    assertEquals(seeded.startTime, saved.startTime)
    assertEquals(seeded.assignees, saved.assignees)
    assertTrue(saved.completed)
    assertEquals(voiceNote, saved.voiceNote)
    assertEquals("owner-1", saved.ownerId)
    assertEquals("event-1", saved.eventId)
    val state = viewModel.uiState.value
    assertTrue(state.isSaved)
    assertFalse(state.isSaving)
    assertEquals(saved, state.task)
    assertFalse(state.needsDiscardConfirmation)
  }

  // Breaks if save() does not set isSaving before the write, so a second tap writes twice.
  @Test
  fun aSecondTapWhileSavingIsIgnored() {
    val gate = CompletableDeferred<Unit>()
    repository.editGate = gate
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")

    viewModel.save()
    assertTrue(viewModel.uiState.value.isSaving)
    assertFalse(viewModel.uiState.value.canSave)
    viewModel.save()
    gate.complete(Unit)
    idle()

    assertEquals(1, repository.edits)
    assertTrue(viewModel.uiState.value.isSaved)
    assertEquals("Run power to the main stage", stored().title)
  }

  // Breaks if a failed write is shown as saved, the error is missing, the form is reset, or the
  // failure is not checked by reading the task back.
  @Test
  fun aFailingRepositoryReportsTheSaveErrorAndKeepsTheForm() {
    repository.editFailure = IllegalStateException("offline")
    viewModel.load()
    val readsAfterLoad = repository.reads
    repository.readFailure = IllegalStateException("still offline")
    viewModel.updateTitle("Run power to the main stage")

    viewModel.save()

    val state = viewModel.uiState.value
    assertEquals(R.string.edit_task_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertEquals("Run power to the main stage", state.form.title)
    assertTrue(state.canSave)
    assertEquals(1, repository.reads - readsAfterLoad)
    assertEquals(seeded, stored())
  }

  // Breaks if the write or the read-back is not bounded by saveTimeoutMs, so a hanging save stays
  // saving forever.
  @Test
  fun aSaveThatHangsTimesOutAndKeepsTheForm() {
    repository.editGate = CompletableDeferred()
    val vm = loadedViewModel(saveTimeoutMs = 1_000)
    repository.readGate = CompletableDeferred()
    vm.updateTitle("Run power to the main stage")

    vm.save()
    assertTrue(vm.uiState.value.isSaving)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))

    val state = vm.uiState.value
    assertEquals(R.string.edit_task_error_save_failed, state.errorMsg)
    assertFalse(state.isSaving)
    assertEquals("Run power to the main stage", state.form.title)
    assertTrue(state.canSave)
  }

  // Breaks if the read-back after a timeout is removed or compares the wrong value.
  @Test
  fun aTimeoutAfterTheWriteLandedEndsAsSaved() {
    repository.editGate = CompletableDeferred()
    repository.landsFirst = true
    val vm = loadedViewModel(saveTimeoutMs = 1_000)
    vm.updateTitle("Run power to the main stage")

    vm.save()
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))

    val state = vm.uiState.value
    assertTrue(state.isSaved)
    assertFalse(state.isSaving)
    assertNull(state.errorMsg)
    assertEquals("Run power to the main stage", state.task?.title)
  }

  // Breaks if a cancellation is caught as a failure (error shown) or leaves the form saving.
  @Test
  fun aCancelledSaveClearsSavingAndShowsNoError() {
    repository.editFailure = CancellationException("cancelled")
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")

    viewModel.save()

    val state = viewModel.uiState.value
    assertFalse(state.isSaving)
    assertFalse(state.isSaved)
    assertNull(state.errorMsg)
  }

  // Breaks if the edit guard lets changes in after a save, or canSave ignores isSaved.
  @Test
  fun editsAfterASaveAreIgnoredAndTheFormCannotSaveAgain() {
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")
    viewModel.save()

    viewModel.updateTitle("Something else")
    viewModel.save()

    assertEquals("Run power to the main stage", viewModel.uiState.value.form.title)
    assertFalse(viewModel.uiState.value.canSave)
    assertEquals(1, repository.edits)
  }

  // Breaks if clearError does not reset the message the snackbar shows.
  @Test
  fun clearErrorRemovesTheMessage() {
    val vm = EditTaskViewModel("missing", { "owner-1" }, repository, zone = zone)
    vm.load()
    assertEquals(R.string.edit_task_error_load_failed, vm.uiState.value.errorMsg)

    vm.clearError()

    assertNull(vm.uiState.value.errorMsg)
  }

  // Breaks if the init check on taskId is removed.
  @Test
  fun aBlankTaskIdFailsFast() {
    assertThrows(IllegalArgumentException::class.java) {
      EditTaskViewModel(" ", { "owner-1" }, repository)
    }
  }

  // Breaks if the guard lets a picker open before load or after save.
  @Test
  fun showPickerDoesNothingBeforeLoadOrAfterSave() {
    viewModel.showPicker(DateTimeField.START_DATE)
    assertNull(viewModel.uiState.value.form.openPicker)

    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")
    viewModel.save()
    assertTrue(viewModel.uiState.value.isSaved)

    viewModel.showPicker(DateTimeField.START_DATE)
    assertNull(viewModel.uiState.value.form.openPicker)
  }

  // Breaks if a saved edit rebuilds the task from the form and drops when each assignee took it.
  @Test
  fun savingKeepsWhenEachAssigneeTookTheTask() {
    val takenAt = mapOf("me" to ZonedDateTime.of(day, LocalTime.of(7, 0), zone))
    runBlocking { local.editTask("task-1", seeded.copy(takenAt = takenAt)) }
    viewModel.load()
    viewModel.updateTitle("Run power to the main stage")

    viewModel.save()

    assertEquals(takenAt, stored().takenAt)
  }
}

/** Tests of the rules of [EditTaskUiState], without a ViewModel. */
class EditTaskUiStateTest {
  private val zone = ZoneId.of("Europe/Zurich")
  // Tomorrow, like the picker tests of #371: the form refuses an end in the past.
  private val day = LocalDate.now(zone).plusDays(1)
  private val bare =
      Task(taskId = "task-1", eventId = "event-1", title = "Run", ownerId = "owner-1")
  private val untouched =
      EditTaskUiState(
          task = bare,
          form = CreateTaskUiState(title = "Run", zone = zone),
          isOwner = { true },
          isLoading = false,
      )
  private val edited = untouched.copy(form = untouched.form.copy(location = "Stage"))

  // Breaks if a task without times or estimate is not mapped to empty fields.
  @Test
  fun aTaskWithoutTimesOrEstimateMatchesAnEmptyForm() {
    assertFalse(untouched.hasChanges)
    assertTrue(edited.hasChanges)
    assertTrue(edited.canSave)
  }

  // Breaks if a state without a task reports changes or builds a task.
  @Test
  fun aStateWithoutATaskHasNoChangesAndBuildsNoTask() {
    val empty = EditTaskUiState()

    assertFalse(empty.hasChanges)
    assertNull(empty.maxAssignError)
    assertFalse(empty.canSave)
    assertThrows(IllegalArgumentException::class.java) { empty.toTask() }
  }

  // Breaks if canSave misses one of the form rules of the Create task form.
  @Test
  fun eachInvalidFieldBlocksSave() {
    val invalid =
        listOf(
            edited.form.copy(title = " ", titleEdited = true),
            edited.form.copy(startTime = LocalTime.NOON),
            edited.form.copy(startDate = day, endDate = day.minusDays(1)),
            edited.form.copy(estimatedMinutes = "0"),
        )

    invalid.forEach { form ->
      val state = edited.copy(form = form)
      assertFalse(state.canSave)
      assertThrows(IllegalArgumentException::class.java) { state.toTask() }
    }
  }

  // Breaks if canSave lets a running or finished save start again.
  @Test
  fun aSavingOrSavedStateCannotSave() {
    assertFalse(edited.copy(isSaving = true).canSave)
    assertFalse(edited.copy(isSaved = true).canSave)
    assertFalse(edited.copy(isSaved = true).needsDiscardConfirmation)
  }
}
