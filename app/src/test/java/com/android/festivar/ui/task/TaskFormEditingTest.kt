// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import com.android.festivar.model.task.Priority
import com.android.festivar.ui.components.DateTimeField
import com.android.festivar.ui.task.create.CreateTaskUiState
import com.android.festivar.ui.task.create.MAX_ESTIMATED_DIGITS
import com.android.festivar.ui.task.create.MIN_MAX_ASSIGN
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests of [TaskFormEditing] over a plain state, without a ViewModel. */
class TaskFormEditingTest {
  private val state = MutableStateFlow(CreateTaskUiState())
  private val editing =
      TaskFormEditing().also {
        it.applyEdit = { change -> state.update(change) }
        it.current = { state.value }
      }

  // Breaks if the title setter does not mark the title as edited, so a blank title shows no error.
  @Test
  fun theTitleSetterMarksTheTitleAsEdited() {
    editing.updateTitle("Run power to stage")

    assertEquals("Run power to stage", state.value.title)
    assertTrue(state.value.titleEdited)
  }

  // Breaks if decrementing goes below MIN_MAX_ASSIGN.
  @Test
  fun decrementStopsAtTheMinimum() {
    editing.incrementMaxAssign()
    repeat(3) { editing.decrementMaxAssign() }

    assertEquals(MIN_MAX_ASSIGN, state.value.maxAssign)
  }

  // Breaks if pickDate writes another field than the open picker's or leaves the picker open.
  @Test
  fun pickDateWritesTheFieldWhosePickerIsOpenAndClosesIt() {
    val day = LocalDate.of(2026, 10, 17)
    editing.showPicker(DateTimeField.END_DATE)

    editing.pickDate(day)

    assertEquals(day, state.value.endDate)
    assertNull(state.value.startDate)
    assertNull(state.value.openPicker)
  }

  private val day = LocalDate.of(2026, 10, 17)
  private val nine = LocalTime.of(9, 0)

  // Applies one setter and checks that only the expected fields changed.
  private fun assertOnly(
      expected: (CreateTaskUiState) -> CreateTaskUiState,
      setter: () -> Unit,
  ) {
    val before = state.value
    setter()
    assertEquals(expected(before), state.value)
  }

  // Breaks if updateDescription writes another field or drops the text.
  @Test
  fun updateDescriptionChangesOnlyTheDescription() =
      assertOnly({ it.copy(description = "Blue crate") }) {
        editing.updateDescription("Blue crate")
      }

  // Breaks if updateLocation writes another field or drops the text.
  @Test
  fun updateLocationChangesOnlyTheLocation() =
      assertOnly({ it.copy(location = "Stage") }) { editing.updateLocation("Stage") }

  // Breaks if updatePriority writes another field or ignores the priority.
  @Test
  fun updatePriorityChangesOnlyThePriority() =
      assertOnly({ it.copy(priority = Priority.HIGH) }) { editing.updatePriority(Priority.HIGH) }

  // Breaks if incrementMaxAssign adds more than one or touches another field.
  @Test
  fun incrementMaxAssignAddsOneAndChangesNothingElse() =
      assertOnly({ it.copy(maxAssign = MIN_MAX_ASSIGN + 1) }) { editing.incrementMaxAssign() }

  // Breaks if typed digits are not parsed as the new number, or another field changes.
  @Test
  fun updateMaxAssignChangesOnlyTheMaximum() =
      assertOnly({ it.copy(maxAssign = 12) }) { editing.updateMaxAssign("12") }

  // Breaks if updateEstimatedMinutes keeps a non digit or more than MAX_ESTIMATED_DIGITS digits,
  // so the number no longer fits in an Int.
  @Test
  fun updateEstimatedMinutesKeepsOnlyDigitsUpToTheLimit() {
    assertOnly({ it.copy(estimatedMinutes = "60") }) { editing.updateEstimatedMinutes("6x0") }

    val tooLong = "1".repeat(MAX_ESTIMATED_DIGITS + 3)
    assertOnly({ it.copy(estimatedMinutes = "1".repeat(MAX_ESTIMATED_DIGITS)) }) {
      editing.updateEstimatedMinutes(tooLong)
    }
  }

  // Breaks if updateStartDate writes another field, such as the end date.
  @Test
  fun updateStartDateChangesOnlyTheStartDate() =
      assertOnly({ it.copy(startDate = day) }) { editing.updateStartDate(day) }

  // Breaks if updateStartTime writes another field, such as the end time.
  @Test
  fun updateStartTimeChangesOnlyTheStartTime() =
      assertOnly({ it.copy(startTime = nine) }) { editing.updateStartTime(nine) }

  // Breaks if updateEndDate writes another field, such as the start date.
  @Test
  fun updateEndDateChangesOnlyTheEndDate() =
      assertOnly({ it.copy(endDate = day) }) { editing.updateEndDate(day) }

  // Breaks if updateEndTime writes another field, such as the start time.
  @Test
  fun updateEndTimeChangesOnlyTheEndTime() =
      assertOnly({ it.copy(endTime = nine) }) { editing.updateEndTime(nine) }

  // Breaks if showPicker opens another field than the one given, or null does not close it.
  @Test
  fun showPickerOpensTheGivenFieldAndNullClosesIt() {
    assertOnly({ it.copy(openPicker = DateTimeField.END_TIME) }) {
      editing.showPicker(DateTimeField.END_TIME)
    }

    assertOnly({ it.copy(openPicker = null) }) { editing.showPicker(null) }
  }

  // Breaks if pickTime writes another field than the open picker's or leaves the picker open.
  @Test
  fun pickTimeWritesTheFieldWhosePickerIsOpenAndClosesIt() {
    editing.showPicker(DateTimeField.START_TIME)
    assertOnly({ it.copy(startTime = nine, openPicker = null) }) { editing.pickTime(nine) }

    editing.showPicker(DateTimeField.END_TIME)
    assertOnly({ it.copy(endTime = nine, openPicker = null) }) { editing.pickTime(nine) }
  }

  // Breaks if pickTime writes a time while a date picker is open, or leaves that picker open.
  @Test
  fun pickTimeWithADatePickerOpenOnlyClosesIt() {
    editing.showPicker(DateTimeField.START_DATE)

    assertOnly({ it.copy(openPicker = null) }) { editing.pickTime(nine) }
  }
}
