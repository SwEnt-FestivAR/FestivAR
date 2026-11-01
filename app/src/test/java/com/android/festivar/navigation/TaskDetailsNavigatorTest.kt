// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDetailsNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = TaskDetailsNavigator(navController)

  @Test
  fun backClickPopsBackStack() {
    navigator.onBack()

    verify { navController.popBackStack() }
  }

  // Breaks if edit opens another route, or the navigation drops launchSingleTop.
  @Test
  fun editOpensTheEditTaskRouteSingleTop() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onEdit("t1")

    verify { navController.navigate(AppScreens.editTask("t1"), capture(options)) }
    assertTrue(navOptions(options.captured).shouldLaunchSingleTop())
  }
}
