// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.RequireSignedIn
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreen
import com.android.festivar.ui.announcement.create.CreateAnnouncementViewModel
import com.android.festivar.ui.announcement.details.AnnouncementDetailsScreen
import com.android.festivar.ui.announcement.details.AnnouncementDetailsViewModel
import com.android.festivar.ui.announcement.edit.EditAnnouncementScreen
import com.android.festivar.ui.announcement.edit.EditAnnouncementViewModel
import com.android.festivar.ui.announcement.feed.AnnouncementFeedScreen
import com.android.festivar.ui.announcement.feed.AnnouncementFeedViewModel

private const val EVENT_ID_REQUIRED = "eventId argument is required"

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
    val (eventId, announcementId) =
        requireAnnouncementDetailsArguments(
            backStackEntry.arguments?.getString("eventId"),
            backStackEntry.arguments?.getString("announcementId"),
        )
    val navigator = AnnouncementDetailsNavigator(navController)
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      // Keyed by user: the view model knows whether the user may edit, so another sign-in needs a
      // new one
      AnnouncementDetailsScreen(
          viewModel =
              viewModel(key = "$eventId:$announcementId:${firebaseUser.uid}") {
                AnnouncementDetailsViewModel(
                    eventId,
                    announcementId,
                    { firebaseUser.uid },
                    dataRepository,
                )
              },
          onBack = navigator.onBack,
          onEdit = { navigator.onEdit(eventId, announcementId) },
      )
    }
  }
  // Reached through AppScreens.editAnnouncement(eventId, announcementId), from the details of an
  // announcement the organizer of the event opened.
  composable(
      route = "${AppScreens.EditAnnouncement.name}/{eventId}/{announcementId}",
      arguments =
          listOf(
              navArgument("eventId") { type = NavType.StringType },
              navArgument("announcementId") { type = NavType.StringType },
          ),
  ) { backStackEntry ->
    val (eventId, announcementId) =
        requireAnnouncementDetailsArguments(
            backStackEntry.arguments?.getString("eventId"),
            backStackEntry.arguments?.getString("announcementId"),
        )
    // Leaving the screen, whether the edit was saved or not, goes back to the details. The arrow
    // still takes taps during the exit transition, so only this entry may pop itself, and only
    // once.
    val leave: () -> Unit = {
      if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
    }
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      // No key needed: the view model is scoped to this back stack entry, which has its own ids.
      EditAnnouncementScreen(
          viewModel =
              viewModel {
                EditAnnouncementViewModel(
                    eventId,
                    announcementId,
                    { firebaseUser.uid },
                    dataRepository,
                )
              },
          onBack = leave,
          onSaved = leave,
      )
    }
  }
  // Reached through AppScreens.createAnnouncement(eventId). The route carries the id of the event
  // the new announcement belongs to.
  composable(
      route = "${AppScreens.CreateAnnouncement.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId = requireEventId(backStackEntry)
    // Leaving the screen, whether the announcement was published or not, goes back. The arrow still
    // takes taps during the exit transition, so only this entry may pop itself, and only once.
    val leave: () -> Unit = {
      if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
    }
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      // No key needed: the view model is scoped to this back stack entry, which has its own
      // eventId.
      CreateAnnouncementScreen(
          viewModel =
              viewModel {
                CreateAnnouncementViewModel(eventId, { firebaseUser.uid }, dataRepository)
              },
          onBack = leave,
          onPublished = leave,
      )
    }
  }
  // Reached through AppScreens.announcementFeed(eventId). The route carries the id of the event
  // whose announcements are shown.
  composable(
      route = "${AppScreens.AnnouncementFeed.name}/{eventId}",
      arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
  ) { backStackEntry ->
    val eventId = requireEventId(backStackEntry)
    val navigator = AnnouncementFeedNavigator(navController, backStackEntry)
    AnnouncementFeedScreen(
        viewModel = viewModel { AnnouncementFeedViewModel(eventId, dataRepository) },
        // Every member may add an announcement for now. Limiting it to the event's owner needs the
        // loaded event, so it is left to the feed's view model
        canAddAnnouncement = true,
        onBack = navigator.onBack,
        onAddAnnouncement = { navigator.onAddAnnouncement(eventId) },
        onSelectAnnouncement = { announcement ->
          navigator.onSelectAnnouncement(eventId, announcement.announcementId)
        },
    )
  }
}

internal fun requireEventId(eventId: String?): String =
    requireNotNull(eventId) { EVENT_ID_REQUIRED }

internal fun requireEventId(backStackEntry: NavBackStackEntry): String =
    requireEventId(backStackEntry.arguments?.getString("eventId"))

internal fun requireAnnouncementDetailsArguments(
    eventId: String?,
    announcementId: String?,
): Pair<String, String> {
  val requiredEventId = requireEventId(eventId)
  val requiredAnnouncementId =
      requireNotNull(announcementId) { "announcementId argument is required" }
  require(requiredEventId.isNotBlank()) { "eventId must not be blank" }
  require(requiredAnnouncementId.isNotBlank()) { "announcementId must not be blank" }
  return requiredEventId to requiredAnnouncementId
}
