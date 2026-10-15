// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController
import com.android.festivar.model.event.Event

class EventsOverviewNavigator(private val navController: NavHostController) {
  // TODO: pass the event id once the event details screen is in the navigation graph
  val onSelectEvent: (Event) -> Unit = { navController.navigate(AppScreens.EventDetails.name) }

  val onJoinEvent: () -> Unit = { navController.navigate(AppScreens.JoinEvent.name) }

  val onAddEvent: () -> Unit = { navController.navigate(AppScreens.CreateEvent.name) }

  val onSelectAvatar: () -> Unit = { navController.navigate(AppScreens.Profile.name) }

  val onSignedOut: () -> Unit = {
    navController.navigate(AppScreens.Login.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
