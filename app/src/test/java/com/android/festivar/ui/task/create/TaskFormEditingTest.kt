// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.create

import com.android.festivar.ui.components.DateTimeField
import java.time.LocalDate
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
        it.applyPicker = { change -> state.update(change) }
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
}
