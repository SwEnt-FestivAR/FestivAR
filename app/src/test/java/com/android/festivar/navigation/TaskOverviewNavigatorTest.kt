// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TaskOverviewNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = TaskOverviewNavigator(navController)

  @Test
  fun backClickNavigatesToEventDetails() {
    navigator.onBack()

    verify { navController.popBackStack(AppScreens.EventDetails.name, inclusive = false) }
  }

  @Test
  fun openTaskNavigatesToTaskDetails() {
    navigator.onOpenTask("task-123")

    verify { navController.navigate(AppScreens.taskDetails("task-123")) }
  }
}
