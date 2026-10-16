// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.ScreenName
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryProvider
import com.android.festivar.ui.event.CreateEventScreen
import com.android.festivar.ui.event.CreateEventViewModel
import com.android.festivar.ui.event.EventsOverviewScreen
import com.android.festivar.ui.event.EventsOverviewViewModel
import com.android.festivar.ui.event.JoinEventScreen
import com.android.festivar.ui.event.JoinEventViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** The destinations of the event feature. */
fun NavGraphBuilder.eventGraph(navController: NavHostController, authRepository: AuthRepository) {
  composable(route = AppScreens.EventsOverview.name) {
    val navigator = EventsOverviewNavigator(navController)
    val firebaseUser = authRepository.currentUser
    if (firebaseUser == null) {
      // Nobody is signed in, so there are no events to show
      LaunchedEffect(Unit) { navigator.onSignedOut() }
    } else {
      EventsOverviewScreen(
          eventsOverviewViewModel =
              viewModel {
                EventsOverviewViewModel(
                    EventRepositoryFirebase(Firebase.firestore),
                    UsersRepositoryProvider.repository,
                    firebaseUser.uid,
                )
              },
          onSelectEvent = navigator.onSelectEvent,
          onJoinEvent = navigator.onJoinEvent,
          onAddEvent = navigator.onAddEvent,
          onSelectAvatar = navigator.onSelectAvatar,
      )
    }
  }
  composable(route = AppScreens.CreateEvent.name) {
    val navigator = CreateEventNavigator(navController)
    val firebaseUser = authRepository.currentUser
    val userId = runCatching { firebaseUser?.uid }.getOrNull() ?: ""
    CreateEventScreen(
        viewModel =
            viewModel {
              CreateEventViewModel(
                  eventsRepository = EventRepositoryFirebase(Firebase.firestore),
                  usersRepository = UsersRepositoryProvider.repository,
                  userId = userId,
              )
            },
        onBack = navigator.onBack,
        onCreated = navigator.onCreated,
    )
  }
  // Reached through AppScreens.eventDetails(eventId)
  composable(
      route = "${AppScreens.EventDetails.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) {
    ScreenName("event details")
  }
  composable(route = AppScreens.JoinEvent.name) {
    val navigator = JoinEventNavigator(navController)
    val firebaseUser = authRepository.currentUser
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
                  User(uid = firebaseUser.uid),
                  EventRepositoryFirebase(Firebase.firestore),
                  UsersRepositoryProvider.repository,
              )
            },
        onBack = navigator.onBack,
        onOpenEvent = navigator.onOpenEvent,
    )
  }
}
