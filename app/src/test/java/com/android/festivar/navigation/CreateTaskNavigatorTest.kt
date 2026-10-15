// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CreateTaskNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = CreateTaskNavigator(navController)

  @Test
  fun backClickPopsBackToTaskOverview() {
    navigator.onBack()

    verify(exactly = 1) { navController.popBackStack() }
  }

  @Test
  fun taskCreatedPopsBackToTaskOverview() {
    navigator.onTaskCreated()

    verify(exactly = 1) { navController.popBackStack() }
  }
}
