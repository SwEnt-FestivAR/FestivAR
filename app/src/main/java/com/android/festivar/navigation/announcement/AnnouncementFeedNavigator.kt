// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.android.festivar.navigation.AppScreens

/**
 * Navigation out of the announcement feed.
 *
 * @param backStackEntry The feed's own entry, the only one [onBack] may pop.
 */
class AnnouncementFeedNavigator(
    private val navController: NavHostController,
    private val backStackEntry: NavBackStackEntry,
) {
  // The arrow still takes taps during the exit transition, so only this entry may pop itself, and
  // only once
  val onBack: () -> Unit = {
    if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
  }

  val onAddAnnouncement: (String) -> Unit = { eventId ->
    navController.navigate(AppScreens.createAnnouncement(eventId))
  }

  val onSelectAnnouncement: (String, String) -> Unit = { eventId, announcementId ->
    navController.navigate(AppScreens.announcementDetails(eventId, announcementId))
  }
}
