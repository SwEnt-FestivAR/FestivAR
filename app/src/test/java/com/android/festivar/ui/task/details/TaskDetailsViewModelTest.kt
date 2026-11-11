// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.task.details

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.ZonedDateTime
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

  /** How many times [getTask] was called. */
  var getCalls = 0

  override suspend fun getTask(taskId: String): Task {
    getCalls++
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
  private val events = EventsRepositoryLocal()
  private val users = UsersRepositoryLocal()
  private val dataRepository =
      DataRepositoryLocal(
          authRepository = mockk(),
          tasksRepository = repository,
          eventsRepository = events,
          usersRepository = users,
      )
  private val mockDataRepository =
      mockk<DataRepository>(relaxed = true) {
        every { tasksRepository } returns repository
        coEvery { joinTask(any(), any()) } coAnswers
            {
              dataRepository.joinTask(firstArg(), secondArg())
            }
        coEvery { quitTask(any(), any()) } coAnswers
            {
              dataRepository.quitTask(firstArg(), secondArg())
            }
        coEvery { deleteTask(any()) } coAnswers { dataRepository.deleteTask(firstArg()) }
      }
  private val me = User(uid = "me")
  private val other = User(uid = "other")
  private val start = ZonedDateTime.parse("2026-07-01T14:00:00+02:00")

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /**
   * Stores [task], its event and [user], consistent with each other: the user lists the task when
   * they are one of its assignees. Then creates the view model and loads the task.
   */
  private fun viewModel(
      task: Task,
      user: User = me,
      repo: DataRepository = dataRepository,
  ): TaskDetailsViewModel {
    runBlocking {
      repository.local.addTask(task)
      events.addEvent(
          Event(
              eventId = task.eventId,
              ownerId = "owner",
              title = "Summer festival",
              tasks = listOf(task),
              startDate = start,
              endDate = start.plusHours(10),
          )
      )
      val assigned = task.assignees.any { it.uid == user.uid }
      users.addUser(if (assigned) user.copy(tasksAssigned = listOf(task.taskId)) else user)
    }
    return TaskDetailsViewModel(task.taskId, user, repo).also { it.load() }
  }

  private fun task(
      assignees: List<User> = emptyList(),
      maxAssign: Int = 1,
      completed: Boolean = false,
  ) =
      Task(
          ownerId = "owner-1",
          taskId = "t1",
          eventId = "e1",
          title = "Run power to stage",
          maxAssign = maxAssign,
          assignees = assignees,
          completed = completed,
      )

  private fun stored(): Task = runBlocking { repository.local.getTask("t1") }

  private fun storedUser(): User = runBlocking { users.getUser(me.uid) }

  private fun taskInEvent(): Task = runBlocking { events.getEvent("e1").tasks.single() }

  // State of the screen

  // Breaks if canEdit compares the owner with the wrong id, or lets anyone edit.
  @Test
  fun canEditIsTrueOnlyForTheOwner() {
    assertTrue(viewModel(task(), User(uid = "owner-1")).uiState.value.canEdit)
    // An assignee who is not the owner, and the owner before any task is loaded.
    assertFalse(TaskDetailsUiState(me.uid, task(assignees = listOf(me))).canEdit)
    assertFalse(TaskDetailsUiState(currentUserId = "owner-1").canEdit)
  }

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
    val vm = TaskDetailsViewModel("missing", me, dataRepository).also { it.load() }
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

  // Breaks if the view model reads the task at construction again, so the screen's first resume
  // reads it twice on open.
  @Test
  fun nothingIsReadBeforeLoad() {
    runBlocking { repository.local.addTask(task()) }
    val vm = TaskDetailsViewModel("t1", me, dataRepository)

    assertEquals(0, repository.getCalls)
    assertNull(vm.uiState.value.task)

    vm.load()

    assertEquals(1, repository.getCalls)
  }

  // Breaks if a reload on resume clears a save error the user has not dismissed yet.
  @Test
  fun reloadAfterAFailedSave_keepsTheSaveError() {
    val vm = viewModel(task())
    repository.editFailure = IOException("no network")
    vm.take()

    vm.load()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
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

    // The save reads the task again once stored, so the network is back by then.
    repository.getFailure = null
    repository.editGate!!.complete(Unit)

    assertEquals(TaskStatus.YOURS, vm.uiState.value.status)
  }

  @Test
  fun reloadWhileLoading_isIgnored() {
    repository.getGate = CompletableDeferred()
    val vm = viewModel(task())

    vm.load()
    repository.getGate!!.complete(Unit)

    assertEquals(1, repository.getCalls)
    assertNull(vm.uiState.value.errorMsg)
    assertEquals(TaskStatus.OPEN, vm.uiState.value.status)
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

  // Breaks if markDone() reads the task again: it already knows the completed version.
  @Test
  fun markDone_showsTheCompletedTaskWithoutReadingItAgain() {
    val vm = viewModel(task(assignees = listOf(me)))
    val readsBefore = repository.getCalls
    vm.markDone()
    assertEquals(readsBefore, repository.getCalls)
    assertEquals(stored(), vm.uiState.value.task)
  }

  // Breaks if take() edits only the task instead of going through dataRepository.joinTask.
  @Test
  fun take_addsTheTaskToTheUserAndUpdatesItInItsEvent() {
    val vm = viewModel(task())
    vm.take()
    assertEquals(listOf("t1"), storedUser().tasksAssigned)
    assertEquals(listOf(me.uid), taskInEvent().assignees.map { it.uid })
    assertEquals(stored(), vm.uiState.value.task)
  }

  // Breaks if giveBack() edits only the task instead of going through dataRepository.quitTask.
  @Test
  fun giveBack_removesTheTaskFromTheUserAndUpdatesItInItsEvent() {
    val vm = viewModel(task(assignees = listOf(other, me), maxAssign = 2))
    vm.giveBack()
    assertTrue(storedUser().tasksAssigned.isEmpty())
    assertEquals(listOf(other.uid), taskInEvent().assignees.map { it.uid })
    assertEquals(stored(), vm.uiState.value.task)
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

  // Breaks if the IllegalStateException of a task filled meanwhile is not shown as a save error.
  @Test
  fun takingATaskFilledMeanwhile_showsTheSaveError() {
    val vm = viewModel(task())
    runBlocking { repository.local.editTask("t1", task(assignees = listOf(other))) }

    vm.take()

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(listOf(other), stored().assignees)
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

  @Test
  fun deleteTask_whileLoadingDoesNothing() {
    repository.getGate = CompletableDeferred()
    val vm = TaskDetailsViewModel("t1", User(uid = "owner-1"), mockDataRepository)

    vm.load()
    vm.deleteTask()

    coVerify(exactly = 0) { mockDataRepository.deleteTask(any()) }
    assertTrue(vm.uiState.value.isLoading)
    assertFalse(vm.uiState.value.isUpdating)
  }

  @Test
  fun deleteTask_whileSavingDoesNothing() {
    repository.editGate = CompletableDeferred()
    val vm = viewModel(task(), User(uid = "owner-1"), mockDataRepository)
    vm.take()

    vm.deleteTask()

    coVerify(exactly = 0) { mockDataRepository.deleteTask(any()) }
    assertTrue(vm.uiState.value.isUpdating)
  }

  @Test
  fun deleteTask_deletesThroughDataRepository() {
    val vm = viewModel(task(), User(uid = "owner-1"), mockDataRepository)
    var deletionCount = 0

    vm.deleteTask { deletionCount++ }

    coVerify(exactly = 1) { mockDataRepository.deleteTask("t1") }
    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
    assertEquals(1, deletionCount)
  }

  @Test
  fun deleteTask_failureShowsError() {
    val vm = viewModel(task(), User(uid = "owner-1"), mockDataRepository)
    var deletionCount = 0
    coEvery { mockDataRepository.deleteTask("t1") } throws IOException("offline")

    vm.deleteTask { deletionCount++ }

    assertEquals(R.string.task_details_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isUpdating)
    assertEquals(0, deletionCount)
  }

  @Test
  fun deleteTask_cancelledResetStateWithoutError() {
    val vm = viewModel(task(), User(uid = "owner-1"), mockDataRepository)
    coEvery { mockDataRepository.deleteTask("t1") } throws CancellationException("cancelled")

    vm.deleteTask()

    assertFalse(vm.uiState.value.isUpdating)
    assertNull(vm.uiState.value.errorMsg)
  }

  @Test
  fun deleteTask_nonOwnerDoesNothing() {
    val vm = viewModel(task(), repo = mockDataRepository)

    vm.deleteTask()

    coVerify(exactly = 0) { mockDataRepository.deleteTask(any()) }
  }
}
