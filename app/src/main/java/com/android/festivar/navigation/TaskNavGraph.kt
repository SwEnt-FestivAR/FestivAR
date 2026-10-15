// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>
package com.android.festivar.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.ScreenName
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.ui.task.details.TaskDetailsScreen
import com.android.festivar.ui.tasks.TaskOverviewScreen

/** The destinations of the task feature. */
fun NavGraphBuilder.taskGraph(navController: NavHostController, authRepository: AuthRepository) {
  // navigate to this screen with
  // navController.navigate(AppScreens.taskOverview(eventId))
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
  }
  composable(
      route = "${AppScreens.TaskDetails.name}/{taskId}",
      arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
    val firebaseUser = authRepository.currentUser
    val navigator = TaskDetailsNavigator(navController)

    if (firebaseUser == null) {
      LaunchedEffect(Unit) {
        navController.navigate(AppScreens.Login.name) {
          popUpTo(navController.graph.id) { inclusive = true }
        }
      }
      return@composable
    }

    val currentUser =
        remember(firebaseUser) {
          User(
              uid = firebaseUser.uid,
              name = firebaseUser.displayName.orEmpty(),
              email = firebaseUser.email.orEmpty(),
          )
        }

    TaskDetailsScreen(
        taskId = taskId,
        currentUser = currentUser,
        onBack = navigator.onBack,
    )
  }
  composable(route = AppScreens.CreateTask.name) { ScreenName("create task") }
}
