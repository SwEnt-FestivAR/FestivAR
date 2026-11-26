// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.announcement

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * Where the edit announcement screen can go: back to the details, whether the edit was saved or
 * not.
 *
 * @param backStackEntry The edit's own entry, the only one [onBack] and [onSaved] may pop.
 */
class EditAnnouncementNavigator(
    private val navController: NavHostController,
    private val backStackEntry: NavBackStackEntry,
) {
  // The arrow still takes taps during the exit transition, so only this entry may pop itself, and
  // only once.
  private val backToDetails: () -> Unit = {
    if (navController.currentBackStackEntry == backStackEntry) navController.popBackStack()
  }

  val onBack: () -> Unit = backToDetails

  /** The details reload on resume, so they show the edit on return. */
  val onSaved: () -> Unit = backToDetails
}
