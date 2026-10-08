// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

  /** Whether the "Create task" button can be pressed. */
  val canCreate: Boolean
    get() = title.isNotBlank() && startDateError == null && endDateError == null && !isSaving

  /**
   * Builds the [Task] described by this form.
   *
   * @throws IllegalArgumentException if the form is not valid (see [canCreate]).
   */
  fun toTask(taskId: String, eventId: String): Task =
      Task(
          taskId = taskId,
          eventId = eventId,
          title = title.trim(),
          description = description.trim(),
          location = location.trim(),
          startTime = startDateTime,
          endTime = endDateTime,
      )

  private fun combine(date: LocalDate?, time: LocalTime?): LocalDateTime? =
      date?.atTime(time ?: LocalTime.MIDNIGHT)
}

/**
 * Holds the state of the Create task screen and saves the new task in [tasksRepository].
 *
 * @param eventId The event the created task belongs to.
 * @param tasksRepository Where the task is saved. Only the interface is known here, never Firebase.
 */
class CreateTaskViewModel(
    private val eventId: String,
    private val tasksRepository: TasksRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(CreateTaskUiState())
  val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

  fun updateTitle(title: String) = _uiState.update { it.copy(title = title, titleEdited = true) }

  fun updateDescription(description: String) = _uiState.update {
    it.copy(description = description)
  }

  fun updateLocation(location: String) = _uiState.update { it.copy(location = location) }

  fun updateStartDate(date: LocalDate) = _uiState.update { it.copy(startDate = date) }

  fun updateStartTime(time: LocalTime) = _uiState.update { it.copy(startTime = time) }

  fun updateEndDate(date: LocalDate) = _uiState.update { it.copy(endDate = date) }

  fun updateEndTime(time: LocalTime) = _uiState.update { it.copy(endTime = time) }

  fun clearError() = _uiState.update { it.copy(errorMsg = null) }

  /** Saves the task if the form is valid. Does nothing while a save is running. */
  fun createTask() {
    val form = _uiState.value
    if (!form.canCreate) return
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }
    viewModelScope.launch {
      runCatching { tasksRepository.addTask(form.toTask(tasksRepository.getNewUid(), eventId)) }
          .onSuccess { _uiState.update { it.copy(isSaving = false, isCreated = true) } }
          .onFailure { error ->
            _uiState.update {
              it.copy(isSaving = false, errorMsg = error.message ?: "Unable to create the task.")
            }
          }
    }
  }

  companion object {
    fun factory(eventId: String, tasksRepository: TasksRepository): ViewModelProvider.Factory =
        viewModelFactory {
          initializer { CreateTaskViewModel(eventId, tasksRepository) }
        }
  }
}
