// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.ui.task.create.CreateTaskScreen
import com.android.festivar.ui.tasks.TaskOverviewScreen

/** The destinations of the task feature. */
fun NavGraphBuilder.taskGraph(navController: NavHostController, authRepository: AuthRepository) {
  // navigate to this screen with
  // navController.navigate("${AppScreens.TaskOverview.name}/$eventId")
  composable(
      route = "${AppScreens.TaskOverview.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val navigator = TaskOverviewNavigator(navController)
    TaskOverviewScreen(
        eventId = backStackEntry.arguments?.getString("eventId") ?: "",
        userId = authRepository.currentUser?.uid ?: "",
        onBack = navigator.onBack,
        onOpenTask = navigator.onOpenTask,
    )
  }  // navigate to this screen with
  // navController.navigate("${AppScreens.CreateTask.name}/$eventId")
  composable(
      route = "${AppScreens.CreateTask.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val navigator = CreateTaskNavigator(navController)
    CreateTaskScreen(
        eventId = backStackEntry.arguments?.getString("eventId") ?: "",
        onBack = navigator.onBack,
        onTaskCreated = navigator.onTaskCreated,
    )
  }
}
