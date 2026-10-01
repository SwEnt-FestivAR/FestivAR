// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryProvider
import com.android.festivar.ui.authentication.SignInScreen
import com.android.festivar.ui.home.HomeScreen
import com.android.festivar.ui.theme.FestivARTheme

/** The single Activity of the app; every screen is a composable under [FestivARApp]. */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { FestivARTheme { Surface(modifier = Modifier.fillMaxSize()) { FestivARApp() } } }
  }
}

/** The routes of the navigation graph. */
object Route {
  const val SIGN_IN = "sign_in"
  const val HOME = "home"
}

/**
 * Builds the navigation graph. A person who is not signed in lands on the sign-in screen, a
 * signed-in one on Home. One [credentialManager] serves sign-in and sign-out, so clearing the
 * credential state on sign-out hits the manager that signed the person in.
 */
@Composable
fun FestivARApp(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    authRepository: AuthRepository = AuthRepositoryProvider.repository,
) {
  val navController = rememberNavController()
  val startDestination = remember {
    if (authRepository.currentUser() != null) Route.HOME else Route.SIGN_IN
  }
  NavHost(navController = navController, startDestination = startDestination) {
    composable(Route.SIGN_IN) {
      SignInScreen(
          credentialManager = credentialManager,
          onSignedIn = { navController.restartAt(Route.HOME) },
      )
    }
    composable(Route.HOME) {
      HomeScreen(
          credentialManager = credentialManager,
          onSignedOut = { navController.restartAt(Route.SIGN_IN) },
      )
    }
  }
}

/** Clears the whole back stack and starts over at [route]: after signing in or out. */
private fun NavHostController.restartAt(route: String) {
  navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
  }
}
