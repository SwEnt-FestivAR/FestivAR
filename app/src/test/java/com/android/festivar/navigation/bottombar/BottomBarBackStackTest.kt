// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.navigation.bottombar

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.BottomBarNavigator
import com.android.festivar.navigation.bottomBarStateFor
import com.android.festivar.ui.components.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bottom bar on a real back stack: the mocked [BottomBarNavigatorTest] checks which calls are
 * made, this one checks where the user lands.
 */
@RunWith(AndroidJUnit4::class)
class BottomBarBackStackTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigator: BottomBarNavigator

  @Before
  fun setUp() {
    composeTestRule.setContent {
      navController = rememberNavController()
      NavHost(navController, startDestination = AppScreens.EventsOverview.name) {
        listOf(
                AppScreens.EventsOverview,
                AppScreens.EventDetails,
                AppScreens.MyTasks,
                AppScreens.Profile,
            )
            .forEach { screen -> composable(screen.name) {} }
      }
    }
    navigator = BottomBarNavigator(navController)
  }

  private fun currentRoute(): String? {
    composeTestRule.waitForIdle()
    return navController.currentDestination?.route
  }

  private fun open(screen: AppScreens) {
    composeTestRule.runOnUiThread { navController.navigate(screen.name) }
    composeTestRule.waitForIdle()
  }

  /** Taps [tab] on the bar as the screen now shown displays it. */
  private fun tap(tab: NavigationTab) {
    composeTestRule.runOnUiThread {
      navigator.onTabSelected(tab, bottomBarStateFor(navController.currentDestination?.route, null))
    }
    composeTestRule.waitForIdle()
  }

  // Breaks if the Events tab stops working from a tab opened through the bar.
  @Test
  fun theEventsTabLeavesATabOpenedFromTheBar() {
    tap(NavigationTab.PROFILE)
    tap(NavigationTab.EVENTS)

    assertEquals(AppScreens.EventsOverview.name, currentRoute())
  }

  // Breaks if the profile opened from the bar blocks the other tabs.
  @Test
  fun theMyTasksTabLeavesTheProfile() {
    tap(NavigationTab.PROFILE)

    tap(NavigationTab.MY_TASKS)

    assertEquals(AppScreens.MyTasks.name, currentRoute())
  }

  // Breaks if the Events tab forgets the event the user was in when they come back from a tab.
  @Test
  fun theEventsTabComesBackToTheEventLeftOpen() {
    open(AppScreens.EventDetails)
    tap(NavigationTab.MY_TASKS)

    tap(NavigationTab.EVENTS)

    assertEquals(AppScreens.EventDetails.name, currentRoute())
  }

  // Breaks if tab switches pile up, so that back from a tab no longer lands on the events overview.
  @Test
  fun backFromATabReturnsToTheEventsOverview() {
    tap(NavigationTab.PROFILE)
    tap(NavigationTab.MY_TASKS)

    composeTestRule.runOnUiThread { navController.popBackStack() }

    assertEquals(AppScreens.EventsOverview.name, currentRoute())
  }
}
