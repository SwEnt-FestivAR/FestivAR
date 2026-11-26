// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import com.android.festivar.model.task.Priority
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.task.create.CreateTaskUiState
import com.android.festivar.ui.task.create.MAX_ESTIMATED_DIGITS
import com.android.festivar.ui.task.create.MIN_MAX_ASSIGN
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests of [TaskFormEditing] over a plain state, without a ViewModel. */
class TaskFormEditingTest {
  private val state = MutableStateFlow(CreateTaskUiState())
  private val editing = TaskFormEditing().also { it.applyEdit = { change -> state.update(change) } }

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

  /** Breaks if updateRange writes another field than the range, or keeps part of the old range. */
  @Test
  fun updateRangeChangesOnlyTheRange() {
    val range = DateTimeRange(day, nine, day.plusDays(1), nine.plusHours(2))

    assertOnly({ it.copy(range = range) }) { editing.updateRange(range) }

    assertOnly({ it.copy(range = DateTimeRange()) }) { editing.updateRange(DateTimeRange()) }
  }
}
