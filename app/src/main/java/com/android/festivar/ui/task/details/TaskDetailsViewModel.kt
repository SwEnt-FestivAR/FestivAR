// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "TaskDetailsViewModel"

/** Shown when the task cannot be loaded. */
private const val LOAD_FAILED_MESSAGE = "Unable to load the task."

/** Shown when an action cannot be saved. */
private const val SAVE_FAILED_MESSAGE = "Unable to save the change."

/** The badge at the top of the Task details screen. */
enum class TaskStatus {
  /** Not done, and a place is left on it. */
  OPEN,

  /** Not done, and every place is held by someone else. */
  TAKEN,

  /** Not done, and the current user holds a place on it. */
  YOURS,

  /** The task is completed. */
  DONE,
}

/**
 * UI state of the Task details screen.
 *
 * @property task the task shown, or null until it is loaded (or if it could not be loaded).
 * @property status the badge of [task]; null while [task] is null.
 * @property isLoading true while the task is being loaded.
 * @property isUpdating true while an action is being saved, so a second tap does nothing.
 *   Actions are also disabled while [isLoading], so a reload cannot overwrite a saved change.
 * @property errorMsg the last failure, or null.
 */
data class TaskDetailsUiState(
    val task: Task? = null,
    val status: TaskStatus? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val errorMsg: String? = null,
) {
  /** The "Take" button of an [TaskStatus.OPEN] task. */
  val canTake: Boolean
    get() = status == TaskStatus.OPEN && !isUpdating && !isLoading

  /** The "Give back" button of a [TaskStatus.YOURS] task. */
  val canGiveBack: Boolean
    get() = status == TaskStatus.YOURS && !isUpdating && !isLoading

  /** The "Mark done" button of a [TaskStatus.YOURS] task. */
  val canMarkDone: Boolean
    get() = status == TaskStatus.YOURS && !isUpdating && !isLoading

  /** Whether the actions bar is shown at all: only open and own tasks have one. */
  val hasActions: Boolean
    get() = status == TaskStatus.OPEN || status == TaskStatus.YOURS
}

/**
 * Holds the state of the Task details screen and applies its three actions: take the task, give it
 * back and mark it done. Each action saves the new version of the task in [repository] and shows it
 * only once saved.
 *
 * @param taskId the task to show.
 * @param currentUser the signed-in user; the screen is only reachable when someone is signed in.
 */
class TaskDetailsViewModel(
    private val taskId: String,
    private val currentUser: User,
    private val repository: TasksRepository = TasksRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(TaskDetailsUiState())
  val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

  init {
    load()
  }

  /**
   * Loads the task; on failure the error is set and the task stays null. Does nothing while an
   * action is saving.
   */
  fun load() {
    if (_uiState.value.isUpdating) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val task = repository.getTask(taskId)
        _uiState.update { show(it.copy(isLoading = false, errorMsg = null), task) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading task $taskId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = LOAD_FAILED_MESSAGE) }
      }
    }
  }

  /**
   * Adds the current user to the task's assignees. Does nothing unless
   * [TaskDetailsUiState.canTake].
   */
  fun take() = update({ it.canTake }) { it.addAssignee(currentUser) }

  /** Removes the current user from the assignees. Does nothing unless the task is yours. */
  fun giveBack() = update({ it.canGiveBack }) { it.removeAssignee(currentUser) }

  /** Completes the task. Does nothing unless the task is yours. */
  fun markDone() = update({ it.canMarkDone }) { it.complete() }

  /** Clears the error message. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  // editTask overwrites the whole task, so two users acting at once can lose a change:
  // https://github.com/SwEnt-FestivAR/FestivAR/issues/186
  private fun update(allowed: (TaskDetailsUiState) -> Boolean, change: (Task) -> Task) {
    // Checking and setting isUpdating in one step, so two taps cannot both start a save.
    val before = _uiState.getAndUpdate {
      if (allowed(it) && it.task != null) it.copy(isUpdating = true, errorMsg = null) else it
    }
    val task = before.task
    if (!allowed(before) || task == null) return
    viewModelScope.launch {
      try {
        val updated = change(task)
        repository.editTask(taskId, updated)
        _uiState.update { show(it.copy(isUpdating = false), updated) }
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error saving task $taskId", e)
        _uiState.update { it.copy(isUpdating = false, errorMsg = SAVE_FAILED_MESSAGE) }
      }
    }
  }

  private fun show(state: TaskDetailsUiState, task: Task) =
      state.copy(task = task, status = statusOf(task))

  private fun statusOf(task: Task): TaskStatus =
      when {
        task.completed -> TaskStatus.DONE
        task.assignees.any { it.uid == currentUser.uid } -> TaskStatus.YOURS
        task.assignees.size >= task.maxAssign -> TaskStatus.TAKEN
        else -> TaskStatus.OPEN
      }
}
