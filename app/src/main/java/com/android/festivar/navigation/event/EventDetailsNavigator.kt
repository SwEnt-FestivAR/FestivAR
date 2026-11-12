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
  val onAnnouncementsClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.announcementFeed(eventId))
  }
}
