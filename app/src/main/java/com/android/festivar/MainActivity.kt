package com.android.festivar

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.android.festivar.ui.authentication.login.LoginScreen
import com.android.festivar.ui.authentication.signup.SignUpScreen
import com.android.festivar.ui.theme.AppTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

const val EMU_IP = "127.0.0.1"

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // Copied from bootcamp
    Log.d("FirebaseConfig", "DEBUG=${BuildConfig.DEBUG}")

    if (BuildConfig.DEBUG) {
      Firebase.firestore.useEmulator(EMU_IP, 8080)
      Firebase.auth.useEmulator(EMU_IP, 9099)
    }

    super.onCreate(savedInstanceState)

    setContent { AppTheme { Surface(modifier = Modifier.fillMaxSize()) { FestivAR() } } }
  }
}

// This enum defines the different screens in the app, used for navigation purposes.
// Put only fully implemented screens here
enum class AppScreens {
  Login,
  SignUp,
  TaskOverview,
  EventOverview,
  CreateEvent,
  CreateTask,
}

/**
 * `FestivAR` is the main composable function that sets up the whole app UI. It initializes the
 * navigation controller and defines the navigation graph. You can add your app implementation
 * inside this function.
 *
 * For B3:
 *
 * @param context The context of the application, used for accessing resources and services.
 * @param credentialManager The CredentialManager instance for handling authentication credentials.
 */
@Composable
fun FestivAR(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    authRepository: AuthRepository = AuthRepositoryFirebase(),
) {
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()

  val startDestination =
      if (runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)) {
        AppScreens.EventOverview.name
      } else {
        AppScreens.Login.name
      }

  NavHost(
      navController = navController,
      startDestination = startDestination,
  ) {
    composable(route = AppScreens.SignUp.name) {
      SignUpScreen(
          credentialManager = credentialManager,
          onSignInClick = {
            navController.navigate(AppScreens.Login.name)
          },
          onBackClick = { navController.popBackStack() },
          onSignUpSuccess = {
            navController.navigate(AppScreens.EventOverview.name) {
              popUpTo(AppScreens.EventOverview.name) { inclusive = true }
            }
          },
      )
    }
    composable (route = AppScreens.Login.name) {
      LoginScreen(
          credentialManager = credentialManager,
          onSignUpClick = {
            navController.navigate(AppScreens.SignUp.name)
          },
          onSignedIn = {
            navController.navigate(AppScreens.EventOverview.name) {
              popUpTo(AppScreens.EventOverview.name) { inclusive = true }
            }
          },
      )
    }
    composable(route = AppScreens.EventOverview.name) {
      ScreenName(
        "event overview, click to logout",
        modifier = Modifier.clickable(
          role = Role.Button,
          onClick = {
            scope.launch {
              authRepository.signOut().onSuccess {
                navController.navigate(AppScreens.Login.name) {
                  popUpTo(AppScreens.Login.name) { inclusive = true }
                }
              }
            }
          },
        )
      )
    }
    composable(route = AppScreens.TaskOverview.name) { ScreenName("task overview") }
    composable(route = AppScreens.CreateEvent.name) { ScreenName("create event") }
    composable(route = AppScreens.CreateTask.name) { ScreenName("create task") }
  }
}

@Composable
fun ScreenName(x0: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $x0!", modifier = modifier)
}
