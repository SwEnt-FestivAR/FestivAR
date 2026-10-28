// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
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
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.user.User
import com.android.festivar.ui.task.create.CreateTaskScreen
import com.android.festivar.ui.task.create.CreateTaskViewModel
import com.android.festivar.ui.task.details.TaskDetailsScreen
import com.android.festivar.ui.tasks.TaskOverviewScreen
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** The destinations of the task feature. */
fun NavGraphBuilder.taskGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    // Created when the Create task screen opens, so that the other screens do not need Firebase.
    eventsRepository: () -> EventsRepository = { EventRepositoryFirebase(Firebase.firestore) },
) {
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
    // Blank when nobody is signed in: the view model then refuses to save (redirect left to #261).
    val ownerId = authRepository.currentUser?.uid.orEmpty()
    // Leaving the screen, whether the task was created or not, goes back to the task overview.
    val leave: () -> Unit = { navController.popBackStack() }
    CreateTaskScreen(
        viewModel = viewModel { CreateTaskViewModel(eventId, ownerId, eventsRepository()) },
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
    )
  }
}
