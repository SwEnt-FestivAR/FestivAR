// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

/** Where the announcement details can go: back to what opened them, or to the edit. */
class AnnouncementDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }

  /** Opens the edit of the announcement, once even if the edit button is tapped twice. */
  val onEdit: (eventId: String, announcementId: String) -> Unit = { eventId, announcementId ->
    navController.navigate(AppScreens.editAnnouncement(eventId, announcementId)) {
      launchSingleTop = true
    }
  }
}
