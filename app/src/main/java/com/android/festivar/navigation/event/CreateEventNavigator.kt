// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation.event

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

/** Where the create event screen can go: back to the overview, also once the event is saved. */
class CreateEventNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = {
    navController.popBackStack(AppScreens.EventsOverview.name, inclusive = false)
  }

  /** The overview reloads on entry, so the new event is listed on return. */
  val onCreated: () -> Unit = onBack
}
