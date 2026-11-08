package com.android.festivar.navigation.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

class EventDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }
  val onTasksButtonClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.taskOverview(eventId))
  }
  val onOpenMap: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.venueMap(eventId))
  }

  /** Sends a signed out user on the venue map to the login and clears the back stack. */
  val onSignedOut: () -> Unit = {
    navController.navigate(AppScreens.Login.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
