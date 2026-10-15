// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class SignUpNavigator(private val navController: NavHostController) {
  val onSignInClick: () -> Unit = {
    navController.popBackStack(AppScreens.Login.name, inclusive = false)
  }

  val onBackClick: () -> Unit = {
    navController.popBackStack(AppScreens.Login.name, inclusive = false)
  }

  val onSignUpSuccess: () -> Unit = {
    navController.navigate(AppScreens.EventOverview.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
