// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import com.android.festivar.ui.components.NavigationSubScreen
import com.android.festivar.ui.components.NavigationTab

/**
 * What the bottom bar shows for one destination: the selected tab, and the sub-screen it is nested
 * under, or null on the start screen of the tab.
 */
data class BottomBarState(
    val selectedTab: NavigationTab,
    val subScreen: NavigationSubScreen? = null,
)

/**
 * The bar for the destination whose route pattern is [route] (as `NavDestination.route` gives it,
 * e.g. "TaskDetails/{taskId}?from={from}"), with [from] the value of the TaskDetails origin
 * argument; null when the screen shows no bar (forms, authentication, unknown routes).
 */
fun bottomBarStateFor(route: String?, from: String?): BottomBarState? {
  return when (AppScreens.fromRoute(route)) {
    AppScreens.EventsOverview -> BottomBarState(NavigationTab.EVENTS)
    AppScreens.EventDetails ->
        BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.EVENT_DETAILS)
    AppScreens.TaskOverview -> BottomBarState(NavigationTab.EVENTS, NavigationSubScreen.EVENT_TASKS)
    AppScreens.TaskDetails ->
        BottomBarState(
            if (from == TaskOrigin.MY_TASKS.name) NavigationTab.MY_TASKS else NavigationTab.EVENTS,
            NavigationSubScreen.TASK_DETAILS,
        )
    AppScreens.MyTasks -> BottomBarState(NavigationTab.MY_TASKS)
    AppScreens.Profile -> BottomBarState(NavigationTab.PROFILE)
    // Forms, authentication and any screen not listed above show no bar.
    else -> null
  }
}
