// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** What is wrong with a field of the Create task form, shown as a message under that field. */
enum class FieldError {
  /** The title was edited and is now blank. */
  EMPTY_TITLE,

  /** A time was picked without its date. */
  MISSING_DATE,

  /** The end is before the start. */
  END_BEFORE_START,
}

/**
 * Everything the user typed in the Create task form, plus the rules that decide whether it can be
 * turned into a [Task]. It is a plain immutable value, so these rules are testable without any UI.
 *
 * A date picked without a time means "from the start of that day".
 */
data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val titleEdited: Boolean = false,
    // True while the task is being saved. It keeps a second tap on "Create task" from saving the
    // task twice (see canCreate).
    val isSaving: Boolean = false,
    val isCreated: Boolean = false,
    val errorMsg: String? = null,
) {
  val startDateTime: LocalDateTime?
    get() = combine(startDate, startTime)

  val endDateTime: LocalDateTime?
    get() = combine(endDate, endTime)

  /**
   * The title error. It only appears once the title has been edited, so that an untouched form does
   * not start with an error.
   */
  val titleError: FieldError?
    get() = if (titleEdited && title.isBlank()) FieldError.EMPTY_TITLE else null

  /** The error shown under the start date, or `null`. */
  val startDateError: FieldError?
    get() = if (startTime != null && startDate == null) FieldError.MISSING_DATE else null

  /** The error shown under the end date, or `null`. */
  val endDateError: FieldError?
    get() {
      if (endTime != null && endDate == null) return FieldError.MISSING_DATE
      val start = startDateTime
      val end = endDateTime
      return if (start != null && end != null && end.isBefore(start)) {
        FieldError.END_BEFORE_START
      } else {
        null
      }
    }

  /**
   * Whether the "Create task" button can be pressed. It is false while a save is running and once
   * the task is created, so that a tap just after the save cannot create a second task.
   */
  val canCreate: Boolean
    get() = isValid && !isSaving && !isCreated

  private val isValid: Boolean
    get() = title.isNotBlank() && startDateError == null && endDateError == null

  /**
   * Builds the [Task] described by this form.
   *
   * @throws IllegalArgumentException if the form is not valid: blank title, or a date error (a time
   *   without its date, or an end before the start).
   */
  fun toTask(taskId: String, eventId: String): Task {
    require(isValid) { "The form is not valid." }
    return Task(
        taskId = taskId,
        eventId = eventId,
        title = title.trim(),
        description = description.trim(),
        location = location.trim(),
        startTime = startDateTime,
        endTime = endDateTime,
    )
  }

  private fun combine(date: LocalDate?, time: LocalTime?): LocalDateTime? =
      date?.atTime(time ?: LocalTime.MIDNIGHT)
}

private const val SAVE_TIMEOUT_MS = 15_000L

/**
 * Holds the state of the Create task screen and saves the new task in [tasksRepository].
 *
 * @param tasksRepository Where the task is saved. It is the app's repository by default; the tests
 *   give an in-memory one.
 * @param saveTimeoutMs How long a save may last before it is reported as failed, so that the form
 *   never stays stuck in the saving state.
 */
class CreateTaskViewModel(
    private val tasksRepository: TasksRepository = TasksRepositoryProvider.repository,
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
) : ViewModel() {
  // The id of the task being created and the event it was created for. They are kept across
  // retries: if a save timed out or failed after reaching the server, the retry targets the same
  // task instead of creating a second one. A different event starts a new task.
  private var pendingTaskId: String? = null
  private var pendingEventId: String? = null

  // Create task UI state
  private val _uiState = MutableStateFlow(CreateTaskUiState())
  val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

  // Functions to update the UI state.

  // Once the task is created the form is final: later edits are ignored.
  private fun edit(change: (CreateTaskUiState) -> CreateTaskUiState) = _uiState.update {
    if (it.isCreated) it else change(it)
  }

  /** Sets the title and marks it as edited, so that an empty title is reported. */
  fun updateTitle(title: String) = edit { it.copy(title = title, titleEdited = true) }

  /** Sets the description. */
  fun updateDescription(description: String) = edit { it.copy(description = description) }

  /** Sets the location. */
  fun updateLocation(location: String) = edit { it.copy(location = location) }

  /** Sets the date the task starts. */
  fun updateStartDate(date: LocalDate) = edit { it.copy(startDate = date) }

  /** Sets the time the task starts. */
  fun updateStartTime(time: LocalTime) = edit { it.copy(startTime = time) }

  /** Sets the date the task ends. */
  fun updateEndDate(date: LocalDate) = edit { it.copy(endDate = date) }

  /** Sets the time the task ends. */
  fun updateEndTime(time: LocalTime) = edit { it.copy(endTime = time) }

  /** Clears the error message in the UI state. */
  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  /**
   * Saves the task in the event [eventId] if the form is valid. Does nothing while a save is
   * running.
   *
   * @param eventId The event the created task belongs to.
   */
  fun createTask(eventId: String) {
    val form = _uiState.value
    if (!form.canCreate) return
    if (eventId.isBlank()) {
      // A Task needs an event: this is a mistake of the caller, reported instead of crashing.
      Log.e("CreateTaskViewModel", "createTask called without an event")
      _uiState.update { it.copy(errorMsg = "Unable to create the task.") }
      return
    }
    // The task is built from the copy of the form taken above, so typing during the save does not
    // change what is saved. It is built outside the try: the form was just checked, so a failure
    // here is a bug and must not be shown to the user as a failed save.
    if (pendingEventId != eventId) pendingTaskId = null
    val taskId = pendingTaskId ?: tasksRepository.getNewUid()
    pendingTaskId = taskId
    pendingEventId = eventId
    val task = form.toTask(taskId, eventId)
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        withTimeout(saveTimeoutMs) { tasksRepository.addTask(task) }
        _uiState.update { it.copy(isSaving = false, isCreated = true) }
      } catch (e: TimeoutCancellationException) {
        onSaveFailed(task, e)
      } catch (e: CancellationException) {
        _uiState.update { it.copy(isSaving = false) }
        throw e
      } catch (e: Exception) {
        onSaveFailed(task, e)
      }
    }
  }

  // A failed save may still have reached the server (for example a timeout). If the task exists,
  // it is created, and updated if the form changed since that save; otherwise the error is shown
  // and the user can retry with the same task id.
  private suspend fun onSaveFailed(task: Task, e: Exception) {
    Log.e("CreateTaskViewModel", "Error adding the task", e)
    val alreadySaved =
        try {
          withTimeout(saveTimeoutMs) {
            val stored = tasksRepository.getTask(task.taskId)
            if (stored != task) tasksRepository.editTask(task.taskId, task)
          }
          true
        } catch (_: TimeoutCancellationException) {
          false
        } catch (e: CancellationException) {
          throw e
        } catch (_: Exception) {
          false
        }
    _uiState.update {
      if (alreadySaved) it.copy(isSaving = false, isCreated = true)
      else it.copy(isSaving = false, errorMsg = "Unable to create the task.")
    }
  }
}
