// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CreateEventNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = CreateEventNavigator(navController)

  @Test
  fun backClickNavigatesToEventsOverview() {
    navigator.onBack()

    verify { navController.popBackStack(AppScreens.EventsOverview.name, inclusive = false) }
  }

  @Test
  fun createdNavigatesBackToEventsOverview() {
    navigator.onCreated()

    verify { navController.popBackStack(AppScreens.EventsOverview.name, inclusive = false) }
  }
}
