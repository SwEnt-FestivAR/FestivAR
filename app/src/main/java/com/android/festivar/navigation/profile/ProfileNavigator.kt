// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation.profile

import android.content.Intent
import androidx.navigation.NavHostController
import com.android.festivar.MySettingsActivity
import com.android.festivar.navigation.AppScreens

/** The navigation actions of the profile screen. */
class ProfileNavigator(
    private val navController: NavHostController,
    private val startActivity: (Intent) -> Unit = { intent ->
      navController.context.startActivity(intent)
    },
) {
  val onBack: () -> Unit = { navController.popBackStack() }
  // Clears the whole back stack, so the profile screen and its ViewModel are destroyed and the
  // user cannot come back to it once signed out.
  val onSignedOut: () -> Unit = {
    navController.navigate(AppScreens.Login.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }

  val onSettingsClick: () -> Unit = {
    startActivity(Intent(navController.context, MySettingsActivity::class.java))
  }
}
