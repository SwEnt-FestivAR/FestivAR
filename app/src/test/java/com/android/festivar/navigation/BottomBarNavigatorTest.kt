// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import com.android.festivar.ui.components.NavigationSubScreen
import com.android.festivar.ui.components.NavigationTab
import io.mockk.every
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
    navigator.onTabSelected(NavigationTab.EVENTS, BottomBarState(NavigationTab.PROFILE))

    verify {
      navController.navigate(AppScreens.EventsOverview.name, any<NavOptionsBuilder.() -> Unit>())
    }
  }

  // Breaks if the My tasks tab opens another screen than the My tasks destination.
  @Test
  fun theMyTasksTabOpensMyTasks() {
    navigator.onTabSelected(NavigationTab.MY_TASKS, BottomBarState(NavigationTab.EVENTS))

    verify { navController.navigate(AppScreens.MyTasks.name, any<NavOptionsBuilder.() -> Unit>()) }
  }

  // Breaks if the Profile tab opens another screen than the profile.
  @Test
  fun theProfileTabOpensTheProfile() {
    navigator.onTabSelected(NavigationTab.PROFILE, BottomBarState(NavigationTab.EVENTS))

    verify { navController.navigate(AppScreens.Profile.name, any<NavOptionsBuilder.() -> Unit>()) }
  }

  // Breaks if tab switches pile up on the back stack, or back from a tab stops returning to the
  // events overview, or a tab loses its state when the user comes back to it.
  @Test
  fun aTabSwitchIsSingleTopRestoresStateAndPopsToTheEventsOverview() {
    val options = slot<NavOptionsBuilder.() -> Unit>()

    navigator.onTabSelected(NavigationTab.PROFILE, BottomBarState(NavigationTab.EVENTS))

    verify { navController.navigate(AppScreens.Profile.name, capture(options)) }
    val navOptions = navOptions(options.captured)
    assertTrue(navOptions.shouldLaunchSingleTop())
    assertTrue(navOptions.shouldRestoreState())
    assertEquals(AppScreens.EventsOverview.name, navOptions.popUpToRoute)
    assertTrue(navOptions.shouldPopUpToSaveState())
  }

  // Breaks if tapping the selected tab on one of its sub-screens no longer goes back to its start
  // screen, or navigates on top of it instead of popping.
  @Test
  fun reselectingTheNestedTabPopsBackToItsStartScreen() {
    every { navController.popBackStack(AppScreens.EventsOverview.name, false) } returns true

    navigator.onTabSelected(
        NavigationTab.EVENTS,
        BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.EVENT_TASKS),
    )

    verify { navController.popBackStack(AppScreens.EventsOverview.name, false) }
    verify(exactly = 0) {
      navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>())
    }
  }

  // Breaks if a sub-screen reached without its start screen on the stack (a deep link) leaves the
  // user stuck when they tap the tab.
  @Test
  fun reselectingTheNestedTabNavigatesWhenItsStartScreenIsNotOnTheStack() {
    every { navController.popBackStack(AppScreens.MyTasks.name, false) } returns false

    navigator.onTabSelected(
        NavigationTab.MY_TASKS,
        BottomBarState(NavigationTab.MY_TASKS, NavigationSubScreen.TASK_DETAILS),
    )

    verify { navController.navigate(AppScreens.MyTasks.name, any<NavOptionsBuilder.() -> Unit>()) }
  }

  // Breaks if tapping the selected tab on its own start screen pops the stack under it.
  @Test
  fun reselectingTheTabOnItsStartScreenDoesNotPop() {
    navigator.onTabSelected(NavigationTab.EVENTS, BottomBarState(NavigationTab.EVENTS))

    verify(exactly = 0) { navController.popBackStack(any<String>(), any()) }
    verify {
      navController.navigate(AppScreens.EventsOverview.name, any<NavOptionsBuilder.() -> Unit>())
    }
  }
}
