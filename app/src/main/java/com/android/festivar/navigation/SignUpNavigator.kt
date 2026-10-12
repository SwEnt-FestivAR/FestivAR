package com.android.festivar.navigation

import androidx.navigation.NavHostController

class SignUpNavigator(private val navController: NavHostController) {
  val onSignInClick: () -> Unit = { navController.popBackStack() }

  val onBackClick: () -> Unit = { navController.popBackStack() }

  val onSignUpSuccess: () -> Unit = {
    navController.navigate(AppScreens.EventOverview.name) {
      popUpTo(navController.graph.id) { inclusive = true }
    }
  }
}
