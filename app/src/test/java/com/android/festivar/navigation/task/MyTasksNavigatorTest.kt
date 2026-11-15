// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.task

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.TaskOrigin
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyTasksNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = MyTasksNavigator(navController)

  /**
   * Turns red when a tapped row stops opening the details of that task, or stops telling them they
   * were opened from My tasks.
   */
  @Test
  fun openTaskNavigatesToTaskDetails() {
    navigator.onOpenTask("task-123")

    verify { navController.navigate(AppScreens.taskDetails("task-123", TaskOrigin.MY_TASKS)) }
  }

  /** Turns red when signing out stops opening the login or leaves My tasks under it. */
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
