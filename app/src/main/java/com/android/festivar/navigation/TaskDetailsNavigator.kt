// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

/** Where the task details can go: back to whatever opened them, or to the edit of the task. */
class TaskDetailsNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }

  /** Opens the edit of the task, once even if the edit button is tapped twice. */
  val onEdit: (taskId: String) -> Unit = { taskId ->
    navController.navigate(AppScreens.editTask(taskId)) { launchSingleTop = true }
  }
}
