// Co-authored-by: Copilot App <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.navigation.event.EventDetailsNavigator
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

  /** Red if the map button opens anything but the venue map of that event. */
  @Test
  fun openMapNavigatesToTheVenueMapRoute() {
    navigator.onOpenMap("event-123")

    verify { navController.navigate(AppScreens.venueMap("event-123")) }
  }

  /** Red if a signed out user on the map is not sent to the login with a cleared back stack. */
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
