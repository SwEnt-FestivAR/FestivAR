// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.ui.authentication.login.LoginScreen
import com.android.festivar.ui.authentication.login.LoginViewModel
import com.android.festivar.ui.authentication.signup.SignUpScreen
import com.android.festivar.ui.authentication.signup.SignUpViewModel

/** The destinations of the authentication feature: login and sign up. */
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    credentialManager: CredentialManager,
    authRepository: AuthRepository,
) {
  composable(route = AppScreens.SignUp.name) {
    val navigator = SignUpNavigator(navController)
    SignUpScreen(
        viewModel = viewModel { SignUpViewModel(authRepository) },
        onSignInClick = navigator.onSignInClick,
        onSignUpSuccess = navigator.onSignUpSuccess,
    )
  }
  composable(route = AppScreens.Login.name) {
    val navigator = LoginNavigator(navController)
    LoginScreen(
        credentialManager = credentialManager,
        viewModel = viewModel { LoginViewModel(authRepository) },
        onSignUpClick = navigator.onSignUpClick,
        onSignedIn = navigator.onSignedIn,
    )
  }
}
