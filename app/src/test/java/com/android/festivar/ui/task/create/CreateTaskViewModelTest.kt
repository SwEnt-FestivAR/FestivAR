// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

@RunWith(AndroidJUnit4::class)
class CreateTaskViewModelTest {
  // FAKE: a real, working repository that keeps the tasks in memory instead of Firebase. The tests
  // read the saved tasks back from it. There is no mock in this file: nothing checks that a method
  // was called, the tests check the state and what was stored.
  private val repository = TasksRepositoryLocal()
  private val viewModel = CreateTaskViewModel(repository)
  private val day = LocalDate.of(2026, 10, 17)

  private fun savedTasks(): List<Task> = runBlocking { repository.getAllTasks("event-1") }

  @Test
  fun updates_areReflectedInState() {
    viewModel.updateTitle("Run power to stage")
    viewModel.updateDescription("Blue crate")
    viewModel.updateLocation("Stage")
    viewModel.updateStartDate(day)
    viewModel.updateStartTime(LocalTime.of(9, 30))
    viewModel.updateEndDate(day)
    viewModel.updateEndTime(LocalTime.of(9, 50))

    val state = viewModel.uiState.value
    assertEquals("Run power to stage", state.title)
    assertEquals("Blue crate", state.description)
    assertEquals("Stage", state.location)
    assertEquals(LocalDateTime.of(2026, 10, 17, 9, 30), state.startDateTime)
    assertEquals(LocalDateTime.of(2026, 10, 17, 9, 50), state.endDateTime)
  }

  @Test
  fun createTask_savesTaskInTheEvent() {
    viewModel.updateTitle("Run power to stage")
    viewModel.updateLocation("Stage, north corner")

    viewModel.createTask("event-1")

    val saved = savedTasks().single()
    assertEquals("Run power to stage", saved.title)
    assertEquals("Stage, north corner", saved.location)
    assertEquals("event-1", saved.eventId)
    assertTrue(viewModel.uiState.value.isCreated)
    assertFalse(viewModel.uiState.value.isSaving)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun createTask_withBlankTitle_savesNothing() {
    viewModel.createTask("event-1")

    assertTrue(savedTasks().isEmpty())
    assertFalse(viewModel.uiState.value.isCreated)
  }

  @Test
  fun createTask_withEndBeforeStart_savesNothing() {
    viewModel.updateTitle("Run power to stage")
    viewModel.updateStartDate(day)
    viewModel.updateEndDate(day.minusDays(1))

    viewModel.createTask("event-1")

    assertTrue(savedTasks().isEmpty())
    assertEquals(FieldError.END_BEFORE_START, viewModel.uiState.value.endDateError)
  }

  @Test
  fun titleError_appearsOnlyAfterTheTitleIsEditedToBlank() {
    assertNull(viewModel.uiState.value.titleError)

    viewModel.updateTitle("Run power to stage")
    assertNull(viewModel.uiState.value.titleError)

    viewModel.updateTitle("")
    assertEquals(FieldError.EMPTY_TITLE, viewModel.uiState.value.titleError)

    viewModel.updateTitle("Run")
    assertNull(viewModel.uiState.value.titleError)
  }

  @Test
  fun timeWithoutDate_putsTheErrorUnderTheMatchingDate() {
    viewModel.updateEndTime(LocalTime.of(9, 50))

    assertEquals(FieldError.MISSING_DATE, viewModel.uiState.value.endDateError)
    assertNull(viewModel.uiState.value.startDateError)
  }

  @Test
  fun createTask_whenRepositoryFails_exposesErrorAndAllowsRetry() {
    // STUB: a fake whose addTask always fails with a fixed error, to test how the ViewModel reacts.
    val failing =
        object : TasksRepository by TasksRepositoryLocal() {
          override suspend fun addTask(task: Task) = throw IllegalStateException("offline")
        }
    val vm = CreateTaskViewModel(failing)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")

    val state = vm.uiState.value
    assertEquals("Unable to create the task.", state.errorMsg)
    assertFalse(state.isCreated)
    assertFalse(state.isSaving)
    assertTrue(state.canCreate)

    vm.clearError()
    assertNull(vm.uiState.value.errorMsg)
  }

  // A second tap while the first save is still running must not save the task twice. The fake
  // repository waits on a gate, so the ViewModel stays in the saving state in between.
  @Test
  fun createTask_whileASaveIsRunning_isIgnored() {
    val gate = CompletableDeferred<Unit>()
    val local = TasksRepositoryLocal()
    var addCalls = 0
    // FAKE with a gate: it saves like TasksRepositoryLocal, but only once the test opens the gate,
    // so the ViewModel stays "saving". It also counts the calls to addTask (a hand-written spy).
    val slow =
        object : TasksRepository by local {
          override suspend fun addTask(task: Task) {
            addCalls++
            gate.await()
            local.addTask(task)
          }
        }
    val vm = CreateTaskViewModel(slow)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")
    assertTrue(vm.uiState.value.isSaving)
    vm.createTask("event-1")
    gate.complete(Unit)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(1, addCalls)
    assertEquals(1, runBlocking { local.getAllTasks("event-1") }.size)
    assertTrue(vm.uiState.value.isCreated)
    assertFalse(vm.uiState.value.isSaving)
  }

  // Typing while the save is running must not change the task that is saved: it is built from the
  // form as it was when "Create task" was pressed.
  @Test
  fun createTask_editsDuringTheSave_doNotChangeTheSavedTask() {
    val gate = CompletableDeferred<Unit>()
    val local = TasksRepositoryLocal()
    // FAKE with a gate: it saves like TasksRepositoryLocal, but only once the test opens the gate.
    val slow =
        object : TasksRepository by local {
          override suspend fun addTask(task: Task) {
            gate.await()
            local.addTask(task)
          }
        }
    val vm = CreateTaskViewModel(slow)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")
    vm.updateTitle("Something else")
    gate.complete(Unit)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals("Run power to stage", runBlocking { local.getAllTasks("event-1") }.single().title)
  }

  // After a failure the user can press "Create task" again: the repository now works, so the task
  // is saved once and the old error is gone.
  @Test
  fun createTask_afterAFailure_succeedsOnRetryAndClearsTheError() {
    val local = TasksRepositoryLocal()
    var failing = true
    // FAKE that can fail on demand: it works like TasksRepositoryLocal, but fails while `failing`
    // is true. The test turns it off to simulate a retry that works.
    val flaky =
        object : TasksRepository by local {
          override suspend fun addTask(task: Task) {
            if (failing) throw IllegalStateException("offline")
            local.addTask(task)
          }
        }
    val vm = CreateTaskViewModel(flaky)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")
    assertEquals("Unable to create the task.", vm.uiState.value.errorMsg)

    failing = false
    vm.createTask("event-1")

    val state = vm.uiState.value
    assertNull(state.errorMsg)
    assertTrue(state.isCreated)
    assertFalse(state.isSaving)
    assertEquals(1, runBlocking { local.getAllTasks("event-1") }.size)
  }

  // A cancelled save (for example when the ViewModel is cleared) is not a failure of the user's
  // task, so no error message is shown for it.
  @Test
  fun createTask_whenTheSaveIsCancelled_doesNotReportAnError() {
    // STUB: its addTask always throws a CancellationException, as when the coroutine is cancelled.
    val cancelled =
        object : TasksRepository by TasksRepositoryLocal() {
          override suspend fun addTask(task: Task) = throw CancellationException("cancelled")
        }
    val vm = CreateTaskViewModel(cancelled)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")

    assertNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isCreated)
    assertFalse(vm.uiState.value.isSaving)
  }

  // The save reached the repository but reported a failure (for example a timeout after the
  // commit). The retry keeps the same task id and finds the task, so nothing is saved twice.
  @Test
  fun createTask_whenTheFailedSaveWasStored_retryDoesNotDuplicate() {
    val local = TasksRepositoryLocal()
    var failing = true
    val storedThenFailed =
        object : TasksRepository by local {
          override suspend fun addTask(task: Task) {
            local.addTask(task)
            if (failing) throw IllegalStateException("lost the answer")
          }
        }
    val vm = CreateTaskViewModel(storedThenFailed)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")
    failing = false
    vm.createTask("event-1")

    assertEquals(1, runBlocking { local.getAllTasks("event-1") }.size)
    assertTrue(vm.uiState.value.isCreated)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun createTask_whenTheSaveNeverEnds_timesOutAndAllowsRetry() {
    val never = CompletableDeferred<Unit>()
    val hanging =
        object : TasksRepository by TasksRepositoryLocal() {
          override suspend fun addTask(task: Task) = never.await()
        }
    val vm = CreateTaskViewModel(hanging, saveTimeoutMs = 1_000)
    vm.updateTitle("Run power to stage")

    vm.createTask("event-1")
    assertTrue(vm.uiState.value.isSaving)
    shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5))

    assertEquals("Unable to create the task.", vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSaving)
    assertTrue(vm.uiState.value.canCreate)
  }

  @Test
  fun updates_afterTheTaskIsCreated_areIgnored() {
    viewModel.updateTitle("Run power to stage")
    viewModel.createTask("event-1")

    viewModel.updateTitle("Something else")

    assertEquals("Run power to stage", viewModel.uiState.value.title)
  }

  @Test
  fun createTask_afterASuccess_isIgnored() {
    viewModel.updateTitle("Run power to stage")

    viewModel.createTask("event-1")
    viewModel.createTask("event-1")

    assertEquals(1, savedTasks().size)
  }
}

class CreateTaskUiStateTest {
  private val day = LocalDate.of(2026, 10, 17)
  private val valid = CreateTaskUiState(title = "Run power to stage")

  @Test
  fun emptyForm_cannotBeCreated() {
    assertFalse(CreateTaskUiState().canCreate)
  }

  @Test
  fun blankTitle_cannotBeCreated() {
    assertFalse(valid.copy(title = "   ").canCreate)
  }

  @Test
  fun titleOnly_canBeCreated() {
    assertTrue(valid.canCreate)
    assertNull(valid.titleError)
    assertNull(valid.startDateError)
    assertNull(valid.endDateError)
  }

  @Test
  fun whileSaving_cannotBeCreatedAgain() {
    assertFalse(valid.copy(isSaving = true).canCreate)
  }

  @Test
  fun dateWithoutTime_startsAtMidnight() {
    assertEquals(day.atStartOfDay(), valid.copy(startDate = day).startDateTime)
  }

  @Test
  fun timeWithoutDate_isAnError() {
    // The error sits under the date that is missing, and nowhere else.
    val missingStartDate = valid.copy(startTime = LocalTime.of(9, 30))
    assertEquals(FieldError.MISSING_DATE, missingStartDate.startDateError)
    assertNull(missingStartDate.endDateError)
    assertFalse(missingStartDate.canCreate)

    val missingEndDate = valid.copy(endTime = LocalTime.of(9, 50))
    assertEquals(FieldError.MISSING_DATE, missingEndDate.endDateError)
    assertNull(missingEndDate.startDateError)
    assertFalse(missingEndDate.canCreate)
  }

  @Test
  fun endBeforeStart_isAnError() {
    val state =
        valid.copy(
            startDate = day,
            startTime = LocalTime.of(9, 30),
            endDate = day,
            endTime = LocalTime.of(9, 29),
        )
    assertEquals(FieldError.END_BEFORE_START, state.endDateError)
    assertNull(state.startDateError)
    assertFalse(state.canCreate)
  }

  @Test
  fun endEqualToStart_isValid() {
    val state =
        valid.copy(
            startDate = day,
            startTime = LocalTime.of(9, 30),
            endDate = day,
            endTime = LocalTime.of(9, 30),
        )
    assertNull(state.endDateError)
  }

  // A date without a time means the start of that day, also for the end: this end is at 00:00,
  // before the start at 09:00.
  @Test
  fun endDateWithoutTime_onTheStartDay_isBeforeAStartWithATime() {
    val state = valid.copy(startDate = day, startTime = LocalTime.of(9, 0), endDate = day)
    assertEquals(FieldError.END_BEFORE_START, state.endDateError)
  }

  @Test
  fun onlyOneBoundSet_isValid() {
    assertNull(valid.copy(endDate = day).endDateError)
  }

  @Test
  fun untouchedTitle_hasNoError() {
    assertNull(CreateTaskUiState().titleError)
  }

  @Test
  fun editedBlankTitle_hasAnError() {
    val state = CreateTaskUiState(title = "  ", titleEdited = true)
    assertEquals(FieldError.EMPTY_TITLE, state.titleError)
    assertFalse(state.canCreate)
  }

  @Test
  fun editedFilledTitle_hasNoError() {
    assertNull(CreateTaskUiState(title = "Run power", titleEdited = true).titleError)
  }

  @Test
  fun toTask_copiesAndTrimsFields() {
    val task =
        valid
            .copy(
                title = "  Run power to stage ",
                description = " Extension reel is in the blue crate. ",
                location = " Stage, north corner ",
                startDate = day,
                startTime = LocalTime.of(9, 30),
                endDate = day,
                endTime = LocalTime.of(9, 50),
            )
            .toTask(taskId = "t1", eventId = "e1")

    assertEquals("t1", task.taskId)
    assertEquals("e1", task.eventId)
    assertEquals("Run power to stage", task.title)
    assertEquals("Extension reel is in the blue crate.", task.description)
    assertEquals("Stage, north corner", task.location)
    assertEquals(LocalDateTime.of(2026, 10, 17, 9, 30), task.startTime)
    assertEquals(LocalDateTime.of(2026, 10, 17, 9, 50), task.endTime)
  }

  @Test
  fun toTask_withoutSchedule_leavesTimesNull() {
    val task = valid.toTask("t1", "e1")
    assertNull(task.startTime)
    assertNull(task.endTime)
  }

  @Test
  fun toTask_withBlankTitle_isRejected() {
    assertThrows(IllegalArgumentException::class.java) { CreateTaskUiState().toTask("t1", "e1") }
  }

  @Test
  fun toTask_withTimeWithoutDate_isRejected() {
    val form = valid.copy(startTime = LocalTime.of(9, 30))

    assertThrows(IllegalArgumentException::class.java) { form.toTask("t1", "e1") }
  }

  @Test
  fun createdForm_cannotBeCreatedAgain() {
    assertFalse(valid.copy(isCreated = true).canCreate)
  }

  // The form refuses an end before the start, so the conversion is rejected too.
  @Test
  fun toTask_withEndBeforeStart_isRejected() {
    val form = valid.copy(startDate = day, endDate = day.minusDays(1))

    assertThrows(IllegalArgumentException::class.java) { form.toTask("t1", "e1") }
  }
}
