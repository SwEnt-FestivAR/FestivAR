// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.CreateEventNavigator
import com.android.festivar.navigation.LoginNavigator
import com.android.festivar.navigation.SignUpNavigator
import com.android.festivar.navigation.TaskOverviewNavigator
import com.android.festivar.ui.authentication.login.LoginScreen
import com.android.festivar.ui.authentication.login.LoginViewModel
import com.android.festivar.ui.authentication.signup.SignUpScreen
import com.android.festivar.ui.authentication.signup.SignUpViewModel
import com.android.festivar.ui.event.CreateEventScreen
import com.android.festivar.ui.tasks.TaskOverviewScreen
import com.android.festivar.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent { AppTheme { Surface(modifier = Modifier.fillMaxSize()) { FestivAR() } } }
  }
}

/**
 * `FestivAR` is the main composable function that sets up the whole app UI. It initializes the
 * navigation controller and defines the navigation graph.
 */
@Composable
fun FestivAR(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    authRepository: AuthRepository = remember { AuthRepositoryFirebase() },
) {
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()

  val startDestination =
      if (authRepository.currentUser != null) AppScreens.EventOverview.name
      else AppScreens.Login.name

  NavHost(
      navController = navController,
      startDestination = startDestination,
  ) {
    composable(route = AppScreens.SignUp.name) {
      val navigator = SignUpNavigator(navController)
      SignUpScreen(
          credentialManager = credentialManager,
          viewModel = viewModel { SignUpViewModel(authRepository) },
          onSignInClick = navigator.onSignInClick,
          onBackClick = navigator.onBackClick,
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
    composable(route = AppScreens.EventOverview.name) {
      ScreenName(
          "event overview, click to logout",
          modifier =
              Modifier.clickable(
                  role = Role.Button,
                  onClick = {
                    scope.launch {
                      authRepository.signOut().onSuccess {
                        navController.navigate(AppScreens.Login.name) {
                          popUpTo(navController.graph.id) { inclusive = true }
                        }
                      }
                    }
                  },
              ),
      )
    }
    composable(route = AppScreens.TaskOverview.name) {
      val navigator = TaskOverviewNavigator(navController)
      TaskOverviewScreen(
          eventId = "",
          onBack = navigator.onBack,
          onOpenTask = navigator.onOpenTask,
      )
    }
    composable(route = AppScreens.CreateEvent.name) {
      val navigator = CreateEventNavigator(navController)
      CreateEventScreen(onBack = navigator.onBack)
    }
    composable(route = AppScreens.EventDetails.name) { ScreenName("event details") }
    composable(route = AppScreens.CreateTask.name) { ScreenName("create task") }
  }
}

@Composable
fun ScreenName(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
