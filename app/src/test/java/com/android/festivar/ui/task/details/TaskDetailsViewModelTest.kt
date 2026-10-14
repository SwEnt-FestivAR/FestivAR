// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A [TasksRepositoryLocal] whose calls can be made to fail or to wait, to test how the view model
 * behaves while an action is saving or when it fails.
 */
private class ControlledRepository(val local: TasksRepositoryLocal = TasksRepositoryLocal()) :
    TasksRepository by local {
  /** Thrown by [getTask] when not null. */
  var getFailure: Exception? = null

  /** Thrown by [editTask] when not null. */
  var editFailure: Exception? = null

  /** While not null, [getTask] waits for it to complete. */
  var getGate: CompletableDeferred<Unit>? = null

  /** While not null, [editTask] waits for it to complete. */
  var editGate: CompletableDeferred<Unit>? = null

  /** How many times [editTask] was called. */
  var editCalls = 0

  override suspend fun getTask(taskId: String): Task {
    getGate?.await()
    getFailure?.let { throw it }
    return local.getTask(taskId)
  }

  override suspend fun editTask(taskId: String, newValue: Task) {
    editCalls++
    editGate?.await()
    editFailure?.let { throw it }
    local.editTask(taskId, newValue)
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TaskDetailsViewModelTest {
  private val repository = ControlledRepository()
  private val me = User(uid = "me")
  private val other = User(uid = "other")

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(task: Task, user: User = me): TaskDetailsViewModel {
    runBlocking { repository.local.addTask(task) }
    return TaskDetailsViewModel(task.taskId, user, repository)
  }

  private fun task(
      assignees: List<User> = emptyList(),
      maxAssign: Int = 1,
      completed: Boolean = false,
  ) =
      Task(
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          maxAssign = maxAssign,
          assignees = assignees,
          completed = completed,
      )

  private fun stored(): Task = runBlocking { repository.local.getTask("t1") }

  // State of the screen

  @Test
  fun freeTask_isOpenWithTakeAction() {
    val state = viewModel(task()).uiState.value
    assertEquals(TaskStatus.OPEN, state.status)
    assertTrue(state.canTake)
    assertFalse(state.canGiveBack)
    assertFalse(state.canMarkDone)
    assertTrue(state.hasActions)
    assertFalse(state.isLoading)
  }

  @Test
  fun taskFullOfOthers_isTakenWithoutActions() {
    val state = viewModel(task(assignees = listOf(other))).uiState.value
    assertEquals(TaskStatus.TAKEN, state.status)
    assertFalse(state.canTake)
    assertFalse(state.hasActions)
  }

  @Test
  fun taskWithAPlaceLeft_staysOpenEvenIfSomeoneJoined() {
    val state = viewModel(task(assignees = listOf(other), maxAssign = 2)).uiState.value
    assertEquals(TaskStatus.OPEN, state.status)
  }

  @Test
  fun ownTask_isYoursWithGiveBackAndMarkDone() {
    val state = viewModel(task(assignees = listOf(me))).uiState.value
    assertEquals(TaskStatus.YOURS, state.status)
    assertTrue(state.canGiveBack)
    assertTrue(state.canMarkDone)
    assertFalse(state.canTake)
  }

  @Test
  fun yoursStatus_matchesTheUserByUidNotByName() {
    val sameUidOtherName = me.copy(name = "Renamed", surname = "Person")
    val state = viewModel(task(assignees = listOf(sameUidOtherName))).uiState.value
    assertEquals(TaskStatus.YOURS, state.status)
  }

  @Test
  fun completedTask_isDoneWithoutActions() {
    val state = viewModel(task(assignees = listOf(me), completed = true)).uiState.value
    assertEquals(TaskStatus.DONE, state.status)
    assertFalse(state.hasActions)
  }

  // Loading

  @Test
  fun whileLoading_nothingIsShownAndNothingCanBeDone() {
    repository.getGate = CompletableDeferred()
    val vm = viewModel(task())
    assertTrue(vm.uiState.value.isLoading)
    assertNull(vm.uiState.value.task)
    assertNull(vm.uiState.value.status)

    vm.take()
    assertEquals(0, repository.editCalls)

    repository.getGate!!.complete(Unit)
    assertFalse(vm.uiState.value.isLoading)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
  }

  @Test
  fun missingTask_setsErrorAndClearErrorResetsIt() {
    val vm = TaskDetailsViewModel("missing", me, repository)
    assertNull(vm.uiState.value.task)
    assertEquals(R.string.task_details_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
    vm.clearError()
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun reloadAfterFailure_showsTheTaskAndClearsTheError() {
    repository.getFailure = IOException("offline")
    val vm = viewModel(task())
    assertEquals(R.string.task_details_load_failed, vm.uiState.value.errorMsg)
    assertNull(vm.uiState.value.task)

    repository.getFailure = null
    vm.load()

    assertNull(vm.uiState.value.errorMsg)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
  }

  @Test
  fun failedReload_keepsTheTaskShownAndSetsTheError() {
    val vm = viewModel(task())
    repository.getFailure = IOException("offline")

    vm.load()

    assertEquals(R.string.task_details_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
    assertEquals("Run power to stage", vm.uiState.value.task?.title)
  }

  @Test
  fun reloadWhileSaving_isIgnored() {
    repository.editGate = CompletableDeferred()
    val vm = viewModel(task())
    vm.take()
    repository.getFailure = IOException("offline")

    vm.load()

    assertNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)

    repository.editGate!!.complete(Unit)

    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
  }

  @Test
  fun actionsWhileReloading_doNothing() {
    val vm = viewModel(task(assignees = listOf(me), maxAssign = 2))
    repository.getGate = CompletableDeferred()
    vm.load()

    assertTrue(vm.uiState.value.isLoading)
    assertFalse(vm.uiState.value.canGiveBack)
    vm.giveBack()
    vm.markDone()
    assertEquals(0, repository.editCalls)

    repository.getGate!!.complete(Unit)

    assertFalse(vm.uiState.value.isLoading)
    assertTrue(vm.uiState.value.canGiveBack)
  }

  // Actions

  @Test
  fun take_assignsTheCurrentUserAndKeepsTheOthers() {
    val vm = viewModel(task(assignees = listOf(other), maxAssign = 2))
    vm.take()
    assertEquals(listOf(other, me), stored().assignees)
    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
    assertFalse(vm.uiState.value.isUpdating)
  }

  @Test
  fun giveBack_removesOnlyTheCurrentUser() {
    val vm = viewModel(task(assignees = listOf(other, me), maxAssign = 2))
    vm.giveBack()
    assertEquals(listOf(other), stored().assignees)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
  }

  @Test
  fun markDone_completesTheTask() {
    val vm = viewModel(task(assignees = listOf(me)))
    vm.markDone()
    assertTrue(stored().completed)
    assertEquals(TaskStatus.DONE, vm.uiState.value.status)
  }

  @Test
  fun actionsNotAllowedForTheStatus_doNothing() {
    val taken = viewModel(task(assignees = listOf(other)))
    taken.take()
    taken.giveBack()
    taken.markDone()
    assertEquals(TaskStatus.TAKEN, taken.uiState.value.status)
    assertEquals(0, repository.editCalls)
  }

  @Test
  fun openTask_cannotBeGivenBackOrMarkedDone() {
    val vm = viewModel(task())
    vm.giveBack()
    vm.markDone()
    assertEquals(0, repository.editCalls)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun doneTask_cannotBeChangedAnymore() {
    val vm = viewModel(task(assignees = listOf(me), completed = true))
    vm.take()
    vm.giveBack()
    vm.markDone()
    assertEquals(0, repository.editCalls)
  }

  // While an action is saving

  @Test
  fun whileSaving_isUpdatingAndTheOldTaskStaysShownUntilSaved() {
    repository.editGate = CompletableDeferred()
    val vm = viewModel(task())
    vm.take()

    assertTrue(vm.uiState.value.isUpdating)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
    assertFalse(vm.uiState.value.canTake)

    repository.editGate!!.complete(Unit)

    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
  }

  @Test
  fun whileSaving_aSecondTapDoesNothing() {
    repository.editGate = CompletableDeferred()
    val vm = viewModel(task(assignees = listOf(me)))
    vm.markDone()
    vm.markDone()
    vm.giveBack()
    assertEquals(1, repository.editCalls)

    repository.editGate!!.complete(Unit)

    assertEquals(1, repository.editCalls)
    assertEquals(TaskStatus.DONE, vm.uiState.value.status)
  }

  // Failures

  @Test
  fun failedSave_keepsTheOldTaskShowsTheErrorAndCanBeRetried() {
    val vm = viewModel(task())
    repository.editFailure = IOException("no network")
    vm.take()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
    assertTrue(stored().assignees.isEmpty())

    repository.editFailure = null
    vm.take()

    assertNull(vm.uiState.value.errorMsg)
    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
    assertEquals(listOf(me), stored().assignees)
  }

  @Test
  fun failedGiveBack_keepsTheTaskAndShowsTheError() {
    val vm = viewModel(task(assignees = listOf(me)))
    repository.editFailure = IOException("no network")
    vm.giveBack()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
    assertEquals(listOf(me), stored().assignees)
  }

  @Test
  fun failedMarkDone_keepsTheTaskAndShowsTheError() {
    val vm = viewModel(task(assignees = listOf(me)))
    repository.editFailure = IOException("no network")
    vm.markDone()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
    assertFalse(stored().completed)
  }

  @Test
  fun cancelledSave_resetsIsUpdatingWithoutShowingAnError() {
    val vm = viewModel(task())
    repository.editFailure = CancellationException("cancelled")
    vm.take()

    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
  }
}
