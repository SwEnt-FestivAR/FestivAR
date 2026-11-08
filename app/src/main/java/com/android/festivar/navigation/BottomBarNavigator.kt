// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import com.android.festivar.ui.components.NavigationTab

/**
 * Where the bottom bar goes: each tab opens its top-level screen, back from any tab returns to the
 * events overview.
 */
class BottomBarNavigator(private val navController: NavHostController) {
  fun onTabSelected(tab: NavigationTab) {
    val route =
        when (tab) {
          NavigationTab.EVENTS -> AppScreens.EventsOverview.name
          NavigationTab.MY_TASKS -> AppScreens.MyTasks.name
          NavigationTab.PROFILE -> AppScreens.Profile.name
        }
    navController.navigate(route) {
      popUpTo(AppScreens.EventsOverview.name) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }
}
