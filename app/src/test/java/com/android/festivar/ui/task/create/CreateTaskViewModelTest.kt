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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateTaskViewModelTest {
  private val repository = TasksRepositoryLocal()
  private val viewModel = CreateTaskViewModel(eventId = "event-1", tasksRepository = repository)
  private val day = LocalDate.of(2026, 10, 17)

  private fun savedTasks(): List<Task> = runBlocking { repository.getAllTasks() }

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
    assertEquals(ScheduleError.END_BEFORE_START, viewModel.uiState.value.scheduleError)
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
