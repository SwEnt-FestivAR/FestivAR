// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.ui.task.create.CreateTaskScreen
import com.android.festivar.ui.task.create.CreateTaskViewModel
import com.android.festivar.ui.task.details.TaskDetailsScreen
import com.android.festivar.ui.task.edit.EditTaskScreen
import com.android.festivar.ui.task.edit.EditTaskViewModel
import com.android.festivar.ui.task.overview.TaskOverviewScreen
import com.android.festivar.ui.task.overview.TaskOverviewViewModel

/** The destinations of the task feature. */
fun NavGraphBuilder.taskGraph(navController: NavHostController, authRepository: AuthRepository) {
  // Reached through AppScreens.taskOverview(eventId)
  composable(
      route = "${AppScreens.TaskOverview.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val navigator = TaskOverviewNavigator(navController)
    val firebaseUser = authRepository.currentUser
    if (firebaseUser == null) {
      // Nobody is signed in, so there is nobody whose tasks to mark as mine
      LaunchedEffect(Unit) { navigator.onSignedOut() }
      return@composable
    }
    val eventId =
        requireNotNull(backStackEntry.arguments?.getString("eventId")) {
          "eventId argument is required"
        }
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    TaskOverviewScreen(
        viewModel =
            viewModel(key = "$eventId:${firebaseUser.uid}") {
              TaskOverviewViewModel(eventId, firebaseUser.uid)
            },
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
    // Asked at each tap on "Create task". Blank when nobody is signed in: the view model then
    // refuses to save (redirect left to #261).
    val currentUserId: () -> String = { authRepository.currentUser?.uid.orEmpty() }
    // Leaving the screen, whether the task was created or not, goes back to the task overview.
    val leave: () -> Unit = { navController.popBackStack() }
    // No key needed: the view model is scoped to this back stack entry, which has its own eventId.
    CreateTaskScreen(
        viewModel = viewModel { CreateTaskViewModel(eventId, currentUserId) },
        onBack = leave,
        onTaskCreated = leave,
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
        onEdit = navigator.onEdit,
    )
  }
  // Reached through AppScreens.editTask(taskId), from the details of a task its owner opened.
  composable(
      route = "${AppScreens.EditTask.name}/{taskId}",
      arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
  ) { backStackEntry ->
    if (authRepository.currentUser == null) {
      LaunchedEffect(Unit) {
        navController.navigate(AppScreens.Login.name) {
          popUpTo(navController.graph.id) { inclusive = true }
        }
      }
      return@composable
    }
    val taskId =
        requireNotNull(backStackEntry.arguments?.getString("taskId")) {
          "taskId argument is required"
        }
    require(taskId.isNotBlank()) { "taskId must not be blank" }
    // Read at load, and again at save, so a sign out in between refuses the save.
    val currentUserId: () -> String = { authRepository.currentUser?.uid.orEmpty() }
    // Leaving the screen, whether the edit was saved or not, goes back to the task details.
    val leave: () -> Unit = { navController.popBackStack() }
    // No key needed: the view model is scoped to this back stack entry, which has its own taskId.
    EditTaskScreen(
        viewModel = viewModel { EditTaskViewModel(taskId, currentUserId) },
        onBack = leave,
        onSaved = leave,
    )
  }
}
