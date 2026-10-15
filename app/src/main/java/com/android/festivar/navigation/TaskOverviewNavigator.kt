// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class TaskOverviewNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = {
    navController.popBackStack("${AppScreens.EventDetails.name}/{eventId}", inclusive = false)
  }

  val onOpenTask: (taskId: String) -> Unit = { taskId ->
    navController.navigate(AppScreens.taskDetails(taskId))
  }

  val onAddTask: (eventId: String) -> Unit = { eventId ->
    navController.navigate(AppScreens.createTask(eventId))
  }
}
