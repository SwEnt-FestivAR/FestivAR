// Co-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class EventDetailsNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = EventDetailsNavigator(navController)

  @Test
  fun backClickPopsBackStack() {
    navigator.onBack()

    verify { navController.popBackStack() }
  }

  @Test
  fun tasksButtonNavigatesToTaskOverviewOfThatEvent() {
    navigator.onTasksButtonClicked("event-123")

    verify { navController.navigate(AppScreens.taskOverview("event-123")) }
  }

  /** Breaks if Edit event opens a screen other than the Edit Event screen of that event. */
  @Test
  fun editNavigatesToEditEventOfThatEvent() {
    navigator.onEdit("event-123")

    verify { navController.navigate(AppScreens.editEvent("event-123")) }
  }
}
