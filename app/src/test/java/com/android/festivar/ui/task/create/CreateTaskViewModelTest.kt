// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateTaskViewModelTest {
  private val repository = TasksRepositoryLocal()
  private val viewModel = CreateTaskViewModel(eventId = "event-1", tasksRepository = repository)
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

    viewModel.createTask()

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
    viewModel.createTask()

    assertTrue(savedTasks().isEmpty())
    assertFalse(viewModel.uiState.value.isCreated)
  }

  @Test
  fun createTask_withEndBeforeStart_savesNothing() {
    viewModel.updateTitle("Run power to stage")
    viewModel.updateStartDate(day)
    viewModel.updateEndDate(day.minusDays(1))

    viewModel.createTask()

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
    val failing =
        object : TasksRepository by TasksRepositoryLocal() {
          override suspend fun addTask(task: Task) = throw IllegalStateException("offline")
        }
    val vm = CreateTaskViewModel("event-1", failing)
    vm.updateTitle("Run power to stage")

    vm.createTask()

    val state = vm.uiState.value
    assertEquals("offline", state.errorMsg)
    assertFalse(state.isCreated)
    assertFalse(state.isSaving)
    assertTrue(state.canCreate)

    vm.clearError()
    assertNull(vm.uiState.value.errorMsg)
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
}
