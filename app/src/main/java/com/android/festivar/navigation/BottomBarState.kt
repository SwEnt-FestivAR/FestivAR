// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import com.android.festivar.ui.components.NavigationTab

/** What the bottom bar shows for one destination. */
data class BottomBarState(val selectedTab: NavigationTab, val inEventTasks: Boolean = false)

/**
 * The bar for the destination whose route pattern is [route] (as `NavDestination.route` gives it,
 * e.g. "TaskDetails/{taskId}?from={from}"), with [from] the value of the TaskDetails origin
 * argument; null when the screen shows no bar (forms, authentication, unknown routes).
 */
fun bottomBarStateFor(route: String?, from: String?): BottomBarState? {
  val name = route?.substringBefore('/')?.substringBefore('?')
  return when (AppScreens.entries.firstOrNull { it.name == name }) {
    AppScreens.EventsOverview,
    AppScreens.EventDetails -> BottomBarState(NavigationTab.EVENTS)
    AppScreens.TaskOverview -> BottomBarState(NavigationTab.EVENTS, inEventTasks = true)
    AppScreens.TaskDetails ->
        if (from == TaskOrigin.MY_TASKS.name) BottomBarState(NavigationTab.MY_TASKS)
        else BottomBarState(NavigationTab.EVENTS, inEventTasks = true)
    AppScreens.MyTasks -> BottomBarState(NavigationTab.MY_TASKS)
    AppScreens.Profile -> BottomBarState(NavigationTab.PROFILE)
    AppScreens.Login,
    AppScreens.SignUp,
    AppScreens.ForgotPassword,
    AppScreens.JoinEvent,
    AppScreens.CreateEvent,
    AppScreens.CreateTask,
    AppScreens.CreateAnnouncement,
    AppScreens.EditTask,
    null -> null
  }
}
