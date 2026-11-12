// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.task.details

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.user.User
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "TaskDetailsViewModel"

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
 * @property currentUserId the uid of the signed-in user, to tell if [task] is [TaskStatus.YOURS].
 * @property task the task shown, or null until it is loaded (or if it could not be loaded).
 * @property isLoading true while the task is being loaded.
 * @property isUpdating true while an action is being saved, so a second tap does nothing. Actions
 *   are also disabled while [isLoading], so a reload cannot overwrite a saved change.
 * @property errorMsg the string resource of the last failure, or null.
 * @property ownerName the name of the owner of [task], who is credited with its voice note. Blank
 *   when the current user is the owner, when the task has no voice note, or if it is unknown.
 */
data class TaskDetailsUiState(
    val currentUserId: String,
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    @StringRes val errorMsg: Int? = null,
    val ownerName: String = "",
) {
  /** The badge of [task], computed from it so the two cannot differ; null while [task] is null. */
  val status: TaskStatus?
    get() = task?.let {
      when {
        it.completed -> TaskStatus.DONE
        it.assignees.any { user -> user.uid == currentUserId } -> TaskStatus.YOURS
        it.assignees.size >= it.maxAssign -> TaskStatus.TAKEN
        else -> TaskStatus.OPEN
      }
    }

  /** The "Take" button of an [TaskStatus.OPEN] task. */
  val canTake: Boolean
    get() = status == TaskStatus.OPEN && !isUpdating && !isLoading

  /** The "Give back" button of a [TaskStatus.YOURS] task. */
  val canGiveBack: Boolean
    get() = status == TaskStatus.YOURS && !isUpdating && !isLoading

  /** The "Mark done" button of a [TaskStatus.YOURS] task. */
  val canMarkDone: Boolean
    get() = status == TaskStatus.YOURS && !isUpdating && !isLoading

  /** Whether the edit button is shown: only the owner of [task] can edit it. */
  val canEdit: Boolean
    get() = task != null && task.ownerId == currentUserId

  /** Whether the actions bar is shown at all: only open and own tasks have one. */
  val hasActions: Boolean
    get() = status == TaskStatus.OPEN || status == TaskStatus.YOURS

}

/**
 * Holds the state of the Task details screen and applies its three actions: take the task, give it
 * back and mark it done. Each action saves its change through [dataRepository] and shows the task
 * only once saved. Taking and giving back read the task again, since the stored assignee is the
 * user's stored profile; marking done shows the completed task directly.
 *
 * It also gives the voice note of the task ([voiceNote]): everybody can play it, and only the task
 * owner can record, send and delete it.
 *
 * Nothing loads until [load] is called: the screen calls it on every resume, the first one
 * included, so a load at construction would read the task twice on open.
 *
 * @param taskId the task to show.
 * @param currentUser the signed-in user. The caller must pass it: this class does not check that
 *   someone is signed in.
 * @param dataRepository where the task is read and saved. Taking and giving back the task go
 *   through it, so the user's [User.tasksAssigned] and the task in its event change too.
 * @param voiceNoteServices what records, keeps and plays the voice note. Without it the section is
 *   read-only and the note cannot be played.
 */
class TaskDetailsViewModel(
    private val taskId: String,
    private val currentUser: User,
    private val dataRepository: DataRepository,
    voiceNoteServices: VoiceNoteServices? = null,
) : ViewModel() {
  private val repository: TasksRepository = dataRepository.tasksRepository

  private val _uiState = MutableStateFlow(TaskDetailsUiState(currentUserId = currentUser.uid))
  val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

  /** The voice note of the task: the state of its section and the actions behind it. */
  val voiceNote =
      VoiceNoteController(
          scope = viewModelScope,
          host = TaskHost(),
          currentUserId = currentUser.uid,
          services = voiceNoteServices,
      )

  // isLoading starts true, so it cannot tell a running load from the initial state.
  private var loadJob: Job? = null

  /**
   * Loads the task; on failure the error is set and the version already shown, if any, stays shown.
   * A successful load clears a load error but keeps a save error. Does nothing while an action is
   * saving or another load is running.
   */
  fun load() {
    if (_uiState.value.isUpdating || loadJob?.isActive == true) return
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val task = repository.getTask(taskId)
        val ownerName = ownerNameOf(task)
        // Only a load error is cleared: a save error stays until the user dismisses it, even
        // when the task reloads on resume.
        _uiState.update {
          it.copy(
              task = task,
              ownerName = ownerName,
              isLoading = false,
              errorMsg = it.errorMsg.takeUnless { e -> e == R.string.task_details_load_failed },
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error loading task $taskId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = R.string.task_details_load_failed) }
      }
    }
  }

  /**
   * The name of the owner of [task], read only when somebody else has to be credited with its voice
   * note. A name that cannot be read is blank: the task is shown all the same.
   */
  private suspend fun ownerNameOf(task: Task): String {
    if (task.voiceNote == null || task.ownerId == currentUser.uid) return ""
    return try {
      val owner = dataRepository.usersRepository.getUser(task.ownerId)
      "${owner.name} ${owner.surname}".trim()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(LOG_TAG, "Error loading the owner of task $taskId", e)
      ""
    }
  }

  /**
   * Adds the current user to the task's assignees, and the task to the user's [User.tasksAssigned]
   * and to its event. Does nothing unless [TaskDetailsUiState.canTake].
   */
  fun take() =
      saveIf({ it.canTake }) {
        dataRepository.joinTask(taskId, currentUser.uid)
        repository.getTask(taskId)
      }

  /**
   * Removes the current user from the assignees, in the task and in its event, and the task from
   * the user's [User.tasksAssigned]. Does nothing unless the task is yours.
   */
  fun giveBack() =
      saveIf({ it.canGiveBack }) {
        dataRepository.quitTask(taskId, currentUser.uid)
        repository.getTask(taskId)
      }

  /** Completes the task. Does nothing unless the task is yours. */
  fun markDone() =
      saveIf({ it.canMarkDone }) {
        val completedTask = it.complete()
        repository.editTask(taskId, completedTask)
        completedTask
      }

  /** Deletes the task. Does nothing unless the current user owns it. */
  fun deleteTask(onDeleted: () -> Unit = {}) {
    val before = _uiState.getAndUpdate {
      if (it.canEdit && !it.isLoading && !it.isUpdating) {
        it.copy(isUpdating = true, errorMsg = null)
      } else {
        it
      }
    }
    if (!before.canEdit || before.isLoading || before.isUpdating) return

    viewModelScope.launch {
      try {
        dataRepository.deleteTask(taskId)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isUpdating = false) }
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error deleting task $taskId", e)
        _uiState.update {
          it.copy(isUpdating = false, errorMsg = R.string.task_details_save_failed)
        }
        return@launch
      }
      _uiState.update { it.copy(isUpdating = false) }
      onDeleted()
    }
  }

  /** Clears the error message. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  // editTask overwrites the whole task, so two users acting at once can lose a change:
  // https://github.com/SwEnt-FestivAR/FestivAR/issues/186
  // save stores the change and returns the task to show.
  private fun saveIf(allowed: (TaskDetailsUiState) -> Boolean, save: suspend (Task) -> Task) {
    val task = startSaving(allowed) ?: return

    viewModelScope.launch {
      try {
        finishSaving(save(task))
      } catch (e: CancellationException) {
        finishSaving(null)
        throw e
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Error saving task $taskId", e)
        finishSaving(null, failed = true)
      }
    }
  }

  /**
   * Marks the state as updating if [allowed] and a task is shown, and returns that task; null if
   * the action must not run. Checking and setting `isUpdating` is one step, so two taps cannot both
   * start a save.
   */
  private fun startSaving(allowed: (TaskDetailsUiState) -> Boolean): Task? {
    val canRun = { state: TaskDetailsUiState -> allowed(state) && state.task != null }
    val before = _uiState.getAndUpdate {
      if (canRun(it)) it.copy(isUpdating = true, errorMsg = null) else it
    }
    return before.task?.takeIf { canRun(before) }
  }

  private fun finishSaving(updated: Task?, failed: Boolean = false) = _uiState.update {
    it.copy(
        task = updated ?: it.task,
        isUpdating = false,
        errorMsg = if (failed) R.string.task_details_save_failed else it.errorMsg,
    )
  }

  override fun onCleared() = voiceNote.release()

  /** Lets the voice note save the task through the guard of the other actions. */
  private inner class TaskHost : VoiceNoteHost {
    override val state: StateFlow<TaskDetailsUiState>
      get() = uiState

    override fun startSaving(allowed: (TaskDetailsUiState) -> Boolean) =
        this@TaskDetailsViewModel.startSaving(allowed)

    override suspend fun saveTask(updated: Task) = repository.editTask(taskId, updated)

    override fun finishSaving(updated: Task?, failed: Boolean) =
        this@TaskDetailsViewModel.finishSaving(updated, failed)
  }
}

