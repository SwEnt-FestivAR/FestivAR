// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.ui.task.create.CreateTaskScreen
import com.android.festivar.ui.task.details.TaskDetailsScreen
import com.android.festivar.ui.tasks.TaskOverviewScreen

/** The destinations of the task feature. */
fun NavGraphBuilder.taskGraph(navController: NavHostController, authRepository: AuthRepository) {
  // Reached through AppScreens.taskOverview(eventId)
  composable(
      route = "${AppScreens.TaskOverview.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val navigator = TaskOverviewNavigator(navController)
    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
    TaskOverviewScreen(
        eventId = eventId,
        userId = authRepository.currentUser?.uid ?: "",
        onBack = navigator.onBack,
        onOpenTask = navigator.onOpenTask,
        onAddTask = { navigator.onAddTask(eventId) },
    )
  }
  // The route carries the id of the event the new task belongs to.
  composable(
      route = "${AppScreens.CreateTask.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId =
        requireNotNull(backStackEntry.arguments?.getString("eventId")) {
          "eventId argument is required"
        }
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    val navigator = CreateTaskNavigator(navController)
    CreateTaskScreen(
        eventId = eventId,
        // Blank when nobody is signed in: the view model then refuses to save. Read once, when the
        // route is composed, so it does not follow a sign-out while the screen is open. The blank
        // fallback and that missing follow-up are left to the signed-out redirect of #261, which
        // will replace them.
        ownerId = authRepository.currentUser?.uid.orEmpty(),
        onBack = navigator.onBack,
        onTaskCreated = navigator.onTaskCreated,
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
}
