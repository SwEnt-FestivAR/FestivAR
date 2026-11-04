// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.edit

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.task.TaskFormEditing
import com.android.festivar.ui.task.TaskFormEditor
import com.android.festivar.ui.task.create.CreateTaskUiState
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * UI state of the Edit task screen. The form is the Create task one, filled from [task].
 *
 * @property task The task as stored, or null until it is loaded (or if it could not be loaded).
 * @property form What the user edits, with the field rules of the Create task form.
 * @property isOwner Whether the signed-in user owns [task]: only the owner can save.
 * @property isLoading True until the first load ends.
 * @property isSaving True while a save is running, so a second tap does nothing.
 * @property isSaved True once the edit is stored; the form is then final.
 * @property errorMsg The string resource of the last failure, or null.
 */
data class EditTaskUiState(
    val task: Task? = null,
    val form: CreateTaskUiState = CreateTaskUiState(),
    val isOwner: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @StringRes val errorMsg: Int? = null,
) {
  /**
   * Whether the form differs from [task] on a field the user can edit. Opening a picker, touching
   * the title without changing it, or adding spaces around a text is not a change.
   */
  val hasChanges: Boolean
    get() = task != null && form.normalised() != task.toForm(form.zone).normalised()

  /** Whether going back must ask first: a saved edit has nothing left to lose. */
  val needsDiscardConfirmation: Boolean
    get() = hasChanges && !isSaved

  /** [FieldError.TOO_FEW_SLOTS] when fewer people are needed than are already assigned. */
  val maxAssignError: FieldError?
    get() =
        if (task != null && form.maxAssign < task.assignees.size) FieldError.TOO_FEW_SLOTS else null

  /** Whether the "Save" button can be pressed. [hasChanges] implies that a task is loaded. */
  val canSave: Boolean
    get() = isOwner && form.isValid && maxAssignError == null && hasChanges && !isSaving && !isSaved

  /**
   * Builds the edited [Task]. What the form does not own (assignees, completion, voice note, owner,
   * event) is kept from [task].
   *
   * @throws IllegalArgumentException if no task is loaded, the form is not valid, or fewer people
   *   are needed than are assigned (the rules of [CreateTaskUiState.toTask] and [Task]).
   */
  fun toTask(): Task {
    val original = requireNotNull(task) { "No task is loaded." }
    return form
        .toTask(original.taskId, original.eventId, original.ownerId)
        .copy(
            // editTask overwrites the whole task, so a take or a completion saved by someone else
            // since the load can be lost: https://github.com/SwEnt-FestivAR/FestivAR/issues/186
            assignees = original.assignees,
            completed = original.completed,
            voiceNote = original.voiceNote,
        )
  }
}

// Every stored time is kept explicit, even one on the START_OF_DAY or END_OF_DAY boundary: the
// form maps it back to the same instant either way, and an explicit time loses nothing.
// A time in the repeated hour of a daylight saving change maps back to the earlier offset.
private fun Task.toForm(zone: ZoneId): CreateTaskUiState {
  val start = startTime?.withZoneSameInstant(zone)
  val end = endTime?.withZoneSameInstant(zone)
  return CreateTaskUiState(
      title = title,
      description = description,
      location = location,
      startDate = start?.toLocalDate(),
      startTime = start?.toLocalTime(),
      endDate = end?.toLocalDate(),
      endTime = end?.toLocalTime(),
      priority = priority,
      maxAssign = maxAssign,
      estimatedMinutes = estimatedTime?.toMinutes()?.toString() ?: "",
      zone = zone,
  )
}

// What the user cannot see in the form: the picker, the edited flag, flags of the create screen,
// and spaces around the texts, which the save trims anyway.
private fun CreateTaskUiState.normalised(): CreateTaskUiState =
    copy(
        title = title.trim(),
        description = description.trim(),
        location = location.trim(),
        openPicker = null,
        titleEdited = false,
        isSaving = false,
        isCreated = false,
        errorMsg = null,
    )

private const val TAG = "EditTaskViewModel"

private const val SAVE_TIMEOUT_MS = 15_000L

/**
 * Holds the state of the Edit task screen: loads the task, edits it in the Create task form and
 * saves it through [repository].
 *
 * @param taskId The task to edit. It cannot be blank.
 * @param currentUserId Gives the uid of the signed-in user; only the owner of the task can save.
 * @param repository Where the task is read and saved. The app's one by default.
 * @param saveTimeoutMs How long a save may last before it is reported as failed. A failed save is
 *   followed by a read under the same timeout.
 * @param zone The time zone the form shows the times in. The device's one by default.
 */
class EditTaskViewModel
private constructor(
    private val taskId: String,
    private val currentUserId: () -> String,
    private val repository: TasksRepository,
    private val saveTimeoutMs: Long,
    private val zone: ZoneId,
    private val formEditing: TaskFormEditing,
) : ViewModel(), TaskFormEditor by formEditing {

  constructor(
      taskId: String,
      currentUserId: () -> String,
      repository: TasksRepository = TasksRepositoryProvider.repository,
      saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
      zone: ZoneId = ZoneId.systemDefault(),
  ) : this(taskId, currentUserId, repository, saveTimeoutMs, zone, TaskFormEditing())

  init {
    require(taskId.isNotBlank()) { "taskId must not be blank" }
  }

  private val _uiState = MutableStateFlow(EditTaskUiState(form = CreateTaskUiState(zone = zone)))

  /** The state of the form, read by the screen. */
  val uiState: StateFlow<EditTaskUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  /**
   * Loads the task once; a running load is not started twice, and a resume never wipes unsaved
   * edits. After a failed load it can be called again.
   */
  fun load() {
    if (loadJob?.isActive == true || _uiState.value.task != null) return
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }
    loadJob = viewModelScope.launch {
      try {
        val task = repository.getTask(taskId)
        _uiState.update {
          it.copy(
              task = task,
              form = task.toForm(zone),
              isOwner = task.ownerId == currentUserId(),
              isLoading = false,
              errorMsg = null,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error loading task $taskId", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = R.string.edit_task_error_load_failed)
        }
      }
    }
  }

  // Changes are ignored while no task is loaded and once the edit is saved, except closing the
  // picker, so that one opened during a slow save does not stay open.
  private fun edit(change: (CreateTaskUiState) -> CreateTaskUiState) = _uiState.update {
    if (it.task != null && !it.isSaved) return@update it.copy(form = change(it.form))
    val closed = it.form.copy(openPicker = null)
    if (change(it.form) == closed) it.copy(form = closed) else it
  }

  init {
    // The form setters come from TaskFormEditing. Pickers follow the same guard as the fields.
    formEditing.applyEdit = ::edit
    formEditing.current = { _uiState.value.form }
  }

  /** Clears the error message. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  /** Saves the edited task. Does nothing unless [EditTaskUiState.canSave]. */
  fun save() {
    val state = _uiState.value
    if (!state.canSave) return
    // Built outside the try: the state was just checked, so a failure here is a bug.
    val newTask = state.toTask()
    if (currentUserId() != newTask.ownerId) {
      // The signed-in user changed since the load: save nothing and show the save error.
      Log.e(TAG, "save called by a user who does not own task $taskId")
      _uiState.update { it.copy(errorMsg = R.string.edit_task_error_save_failed) }
      return
    }
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      val saved =
          try {
            withTimeout(saveTimeoutMs) { repository.editTask(newTask.taskId, newTask) }
            true
          } catch (e: TimeoutCancellationException) {
            isStored(newTask, e)
          } catch (e: CancellationException) {
            _uiState.update { it.copy(isSaving = false) }
            throw e
          } catch (e: Exception) {
            isStored(newTask, e)
          }
      _uiState.update {
        if (saved) it.copy(task = newTask, isSaving = false, isSaved = true)
        else it.copy(isSaving = false, errorMsg = R.string.edit_task_error_save_failed)
      }
    }
  }

  // A failed save may still have reached the server (for example a timeout): read the task back.
  private suspend fun isStored(newTask: Task, e: Exception): Boolean {
    Log.w(TAG, "Editing the task failed, checking whether it was stored", e)
    return try {
      withTimeout(saveTimeoutMs) { repository.getTask(taskId) } == newTask
    } catch (_: TimeoutCancellationException) {
      Log.e(TAG, "The task could not be checked in time")
      false
    } catch (e: CancellationException) {
      throw e
    } catch (checkError: Exception) {
      Log.e(TAG, "The task could not be checked", checkError)
      false
    }
  }
}
