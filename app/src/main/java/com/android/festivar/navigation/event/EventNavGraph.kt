// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.event

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.event.create.CreateEventScreen
import com.android.festivar.ui.event.create.CreateEventViewModel
import com.android.festivar.ui.event.details.EventDetailsScreen
import com.android.festivar.ui.event.details.EventDetailsViewModel
import com.android.festivar.ui.event.edit.EditEventScreen
import com.android.festivar.ui.event.edit.EditEventViewModel
import com.android.festivar.ui.event.join.JoinEventScreen
import com.android.festivar.ui.event.join.JoinEventViewModel
import com.android.festivar.ui.event.overview.EventsOverviewScreen
import com.android.festivar.ui.event.overview.EventsOverviewViewModel

internal fun requireEventId(eventId: String): String {
  require(eventId.isNotBlank()) { "eventId must not be blank" }
  return eventId
}

/** The destinations of the event feature. */
fun NavGraphBuilder.eventGraph(navController: NavHostController, dataRepository: DataRepository) {
  composable(route = AppScreens.EventsOverview.name) {
    val navigator = EventsOverviewNavigator(navController)
    val firebaseUser = dataRepository.authRepository.currentUser
    if (firebaseUser == null) {
      // Nobody is signed in, so there are no events to show
      LaunchedEffect(Unit) { navigator.onSignedOut() }
    } else {
      EventsOverviewScreen(
          eventsOverviewViewModel =
              viewModel {
                EventsOverviewViewModel(
                    dataRepository,
                    firebaseUser.uid,
                )
              },
          onSelectEvent = navigator.onSelectEvent,
          onJoinEvent = navigator.onJoinEvent,
          onAddEvent = navigator.onAddEvent,
      )
    }
  }
  composable(route = AppScreens.CreateEvent.name) { backStackEntry ->
    val navigator = CreateEventNavigator(navController)
    val leave: () -> Unit = {
      if (navController.currentBackStackEntry == backStackEntry) navigator.onBack()
    }
    val firebaseUser = dataRepository.authRepository.currentUser
    if (firebaseUser == null) {
      LaunchedEffect(Unit) { navigator.onSignedOut() }
    } else {
      CreateEventScreen(
          viewModel =
              viewModel {
                CreateEventViewModel(
                    dataRepository = dataRepository,
                    userId = firebaseUser.uid,
                )
              },
          onBack = leave,
          onCreated = leave,
      )
    }
  }
  // Reached through AppScreens.eventDetails(eventId)
  composable(
      route = "${AppScreens.EventDetails.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    val navigator = EventDetailsNavigator(navController)
    val currentUserId = dataRepository.authRepository.currentUser?.uid.orEmpty()
    // Keyed by user: the view model knows who the owner is, so another sign-in needs a new one
    EventDetailsScreen(
        viewModel(key = "$eventId:$currentUserId") {
          EventDetailsViewModel(dataRepository, eventId, currentUserId)
        },
        onBack = navigator.onBack,
        onTasksButtonClicked = { navigator.onTasksButtonClicked(eventId) },
        onAnnouncementsClicked = { navigator.onAnnouncementsClicked(eventId) },
        onEditClicked = { navigator.onEdit(eventId) },
    )
  }
  composable(
      route = "${AppScreens.EditEvent.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId = requireEventId(backStackEntry.arguments?.getString("eventId") ?: "")
    val currentUserId: () -> String = { dataRepository.authRepository.currentUser?.uid.orEmpty() }
    val navigator = EditEventNavigator(navController, eventId)
    EditEventScreen(
        eventId = eventId,
        viewModel =
            viewModel { EditEventViewModel(dataRepository.eventsRepository, currentUserId) },
        onBack = navigator.onBack,
        onSaved = navigator.onSaved,
    )
  }
  composable(route = AppScreens.JoinEvent.name) {
    val navigator = JoinEventNavigator(navController)
    val firebaseUser = dataRepository.authRepository.currentUser
    if (firebaseUser == null) {
      // Nobody is signed in, so there is nobody to join as
      LaunchedEffect(Unit) { navigator.onSignedOut() }
      return@composable
    }
    // The ViewModel reads the stored profile back by uid before it writes, so the uid is enough
    JoinEventScreen(
        viewModel =
            viewModel {
              JoinEventViewModel(
                  firebaseUser.uid,
                  dataRepository,
              )
            },
        onBack = navigator.onBack,
        onOpenEvent = navigator.onOpenEvent,
    )
  }
}
