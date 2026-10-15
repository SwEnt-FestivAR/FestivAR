// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.task.Task
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskDetailsScreenHelpersTest {
  private companion object {
    const val RANGE = "%1\$s to %2\$s"
  }

  private val start = LocalDateTime.of(2026, 10, 17, 9, 30)

  private fun task(startTime: LocalDateTime? = null, endTime: LocalDateTime? = null) =
      Task("t", "e", "Title", startTime = startTime, endTime = endTime)

  @Test
  fun whenLine_showsDateAndTimes() {
    assertEquals("Sat 17 Oct, 09:30 to 09:50", whenLine(task(start, start.plusMinutes(20)), RANGE))
  }

  @Test
  fun whenLine_withoutEndShowsOnlyTheStart() {
    assertEquals("Sat 17 Oct, 09:30", whenLine(task(start), RANGE))
  }

  @Test
  fun whenLine_isNullWithoutAStart() {
    assertNull(whenLine(task(), RANGE))
  }
}
