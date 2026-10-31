// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class LoginNavigator(private val navController: NavHostController) {
  val onSignUpClick = { navController.navigate(AppScreens.SignUp.name) }

  val onForgotPasswordClick = { navController.navigate(AppScreens.ForgotPassword.name) }

  val onReturnToLogin: () -> Unit = {
    navController.popBackStack(AppScreens.Login.name, inclusive = false)
  }

  val onSignedIn = {
    navController.navigate(AppScreens.EventsOverview.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
