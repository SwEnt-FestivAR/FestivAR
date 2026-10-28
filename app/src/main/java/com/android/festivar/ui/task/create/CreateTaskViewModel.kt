// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.FieldError
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Everything the user typed in the Create task form, plus the rules that decide whether it can be
 * turned into a [Task]. It is a plain immutable value, so these rules are testable without any UI.
 *
 * A date picked without a time covers the whole day: the start is at [START_OF_DAY] and the end at
 * [END_OF_DAY]. So a start on Saturday at 09:30 and an end date of Saturday is a valid task.
 */
data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val priority: Priority = Priority.NONE,
    // The numbers are kept as typed (digits only) so that an empty field stays empty.
    val maxAssign: String = DEFAULT_MAX_ASSIGN,
    val estimatedMinutes: String = "",
    // The field whose picker dialog is open, or null when none is.
    val openPicker: DateTimeField? = null,
    val titleEdited: Boolean = false,
    // True while the task is being saved. It keeps a second tap on "Create task" from saving the
    // task twice (see canCreate).
    val isSaving: Boolean = false,
    val isCreated: Boolean = false,
    // The time zone of the picked dates and times; the device's one by default.
    val zone: ZoneId = ZoneId.systemDefault(),
    // The dates of the event the task belongs to, once loaded. A task must fit between them; no
    // check is made while they are unknown (for example if the event could not be read).
    val eventStart: ZonedDateTime? = null,
    val eventEnd: ZonedDateTime? = null,
    // String resource of the error to show (see R.string.create_task_error_save_failed), or null.
    @StringRes val errorMsg: Int? = null,
) {
  val startDateTime: ZonedDateTime?
    get() = startDate?.atTime(startTime ?: START_OF_DAY)?.atZone(zone)

  val endDateTime: ZonedDateTime?
    get() = endDate?.atTime(endTime ?: END_OF_DAY)?.atZone(zone)

  /**
   * The title error. It only appears once the title has been edited, so that an untouched form does
   * not start with an error.
   */
  val titleError: FieldError?
    get() = if (titleEdited && title.isBlank()) FieldError.EMPTY_TITLE else null

  /** The error shown under the start date, or `null`. */
  val startDateError: FieldError?
    get() =
        when {
          startTime != null && startDate == null -> FieldError.MISSING_DATE
          isOutsideEvent(startDateTime) -> FieldError.OUTSIDE_EVENT
          else -> null
        }

  /** The error shown under the end date, or `null`. */
  val endDateError: FieldError?
    get() {
      if (endTime != null && endDate == null) return FieldError.MISSING_DATE
      val start = startDateTime
      val end = endDateTime
      return when {
        start != null && end != null && end.isBefore(start) -> FieldError.END_BEFORE_START
        isOutsideEvent(end) -> FieldError.OUTSIDE_EVENT
        else -> null
      }
    }

  private fun isOutsideEvent(time: ZonedDateTime?): Boolean =
      time != null &&
          eventStart != null &&
          eventEnd != null &&
          (time.isBefore(eventStart) || time.isAfter(eventEnd))

  /** Whether the user changed anything in the form, so that leaving it would lose something. */
  val hasChanges: Boolean
    get() =
        title.isNotEmpty() ||
            description.isNotEmpty() ||
            location.isNotEmpty() ||
            startDate != null ||
            startTime != null ||
            endDate != null ||
            endTime != null ||
            priority != Priority.NONE ||
            maxAssign != DEFAULT_MAX_ASSIGN ||
            estimatedMinutes.isNotEmpty()

  /** The error shown under the max assign field, or `null`: it must be a number, at least 1. */
  val maxAssignError: FieldError?
    get() = if ((maxAssign.toIntOrNull() ?: 0) >= 1) null else FieldError.INVALID_MAX_ASSIGN

  /**
   * The error shown under the estimated time field, or `null`. It is optional, so empty is fine.
   */
  val estimatedTimeError: FieldError?
    get() =
        if (estimatedMinutes.isEmpty() || estimatedMinutes.toIntOrNull() != null) null
        else FieldError.INVALID_ESTIMATED_TIME

  /**
   * Whether the "Create task" button can be pressed. It is false while a save is running and once
   * the task is created, so that a tap just after the save cannot create a second task.
   */
  val canCreate: Boolean
    get() = isValid && !isSaving && !isCreated

  private val isValid: Boolean
    get() =
        title.isNotBlank() &&
            startDateError == null &&
            endDateError == null &&
            maxAssignError == null &&
            estimatedTimeError == null

  /**
   * Builds the [Task] described by this form.
   *
   * @param ownerId The uid of the user creating the task, who becomes its owner.
   * @throws IllegalArgumentException if the form is not valid: blank title, a date error (a time
   *   without its date, or an end before the start) or a wrong number, or if [ownerId] is blank.
   */
  fun toTask(taskId: String, eventId: String, ownerId: String): Task {
    require(isValid) { "The form is not valid." }
    return Task(
        taskId = taskId,
        eventId = eventId,
        title = title.trim(),
        description = description.trim(),
        location = location.trim(),
        startTime = startDateTime,
        endTime = endDateTime,
        estimatedTime = estimatedMinutes.toIntOrNull()?.let { Duration.ofMinutes(it.toLong()) },
        priority = priority,
        maxAssign = maxAssign.toInt(),
        ownerId = ownerId,
    )
  }
}

private const val DEFAULT_MAX_ASSIGN = "1"

/** The time of a start date picked without a time. */
val START_OF_DAY: LocalTime = LocalTime.MIDNIGHT

/** The time of an end date picked without a time: the last minute the forms can show. */
val END_OF_DAY: LocalTime = LocalTime.of(23, 59)

private const val SAVE_TIMEOUT_MS = 15_000L

/** The message shown when the task could not be saved, whatever the cause (a string resource). */
@StringRes private val SAVE_FAILED_MESSAGE = R.string.create_task_error_save_failed

/**
 * Holds the state of the Create task screen and saves the new task in [tasksRepository].
 *
 * @param eventId The event the created task belongs to. It cannot be blank.
 * @param ownerId The uid of the signed-in user, who becomes the owner of the task. A task always
 *   has an owner when it is created: a blank uid saves nothing and shows the save error.
 * @param eventsRepository Where the event is read, to check the task's dates against it, and
 *   updated so that the new task is part of its tasks.
 * @param tasksRepository Where the task is saved. It is the app's repository by default; the tests
 *   give an in-memory one.
 * @param saveTimeoutMs How long a save may last before it is reported as failed, so that the form
 *   never stays stuck in the saving state. A failed save is followed by a read of the task under
 *   the same timeout, so the saving state lasts at most twice this long.
 * @param zone The time zone of the created task's start and end. The device's one by default.
 */
class CreateTaskViewModel(
    private val eventId: String,
    private val ownerId: String,
    private val eventsRepository: EventsRepository,
    private val tasksRepository: TasksRepository = TasksRepositoryProvider.repository,
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
  // The id of the task being created. It is kept across retries: if a save timed out or failed
  // after reaching the server, the retry targets the same task instead of creating a second one.
  private var pendingTaskId: String? = null

  // Create task UI state
  private val _uiState = MutableStateFlow(CreateTaskUiState(zone = zone))
  val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

  init {
    // A Task needs an event: a blank one is a mistake of the caller.
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    loadEventDates()
  }

  // The task must fit in its event. If the event cannot be read, the form is not checked against
  // it; saving then fails anyway if the event is really unreachable (see linkToEvent).
  private fun loadEventDates() {
    viewModelScope.launch {
      try {
        val event = withTimeout(saveTimeoutMs) { eventsRepository.getEvent(eventId) }
        _uiState.update { it.copy(eventStart = event.startDate, eventEnd = event.endDate) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e("CreateTaskViewModel", "Error reading the event", e)
      }
    }
  }

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

  /** Sets the priority. */
  fun updatePriority(priority: Priority) = edit { it.copy(priority = priority) }

  /** Sets how many people can take the task. Anything but digits is dropped. */
  fun updateMaxAssign(text: String) = edit { it.copy(maxAssign = text.filter(Char::isDigit)) }

  /** Sets the estimated time, in minutes. Anything but digits is dropped. */
  fun updateEstimatedMinutes(text: String) = edit {
    it.copy(estimatedMinutes = text.filter(Char::isDigit))
  }

  /** Sets the date the task starts. */
  fun updateStartDate(date: LocalDate) = edit { it.copy(startDate = date) }

  /** Sets the time the task starts. */
  fun updateStartTime(time: LocalTime) = edit { it.copy(startTime = time) }

  /** Sets the date the task ends. */
  fun updateEndDate(date: LocalDate) = edit { it.copy(endDate = date) }

  /** Sets the time the task ends. */
  fun updateEndTime(time: LocalTime) = edit { it.copy(endTime = time) }

  /** Opens the picker dialog of [field], or closes the open one when [field] is `null`. */
  fun showPicker(field: DateTimeField?) = _uiState.update { it.copy(openPicker = field) }

  /** Sets [date] in the field whose picker is open, then closes the picker. */
  fun pickDate(date: LocalDate) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_DATE -> updateStartDate(date)
      DateTimeField.END_DATE -> updateEndDate(date)
      else -> Unit
    }
    showPicker(null)
  }

  /** Sets [time] in the field whose picker is open, then closes the picker. */
  fun pickTime(time: LocalTime) {
    when (_uiState.value.openPicker) {
      DateTimeField.START_TIME -> updateStartTime(time)
      DateTimeField.END_TIME -> updateEndTime(time)
      else -> Unit
    }
    showPicker(null)
  }

  /** Saves the task if the form is valid. Does nothing while a save is running. */
  fun createTask() {
    val form = _uiState.value
    if (!form.canCreate) return
    if (ownerId.isBlank()) {
      // Nobody could manage the task if it was saved without an owner: this is a mistake of the
      // caller, reported instead of crashing.
      Log.e("CreateTaskViewModel", "createTask called without an owner")
      _uiState.update { it.copy(errorMsg = SAVE_FAILED_MESSAGE) }
      return
    }
    // The task is built from the copy of the form taken above, so typing during the save does not
    // change what is saved. It is built outside the try: the form was just checked, so a failure
    // here is a bug and must not be shown to the user as a failed save.
    val taskId = pendingTaskId ?: tasksRepository.getNewUid()
    pendingTaskId = taskId
    val task = form.toTask(taskId, eventId, ownerId)
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      try {
        withTimeout(saveTimeoutMs) {
          tasksRepository.addTask(task)
          linkToEvent(task)
        }
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

  // Adds the task to the tasks of its event, unless a previous attempt already did.
  private suspend fun linkToEvent(task: Task) {
    val event = eventsRepository.getEvent(eventId)
    if (event.tasks.none { it.taskId == task.taskId }) {
      eventsRepository.editEvent(eventId, event.addTask(task))
    }
  }

  // A failed save may still have reached the server (for example a timeout). If the task exists,
  // it is created (updated if the form changed since that save, and added to its event); otherwise
  // the error is shown
  // and the user can retry with the same task id.
  private suspend fun onSaveFailed(task: Task, e: Exception) {
    Log.e("CreateTaskViewModel", "Error adding the task", e)
    val alreadySaved =
        try {
          withTimeout(saveTimeoutMs) {
            val stored = tasksRepository.getTask(task.taskId)
            if (stored != task) tasksRepository.editTask(task.taskId, task)
            linkToEvent(task)
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
      else it.copy(isSaving = false, errorMsg = SAVE_FAILED_MESSAGE)
    }
  }
}
