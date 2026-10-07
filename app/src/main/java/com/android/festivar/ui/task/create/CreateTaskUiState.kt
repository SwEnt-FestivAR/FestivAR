// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import com.android.festivar.model.task.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Why the schedule typed in the Create task form cannot be turned into a valid [Task]. */
enum class ScheduleError {
  /** A time was picked without its date. */
  TIME_WITHOUT_DATE,

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
    val isSaving: Boolean = false,
    val isCreated: Boolean = false,
    val errorMsg: String? = null,
) {
  val startDateTime: LocalDateTime?
    get() = combine(startDate, startTime)

  val endDateTime: LocalDateTime?
    get() = combine(endDate, endTime)

  /** The first reason the schedule is invalid, or `null` if it is valid (or empty). */
  val scheduleError: ScheduleError?
    get() {
      if ((startTime != null && startDate == null) || (endTime != null && endDate == null)) {
        return ScheduleError.TIME_WITHOUT_DATE
      }
      val start = startDateTime
      val end = endDateTime
      return if (start != null && end != null && end.isBefore(start)) {
        ScheduleError.END_BEFORE_START
      } else {
        null
      }
    }

  /** Whether the "Create task" button can be pressed. */
  val canCreate: Boolean
    get() = title.isNotBlank() && scheduleError == null && !isSaving

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
