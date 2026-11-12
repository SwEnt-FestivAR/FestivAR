// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppScreensTest {
  // Breaks if a task opened from an event stops carrying its origin, so the bar loses the badge.
  @Test
  fun taskDetailsDefaultsToTheEventOrigin() {
    assertEquals("TaskDetails/t1?from=EVENT", AppScreens.taskDetails("t1"))
  }

  // Breaks if the My tasks origin is dropped from the route, so the bar selects Events instead.
  @Test
  fun taskDetailsFromMyTasksCarriesTheMyTasksOrigin() {
    assertEquals(
        "TaskDetails/t1?from=MY_TASKS",
        AppScreens.taskDetails("t1", TaskOrigin.MY_TASKS),
    )
  }
}
