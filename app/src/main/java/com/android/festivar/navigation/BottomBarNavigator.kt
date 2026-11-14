// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import com.android.festivar.ui.components.NavigationTab

/**
 * Where the bottom bar goes: each tab opens its start screen, back from any tab returns to the
 * events overview, and tapping the selected tab while it is nested under a sub-screen goes back to
 * its start screen, as the design asks.
 */
class BottomBarNavigator(private val navController: NavHostController) {
  /**
   * Opens [tab]. [current] is what the bar shows now: when the user taps the tab already selected
   * and nested, the back stack is popped to the start screen of that tab instead.
   */
  fun onTabSelected(tab: NavigationTab, current: BottomBarState?) {
    val route = startRoute(tab)
    val reselectedWhileNested = current?.selectedTab == tab && current.subScreen != null
    if (reselectedWhileNested && navController.popBackStack(route, inclusive = false)) return
    navController.navigate(route) {
      popUpTo(AppScreens.EventsOverview.name) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  private fun startRoute(tab: NavigationTab): String =
      when (tab) {
        NavigationTab.EVENTS -> AppScreens.EventsOverview.name
        NavigationTab.MY_TASKS -> AppScreens.MyTasks.name
        NavigationTab.PROFILE -> AppScreens.Profile.name
      }
}
