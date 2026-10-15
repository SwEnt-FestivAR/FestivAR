package com.android.festivar.navigation

// Co-authored-by: Copilot App

import androidx.navigation.NavHostController

class EventDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }
  val onTasksButtonClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.taskOverview(eventId))
  }
}
