// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.task

import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.user.User
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.RequireSignedIn
import com.android.festivar.navigation.TaskOrigin
import com.android.festivar.ui.task.create.CreateTaskScreen
import com.android.festivar.ui.task.create.CreateTaskViewModel
import com.android.festivar.ui.task.details.TaskDetailsScreen
import com.android.festivar.ui.task.details.TaskDetailsViewModel
import com.android.festivar.ui.task.details.VoiceNoteServices
import com.android.festivar.ui.task.edit.EditTaskScreen
import com.android.festivar.ui.task.edit.EditTaskViewModel
import com.android.festivar.ui.task.mytasks.MyTasksScreen
import com.android.festivar.ui.task.mytasks.MyTasksViewModel
import com.android.festivar.ui.task.overview.TaskOverviewScreen
import com.android.festivar.ui.task.overview.TaskOverviewViewModel

/**
 * The destinations of the task feature.
 *
 * @param voiceNoteServices creates what records, keeps and plays the voice note of a task, once for
 *   each Task details screen (which releases them when it is left). Without them the voice note is
 *   read-only.
 */
fun NavGraphBuilder.taskGraph(
    navController: NavHostController,
    dataRepository: DataRepository,
    voiceNoteServices: () -> VoiceNoteServices? = { null },
) {
  // Reached through AppScreens.taskOverview(eventId)
  composable(
      route = "${AppScreens.TaskOverview.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val navigator = TaskOverviewNavigator(navController)
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      // Checked once signed in, so a signed-out user on a blank route still reaches the login
      val eventId =
          requireNotNull(backStackEntry.arguments?.getString("eventId")) {
            "eventId argument is required"
          }
      require(eventId.isNotBlank()) { "eventId must not be blank" }
      TaskOverviewScreen(
          viewModel =
              viewModel(key = "$eventId:${firebaseUser.uid}") {
                TaskOverviewViewModel(eventId, firebaseUser.uid, dataRepository)
              },
          onBack = navigator.onBack,
          onOpenTask = navigator.onOpenTask,
          onAddTask = { navigator.onAddTask(eventId) },
      )
    }
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
    // Leaving the screen, whether the task was created or not, goes back to the task overview.
    val leave: () -> Unit = {
      if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
    }
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      // No key needed: the view model is scoped to this back stack entry, which has its own
      // eventId.
      CreateTaskScreen(
          viewModel =
              viewModel { CreateTaskViewModel(eventId, { firebaseUser.uid }, dataRepository) },
          onBack = leave,
          onTaskCreated = leave,
      )
    }
  }
  composable(
      route = "${AppScreens.TaskDetails.name}/{taskId}?from={from}",
      arguments =
          listOf(
              navArgument("taskId") { type = NavType.StringType },
              // Where the details were opened from, read by the bottom bar only.
              navArgument("from") {
                type = NavType.StringType
                defaultValue = TaskOrigin.EVENT.name
              },
          ),
  ) { backStackEntry ->
    val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
    val navigator = TaskDetailsNavigator(navController)
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      val currentUser =
          remember(firebaseUser) {
            User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName.orEmpty(),
                email = firebaseUser.email.orEmpty(),
            )
          }
      TaskDetailsScreen(
          viewModel =
              viewModel(key = "$taskId:${currentUser.uid}") {
                TaskDetailsViewModel(taskId, currentUser, dataRepository, voiceNoteServices())
              },
          onBack = navigator.onBack,
          onEdit = navigator.onEdit,
          onTaskDeleted = navigator.onTaskDeleted,
      )
    }
  }
  // Reached through AppScreens.editTask(taskId), from the details of a task its owner opened.
  composable(
      route = "${AppScreens.EditTask.name}/{taskId}",
      arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
  ) { backStackEntry ->
    // Leaving the screen, whether the edit was saved or not, goes back to the task details.
    val leave: () -> Unit = { navController.popBackStack() }
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      // Checked once signed in, so a signed-out user on a blank route still reaches the login
      val taskId =
          requireNotNull(backStackEntry.arguments?.getString("taskId")) {
            "taskId argument is required"
          }
      require(taskId.isNotBlank()) { "taskId must not be blank" }
      // No key needed: the view model is scoped to this back stack entry, which has its own taskId.
      EditTaskScreen(
          viewModel = viewModel { EditTaskViewModel(taskId, { firebaseUser.uid }, dataRepository) },
          onBack = leave,
          onSaved = leave,
      )
    }
  }

  // Every task the signed-in user has taken, across their events. Its tab on the bottom bar opens
  // it.
  composable(AppScreens.MyTasks.name) {
    val navigator = MyTasksNavigator(navController)
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      MyTasksScreen(
          viewModel =
              viewModel(key = firebaseUser.uid) {
                MyTasksViewModel(firebaseUser.uid, dataRepository)
              },
          onOpenTask = navigator.onOpenTask,
      )
    }
  }
}
