// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreen
import com.android.festivar.ui.announcement.create.CreateAnnouncementViewModel

/** The destinations of the announcement feature. */
fun NavGraphBuilder.announcementGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
) {
  // Reached through AppScreens.createAnnouncement(eventId). The route carries the id of the event
  // the new announcement belongs to.
  composable(
      route = "${AppScreens.CreateAnnouncement.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId =
        requireNotNull(backStackEntry.arguments?.getString("eventId")) {
          "eventId argument is required"
        }
    // Asked at each tap on "Publish". Blank when nobody is signed in: the view model then refuses
    // to save (redirect left to #261).
    val currentUserId: () -> String = { authRepository.currentUser?.uid.orEmpty() }
    // Leaving the screen, whether the announcement was published or not, goes back. The arrow still
    // takes taps during the exit transition, so only this entry may pop itself, and only once.
    val leave: () -> Unit = {
      if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
    }
    // No key needed: the view model is scoped to this back stack entry, which has its own eventId.
    CreateAnnouncementScreen(
        viewModel = viewModel { CreateAnnouncementViewModel(eventId, currentUserId) },
        onBack = leave,
        onPublished = leave,
    )
  }
}
