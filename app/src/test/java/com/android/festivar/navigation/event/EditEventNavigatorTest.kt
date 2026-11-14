// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class EditEventNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = EditEventNavigator(navController, "event-123")

  @Test
  fun backReturnsToThisEventsDetails() {
    navigator.onBack()

    verify { navController.popBackStack(AppScreens.eventDetails("event-123"), false) }
  }

  @Test
  fun saveReturnsToThisEventsDetails() {
    navigator.onSaved()

    verify { navController.popBackStack(AppScreens.eventDetails("event-123"), false) }
  }

  @Test
  fun editRouteIncludesEventId() {
    assertEquals("EditEvent/event-123", AppScreens.editEvent("event-123"))
  }
}
