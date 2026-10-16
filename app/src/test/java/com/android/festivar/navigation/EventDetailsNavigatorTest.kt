// Co-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
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
}
