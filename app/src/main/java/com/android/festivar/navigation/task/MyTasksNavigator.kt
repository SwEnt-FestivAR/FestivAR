// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.task

import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.TaskOrigin

/** Where My tasks can go: to the details of a task. */
class MyTasksNavigator(private val navController: NavHostController) {
  /**
   * Opens the details of the tapped task, telling them they were opened from My tasks so the bottom
   * bar keeps that tab selected; back returns to My tasks.
   */
  val onOpenTask: (taskId: String) -> Unit = { taskId ->
    navController.navigate(AppScreens.taskDetails(taskId, TaskOrigin.MY_TASKS))
  }
}
