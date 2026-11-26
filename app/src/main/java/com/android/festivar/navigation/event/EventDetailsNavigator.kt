package com.android.festivar.navigation.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

class EventDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }
  /** Back to the overview, which reloads on entry, so the deleted event is gone. */
  val onEventDeleted: () -> Unit = {
    navController.popBackStack(AppScreens.EventsOverview.name, inclusive = false)
  }
  val onTasksButtonClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.taskOverview(eventId))
  }
  val onAnnouncementsClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.announcementFeed(eventId))
  }
  val onEdit: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.editEvent(eventId))
  }
}
