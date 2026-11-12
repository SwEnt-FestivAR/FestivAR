// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.task

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

/** Where My tasks can go: to the details of a task, or to the login when signed out. */
class MyTasksNavigator(private val navController: NavHostController) {
  /** Opens the details of the tapped task; back returns to My tasks. */
  val onOpenTask: (taskId: String) -> Unit = { taskId ->
    navController.navigate(AppScreens.taskDetails(taskId))
  }

  /** Opens the login and clears the back stack, so back cannot return to My tasks. */
  val onSignedOut: () -> Unit = {
    navController.navigate(AppScreens.Login.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
