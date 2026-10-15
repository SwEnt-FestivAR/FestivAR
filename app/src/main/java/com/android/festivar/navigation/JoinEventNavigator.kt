// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

/**
 * Where the join event screen can go: back, to the joined event, or to the login when signed out.
 */
class JoinEventNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }

  /** Opens the joined event and drops the join screen, so back from the event skips it. */
  val onOpenEvent: (eventId: String) -> Unit = { eventId ->
    navController.navigate(AppScreens.eventDetails(eventId)) {
      popUpTo(AppScreens.JoinEvent.name) { inclusive = true }
    }
  }

  val onSignedOut: () -> Unit = {
    navController.navigate(AppScreens.Login.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
