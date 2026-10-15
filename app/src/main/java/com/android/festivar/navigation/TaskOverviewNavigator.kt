// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class TaskOverviewNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = {
    navController.popBackStack(AppScreens.EventDetails.name, inclusive = false)
  }

  val onOpenTask: (taskId: String) -> Unit = {}
}
