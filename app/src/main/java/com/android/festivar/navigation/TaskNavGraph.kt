// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.ScreenName
import com.android.festivar.model.authentication.AuthRepository
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
  }  composable(route = AppScreens.CreateTask.name) { ScreenName("create task") }
}
