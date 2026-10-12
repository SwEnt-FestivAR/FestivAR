// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class LoginNavigator(private val navController: NavHostController) {
  val onSignUpClick = { navController.navigate(AppScreens.SignUp.name) }

  val onSignedIn = {
    navController.navigate(AppScreens.EventOverview.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
