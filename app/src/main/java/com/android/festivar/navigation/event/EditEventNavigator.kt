// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

/** Returns to the event details after leaving or saving the edit screen. */
class EditEventNavigator(
    private val navController: NavHostController,
    eventId: String,
) {
  private val backToEventDetails: () -> Unit = {
    navController.popBackStack(AppScreens.eventDetails(eventId), inclusive = false)
  }

  val onBack: () -> Unit = backToEventDetails
  val onSaved: () -> Unit = backToEventDetails
}
