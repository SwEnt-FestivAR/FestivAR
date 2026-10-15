// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

/** The navigation actions of the profile screen. */
class ProfileNavigator(private val navController: NavHostController) {
    val onBack: () -> Unit = {
        navController.popBackStack(AppScreens.EventOverview.name, inclusive = false)
    }
    // Clears the whole back stack, so the profile screen and its ViewModel are destroyed and the
    // user cannot come back to it once signed out.
    val onSignedOut: () -> Unit = {
        navController.navigate(AppScreens.Login.name) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
}