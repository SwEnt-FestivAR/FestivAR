// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskOverviewNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = TaskOverviewNavigator(navController)

  /** Turns red when back pops to a fixed route, which strands a user who came another way. */
  @Test
  fun backClickPopsBackStack() {
    navigator.onBack()

    verify { navController.popBackStack() }
  }

  @Test
  fun openTaskNavigatesToTaskDetails() {
    navigator.onOpenTask("task-123")

    verify { navController.navigate(AppScreens.taskDetails("task-123")) }
  }

  @Test
  fun addTaskNavigatesToCreateTaskOfThatEvent() {
    navigator.onAddTask("e1")

    verify { navController.navigate(AppScreens.createTask("e1")) }
  }

  /** Turns red when signing out stops opening the login or leaves the overview under it. */
  @Test
  fun signedOutNavigatesToLoginAndClearsBackStack() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onSignedOut()

    verify { navController.navigate(AppScreens.Login.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertEquals(navController.graph.id, navOptions.popUpToId)
    assertTrue(navOptions.isPopUpToInclusive())
  }
}
