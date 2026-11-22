// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

/** Where the join event screen can go: back or to the joined event. */
class JoinEventNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }

  /** Opens the joined event and drops the join screen, so back from the event skips it. */
  val onOpenEvent: (eventId: String) -> Unit = { eventId ->
    navController.navigate(AppScreens.eventDetails(eventId)) {
      popUpTo(AppScreens.JoinEvent.name) { inclusive = true }
    }
  }
}
