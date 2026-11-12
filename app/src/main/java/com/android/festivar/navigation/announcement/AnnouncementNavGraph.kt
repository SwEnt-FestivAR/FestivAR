// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreen
import com.android.festivar.ui.announcement.create.CreateAnnouncementViewModel
import com.android.festivar.ui.announcement.details.AnnouncementDetailsScreen
import com.android.festivar.ui.announcement.details.AnnouncementDetailsViewModel
import com.android.festivar.ui.announcement.feed.AnnouncementFeedScreen
import com.android.festivar.ui.announcement.feed.AnnouncementFeedViewModel

/** The destinations of the announcement feature. */
fun NavGraphBuilder.announcementGraph(
    navController: NavHostController,
    dataRepository: DataRepository,
) {
  composable(
      route = "${AppScreens.AnnouncementDetails.name}/{eventId}/{announcementId}",
      arguments =
          listOf(
              navArgument("eventId") { type = NavType.StringType },
              navArgument("announcementId") { type = NavType.StringType },
          ),
  ) { backStackEntry ->
    val eventId =
        requireNotNull(backStackEntry.arguments?.getString("eventId")) {
          "eventId argument is required"
        }
    val announcementId =
        requireNotNull(backStackEntry.arguments?.getString("announcementId")) {
          "announcementId argument is required"
        }
    require(eventId.isNotBlank()) { "eventId must not be blank" }
    require(announcementId.isNotBlank()) { "announcementId must not be blank" }
    val navigator = AnnouncementDetailsNavigator(navController)
    AnnouncementDetailsScreen(
        viewModel =
            viewModel { AnnouncementDetailsViewModel(eventId, announcementId, dataRepository) },
        onBack = navigator.onBack,
    )
  }
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
    val currentUserId: () -> String = { dataRepository.authRepository.currentUser?.uid.orEmpty() }
    // Leaving the screen, whether the announcement was published or not, goes back. The arrow still
    // takes taps during the exit transition, so only this entry may pop itself, and only once.
    val leave: () -> Unit = {
      if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
    }
    // No key needed: the view model is scoped to this back stack entry, which has its own eventId.
    CreateAnnouncementScreen(
        viewModel =
            viewModel { CreateAnnouncementViewModel(eventId, currentUserId, dataRepository) },
        onBack = leave,
        onPublished = leave,
    )
  }
  // Reached through AppScreens.announcementFeed(eventId). The route carries the id of the event
  // whose announcements are shown.
  composable(
      route = "${AppScreens.AnnouncementFeed.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId =
        requireNotNull(backStackEntry.arguments?.getString("eventId")) {
          "eventId argument is required"
        }
    val navigator = AnnouncementFeedNavigator(navController, backStackEntry)
    AnnouncementFeedScreen(
        viewModel = viewModel { AnnouncementFeedViewModel(eventId, dataRepository) },
        // Every member may add an announcement for now. Limiting it to the event's owner needs the
        // loaded event, so it is left to the feed's view model
        canAddAnnouncement = true,
        onBack = navigator.onBack,
        onAddAnnouncement = { navigator.onAddAnnouncement(eventId) },
        // Will open the details of the announcement (#364)
        onSelectAnnouncement = {},
    )
  }
}
