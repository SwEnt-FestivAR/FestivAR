package com.android.festivar.navigation.event

// Co-authored-by: Copilot App

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

class EventDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }
  val onTasksButtonClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.taskOverview(eventId))
  }
}
