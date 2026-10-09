// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppScreensTest {
  @Test
  fun containsAllApplicationDestinations() {
    assertEquals(
        listOf("Login", "SignUp", "TaskOverview", "EventOverview", "CreateEvent", "CreateTask"),
        AppScreens.entries.map { it.name },
    )
  }

  @Test
  fun destinationNamesAreUnique() {
    val names = AppScreens.entries.map { it.name }

    assertEquals(names.size, names.toSet().size)
    assertTrue(names.all { it.isNotBlank() })
  }
}
