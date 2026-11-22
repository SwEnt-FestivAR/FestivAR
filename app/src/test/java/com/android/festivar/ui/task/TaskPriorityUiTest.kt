// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import com.android.festivar.R
import com.android.festivar.model.task.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskPriorityUiTest {
  /** Breaks if a priority is named by the label of another one, e.g. an urgent task shown High. */
  @Test
  fun eachPriorityHasItsLabel() {
    assertEquals(R.string.priority_none, priorityLabelRes(Priority.NONE))
    assertEquals(R.string.priority_low, priorityLabelRes(Priority.LOW))
    assertEquals(R.string.priority_medium, priorityLabelRes(Priority.MEDIUM))
    assertEquals(R.string.priority_high, priorityLabelRes(Priority.HIGH))
    assertEquals(R.string.priority_urgent, priorityLabelRes(Priority.URGENT))
  }
}
