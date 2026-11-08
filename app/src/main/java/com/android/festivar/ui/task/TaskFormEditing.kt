// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import android.util.Log
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.Venue
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructuresRepository
import com.android.festivar.model.task.Priority
import com.android.festivar.model.task.TaskSpot
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.task.create.CreateTaskUiState
import com.android.festivar.ui.task.create.MAX_ESTIMATED_DIGITS
import com.android.festivar.ui.task.create.MIN_MAX_ASSIGN
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException

/** The setters a task form screen needs, shared by the create and edit ViewModels. */
interface TaskFormEditor {
  /** Sets the title and marks it as edited, so that an empty title is reported. */
  fun updateTitle(title: String)

  /** Sets the description. */
  fun updateDescription(description: String)

  /** Sets the location. */
  fun updateLocation(location: String)

  /** Sets the priority. */
  fun updatePriority(priority: Priority)

  /** Sets where the task sits on the venue plan, or clears it when [spot] is `null`. */
  fun updateSpot(spot: TaskSpot?)

  /** Adds one to the number of people the task needs. */
  fun incrementMaxAssign()

  /** Removes one from the number of people the task needs, down to [MIN_MAX_ASSIGN]. */
  fun decrementMaxAssign()

  /**
   * Sets the estimated time, in minutes. Anything but digits is dropped, and at most
   * [MAX_ESTIMATED_DIGITS] digits are kept, so that the number always fits in an `Int`.
   */
  fun updateEstimatedMinutes(text: String)

  /** Sets the date the task starts. */
  fun updateStartDate(date: LocalDate)

  /** Sets the time the task starts. */
  fun updateStartTime(time: LocalTime)

  /** Sets the date the task ends. */
  fun updateEndDate(date: LocalDate)

  /** Sets the time the task ends. */
  fun updateEndTime(time: LocalTime)

  /** Opens the picker dialog of [field], or closes the open one when [field] is `null`. */
  fun showPicker(field: DateTimeField?)

  /** Sets [date] in the field whose picker is open, then closes the picker. */
  fun pickDate(date: LocalDate)

  /** Sets [time] in the field whose picker is open, then closes the picker. */
  fun pickTime(time: LocalTime)
}

/**
 * The one implementation of [TaskFormEditor], over a form it does not own: the owning ViewModel
 * decides how a change reaches its state, and when it is ignored.
 */
internal class TaskFormEditing : TaskFormEditor {
  /**
   * Applies a change of a form field or of the open picker. Set by the owning ViewModel in its
   * init, before any setter runs.
   */
  lateinit var applyEdit: ((CreateTaskUiState) -> CreateTaskUiState) -> Unit

  /**
   * The form as it is now, read by [pickDate] and [pickTime]. Set by the owning ViewModel in its
   * init.
   */
  lateinit var current: () -> CreateTaskUiState

  override fun updateTitle(title: String) = applyEdit { it.copy(title = title, titleEdited = true) }

  override fun updateDescription(description: String) = applyEdit {
    it.copy(description = description)
  }

  override fun updateLocation(location: String) = applyEdit { it.copy(location = location) }

  override fun updatePriority(priority: Priority) = applyEdit { it.copy(priority = priority) }

  override fun updateSpot(spot: TaskSpot?) = applyEdit { it.copy(spot = spot) }

  override fun incrementMaxAssign() = applyEdit { it.copy(maxAssign = it.maxAssign + 1) }

  override fun decrementMaxAssign() = applyEdit {
    it.copy(maxAssign = (it.maxAssign - 1).coerceAtLeast(MIN_MAX_ASSIGN))
  }

  override fun updateEstimatedMinutes(text: String) = applyEdit {
    it.copy(estimatedMinutes = text.filter(Char::isDigit).take(MAX_ESTIMATED_DIGITS))
  }

  override fun updateStartDate(date: LocalDate) = applyEdit { it.copy(startDate = date) }

  override fun updateStartTime(time: LocalTime) = applyEdit { it.copy(startTime = time) }

  override fun updateEndDate(date: LocalDate) = applyEdit { it.copy(endDate = date) }

  override fun updateEndTime(time: LocalTime) = applyEdit { it.copy(endTime = time) }

  override fun showPicker(field: DateTimeField?) = applyEdit { it.copy(openPicker = field) }

  override fun pickDate(date: LocalDate) {
    when (current().openPicker) {
      DateTimeField.START_DATE -> updateStartDate(date)
      DateTimeField.END_DATE -> updateEndDate(date)
      else -> Unit
    }
    showPicker(null)
  }

  override fun pickTime(time: LocalTime) {
    when (current().openPicker) {
      DateTimeField.START_TIME -> updateStartTime(time)
      DateTimeField.END_TIME -> updateEndTime(time)
      else -> Unit
    }
    showPicker(null)
  }
}

/**
 * Reads the venue of the event [eventId] and its structures, for the spot field of the task forms.
 * A failure of either read is not an error of the form: it gives no venue and no structures, so the
 * field offers only "No spot".
 *
 * @return The venue, null when the event has none or a read failed, and the structures.
 */
internal suspend fun loadVenueAndStructures(
    eventId: String,
    eventsRepository: EventsRepository,
    structuresRepository: StructuresRepository,
): Pair<Venue?, List<Structure>> =
    try {
      eventsRepository.getEvent(eventId).venue to structuresRepository.getAllStructures(eventId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w("TaskFormEditing", "The venue of event $eventId could not be loaded", e)
      null to emptyList()
    }
