// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.ui.components.NavigationTab
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarNavigatorTest {
  private val navController = mockk<NavHostController>(relaxed = true)
  private val navigator = BottomBarNavigator(navController)

  // Breaks if the Events tab opens another screen than the events overview.
  @Test
  fun theEventsTabOpensTheEventsOverview() {
    navigator.onTabSelected(NavigationTab.EVENTS)

    verify {
      navController.navigate(AppScreens.EventsOverview.name, any<NavOptionsBuilder.() -> Unit>())
    }
  }

  // Breaks if the My tasks tab opens another screen than the My tasks destination.
  @Test
  fun theMyTasksTabOpensMyTasks() {
    navigator.onTabSelected(NavigationTab.MY_TASKS)

    verify { navController.navigate(AppScreens.MyTasks.name, any<NavOptionsBuilder.() -> Unit>()) }
  }

  // Breaks if the Profile tab opens another screen than the profile.
  @Test
  fun theProfileTabOpensTheProfile() {
    navigator.onTabSelected(NavigationTab.PROFILE)

    verify { navController.navigate(AppScreens.Profile.name, any<NavOptionsBuilder.() -> Unit>()) }
  }

  // Breaks if tab switches pile up on the back stack, or back from a tab stops returning to the
  // events overview, or a tab loses its state when the user comes back to it.
  @Test
  fun aTabSwitchIsSingleTopRestoresStateAndPopsToTheEventsOverview() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onTabSelected(NavigationTab.PROFILE)

    verify { navController.navigate(AppScreens.Profile.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertTrue(navOptions.shouldLaunchSingleTop())
    assertTrue(navOptions.shouldRestoreState())
    assertEquals(AppScreens.EventsOverview.name, navOptions.popUpToRoute)
    assertTrue(navOptions.shouldPopUpToSaveState())
  }
}
