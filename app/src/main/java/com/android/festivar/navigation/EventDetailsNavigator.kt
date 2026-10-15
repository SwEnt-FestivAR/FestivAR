package com.android.festivar.navigation

import androidx.navigation.NavHostController

class EventDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }
  val onTasksButtonClicked: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.taskOverview(eventId))
  }
}
