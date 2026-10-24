// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class SignUpNavigator(private val navController: NavHostController) {
  // Also used for the back button as the only way to come to the sign up screen is from the login
  // screen.
  val onSignInClick: () -> Unit = {
    navController.popBackStack(AppScreens.Login.name, inclusive = false)
  }

  val onBackClick: () -> Unit = onSignInClick

  val onSignUpSuccess: () -> Unit = {
    navController.navigate(AppScreens.EventsOverview.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
