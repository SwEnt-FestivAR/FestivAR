// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.model.event.Event
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.BottomBarNavigator
import com.android.festivar.navigation.BottomBarState
import com.android.festivar.ui.components.NavigationTab

class EventsOverviewNavigator(private val navController: NavHostController) {
  val onSelectEvent: (Event) -> Unit = { event ->
    navController.navigate(AppScreens.eventDetails(event.eventId))
  }

  val onJoinEvent: () -> Unit = { navController.navigate(AppScreens.JoinEvent.name) }

  val onAddEvent: () -> Unit = { navController.navigate(AppScreens.CreateEvent.name) }

  // The avatar opens the profile as its tab does. A profile pushed on its own would be saved as
  // the state of the Events tab, and that tab would then bring the profile back.
  val onSelectAvatar: () -> Unit = {
    BottomBarNavigator(navController)
        .onTabSelected(NavigationTab.PROFILE, BottomBarState(NavigationTab.EVENTS))
  }

  val onSignedOut: () -> Unit = {
    navController.navigate(AppScreens.Login.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
