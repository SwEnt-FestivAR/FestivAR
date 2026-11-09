// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.components.DateTimeRange
import com.android.festivar.ui.components.END_OF_DAY
import com.android.festivar.ui.components.FieldError
import com.android.festivar.ui.components.START_OF_DAY
import com.android.festivar.ui.components.endDateError
import com.android.festivar.ui.components.startDateError
import com.android.festivar.ui.task.TaskFormEditing
import com.android.festivar.ui.task.TaskFormEditor
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
    // How many people the task needs. A stepper changes it, so it is never below MIN_MAX_ASSIGN.
    val maxAssign: Int = MIN_MAX_ASSIGN,
    // The estimated time is kept as typed (digits only) so that an empty field stays empty.
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

  /** The error shown under the start date, or `null`. See [startDateError]. */
  val startDateError: FieldError?
    get() = startDateError(startDate, startTime)

  /** The error shown under the end date, or `null`. See [endDateError]. */
  val endDateError: FieldError?
    get() = endDateError(DateTimeRange(startDate, startTime, endDate, endTime), zone)

  /**
   * The error shown under the estimated time field, or `null`. It is optional, so empty is fine,
   * but a duration of zero minutes is not.
   */
  val estimatedTimeError: FieldError?
    get() =
        if (estimatedMinutes.isEmpty() || (estimatedMinutes.toIntOrNull() ?: 0) >= 1) null
        else FieldError.INVALID_ESTIMATED_TIME

  /**
   * Whether the user changed anything in the form, so that leaving it would lose something. Keep it
   * in sync with the form fields: a field missing here is lost without asking.
   */
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
            maxAssign != MIN_MAX_ASSIGN ||
            estimatedMinutes.isNotEmpty()

  /** Whether going back must ask first: a created task has nothing left to lose. */
  val needsDiscardConfirmation: Boolean
    get() = hasChanges && !isCreated

  /**
   * Whether the "Create task" button can be pressed. It is false while a save is running and once
   * the task is created, so that a tap just after the save cannot create a second task.
   */
  val canCreate: Boolean
    get() = isValid && !isSaving && !isCreated

  /** Whether it has a title and no field error; [canCreate] adds the saving and created flags. */
  internal val isValid: Boolean
    get() =
        title.isNotBlank() &&
            startDateError == null &&
            endDateError == null &&
            estimatedTimeError == null

  /**
   * Builds the [Task] described by this form.
   *
   * @param ownerId The uid of the user creating the task, who becomes its owner.
   * @throws IllegalArgumentException if the form is not valid: blank title, a date error (a time
   *   without its date, or an end before the start) or a wrong estimated time, or if [ownerId] is
   *   blank.
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
        maxAssign = maxAssign,
        ownerId = ownerId,
    )
  }
}

/** The most digits of the estimated time: 9 digits is always below `Int.MAX_VALUE`. */
internal const val MAX_ESTIMATED_DIGITS = 9

/** The fewest people a task can need, and the number of people a new task starts with. */
const val MIN_MAX_ASSIGN = 1

private const val TAG = "CreateTaskViewModel"

private const val SAVE_TIMEOUT_MS = 15_000L

/** The message shown when the task could not be saved, whatever the cause (a string resource). */
@StringRes private val SAVE_FAILED_MESSAGE = R.string.create_task_error_save_failed

/**
 * Holds the state of the Create task screen and saves the new task through [dataRepository], which
 * also adds it to its event and to the tasks assigned to its owner.
 *
 * [DataRepository.addTask] does not check that the owner is a member of the event: it stays the job
 * of this view model and of the screens that open it.
 *
 * @param eventId The event the created task belongs to. It cannot be blank.
 * @param currentUserId Gives the uid of the signed-in user, who becomes the owner of the task. It
 *   is asked when the user taps "Create task", not when the screen opens, so a sign-out or an
 *   account change in between is taken into account. A task always has an owner when it is created:
 *   a blank uid saves nothing and shows the save error.
 * @param dataRepository Saves the task and keeps its event and its owner in sync with it. The tests
 *   give an in-memory one.
 * @param saveTimeoutMs How long a save may last before it is reported as failed, so that the form
 *   never stays stuck in the saving state. A failed save is followed by a read of the task under
 *   the same timeout, so the saving state lasts at most twice this long.
 * @param zone The time zone of the created task's start and end. The device's one by default.
 */
class CreateTaskViewModel
private constructor(
    private val eventId: String,
    private val currentUserId: () -> String,
    private val dataRepository: DataRepository,
    private val saveTimeoutMs: Long,
    zone: ZoneId,
    private val formEditing: TaskFormEditing,
) : ViewModel(), TaskFormEditor by formEditing {

  constructor(
      eventId: String,
      currentUserId: () -> String,
      dataRepository: DataRepository,
      saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
      zone: ZoneId = ZoneId.systemDefault(),
  ) : this(eventId, currentUserId, dataRepository, saveTimeoutMs, zone, TaskFormEditing())

  // Gives the new task id, and reads or edits the task after a failed save (see onSaveFailed).
  private val tasksRepository: TasksRepository = dataRepository.tasksRepository

  // The id of the task being created. It is kept across retries: if a save timed out or failed
  // after reaching the server, the retry targets the same task instead of creating a second one.
  private var pendingTaskId: String? = null

  // Create task UI state
  private val _uiState = MutableStateFlow(CreateTaskUiState(zone = zone))
  val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

  // A finished form ignores every change, pickers included, since the screen leaves on creation.
  private fun edit(change: (CreateTaskUiState) -> CreateTaskUiState) = _uiState.update {
    if (it.isCreated) it else change(it)
  }

  init {
    // A Task needs an event: a blank one is a mistake of the caller.
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    // The form setters come from TaskFormEditing. Pickers follow the same guard as the fields.
    formEditing.applyEdit = ::edit
    formEditing.current = { _uiState.value }
  }

  /** Saves the task if the form is valid. Does nothing while a save is running. */
  fun createTask() {
    val form = _uiState.value
    if (!form.canCreate) return
    val ownerId = currentUserId()
    if (ownerId.isBlank()) {
      // Nobody is signed in: a task without an owner could not be managed, so save nothing and
      // show the save error.
      Log.e(TAG, "createTask called without a signed-in user")
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
        withTimeout(saveTimeoutMs) { dataRepository.addTask(task) }
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

  /**
   * Decides what a failed save means, since it may still have reached the server (for example
   * after a timeout).
   *
   * If [task] is stored, it is created: it is updated if the form changed since that save, and
   * added to its event and to its owner if that save stopped before doing it (see [repairTaskSync]).
   * Otherwise, or if this check fails or times out too, the save error is shown and the user can
   * retry with the same task id.
   *
   * @param task The task that could not be saved, as built from the form at the last tap.
   * @param e The failure of the save, logged as a warning.
   */
  private suspend fun onSaveFailed(task: Task, e: Exception) {
    // Not an error yet: the task may have been stored anyway, which is checked below.
    Log.w(TAG, "Adding the task failed, checking whether it was stored", e)
    val alreadySaved =
        try {
          withTimeout(saveTimeoutMs) {
            // Throws if the task is not stored, so nothing below runs for a task that does not
            // exist: linking it would leave its event pointing to a missing task.
            val stored = tasksRepository.getTask(task.taskId)
            if (stored != task) tasksRepository.editTask(task.taskId, task)
              repairTaskSync(task)
          }
          Log.i(TAG, "The task was stored despite the failure")
          true
        } catch (_: TimeoutCancellationException) {
          Log.e(TAG, "The task could not be checked in time, so it may not be stored")
          false
        } catch (e: CancellationException) {
          throw e
        } catch (checkError: Exception) {
          Log.e(TAG, "The task could not be checked, so it may not be stored", checkError)
          false
        }
    _uiState.update {
      if (alreadySaved) it.copy(isSaving = false, isCreated = true)
      else it.copy(isSaving = false, errorMsg = SAVE_FAILED_MESSAGE)
    }
  }

  /**
   * Adds a stored [task] to its event and to the tasks assigned to its owner, wherever it is
   * missing. A failed save may have stored the task but stopped before these writes.
   *
   * Only what is missing is written, so a fully linked task writes nothing, and a failure halfway
   * is completed by the next retry. Trying to repair the [task] itself is not done as the
   * function is called only if succeeds:
   * ```
   * val stored = tasksRepository.getTask(task.taskId)
   * if (stored != task) tasksRepository.editTask(task.taskId, task)
   * ```
   *
   * Call it only once the task is known to be stored: an event listing a task that does not exist
   * can no longer be read.
   *
   * @param task The stored task. Its event and its owner must exist.
   * @throws NoSuchElementException If the event or the owner does not exist.
   */
  private suspend fun repairTaskSync(task: Task) {
    val eventsRepository = dataRepository.eventsRepository
    val usersRepository = dataRepository.usersRepository

      // Checks the synchronization with the event the task is part of and repairs it if needed.
    val event = eventsRepository.getEvent(task.eventId)
    if (event.tasks.none { it.taskId == task.taskId }) {
      eventsRepository.editEvent(event.eventId, event.addTask(task))
    }

      // Checks the synchronization with the user the task is created by and repairs it if needed.
    val owner = usersRepository.getUser(task.ownerId)
    if (task.taskId !in owner.tasksAssigned) {
      usersRepository.editUser(owner.uid, owner.addAssignedTask(task.taskId))
    }
  }
}
