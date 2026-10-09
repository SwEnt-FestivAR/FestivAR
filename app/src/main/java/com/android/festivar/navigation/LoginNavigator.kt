package com.android.festivar.navigation

import androidx.navigation.NavHostController

class LoginNavigator(private val navController: NavHostController) {
  val onSignUpClick = { navController.navigate(AppScreens.SignUp.name) }

  val onSignedIn = {
    navController.navigate(AppScreens.EventOverview.name) {
      popUpTo(AppScreens.EventOverview.name) { inclusive = true }
    }
  }
}
