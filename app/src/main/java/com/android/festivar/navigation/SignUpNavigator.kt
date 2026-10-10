package com.android.festivar.navigation

import androidx.navigation.NavHostController

class SignUpNavigator(private val navController: NavHostController) {
  val onSignInClick: () -> Unit = { navController.navigate(AppScreens.Login.name) }

  val onBackClick: () -> Unit = { navController.popBackStack() }

  val onSignUpSuccess: () -> Unit = {
    navController.navigate(AppScreens.EventOverview.name) {
      popUpTo(AppScreens.SignUp.name) { inclusive = true }
    }
  }
}
